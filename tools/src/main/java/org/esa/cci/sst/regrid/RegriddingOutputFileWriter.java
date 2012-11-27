package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.ProcessingLevel;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cellgrid.CellGrid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.RegionMask;
import org.esa.cci.sst.common.file.CciL3FileType;
import org.esa.cci.sst.common.file.FileType;
import org.esa.cci.sst.util.ProductType;
import org.esa.cci.sst.util.UTC;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * {@author Bettina Scholze}
 * Date: 24.09.12 09:04
 */
class RegriddingOutputFileWriter {
    public static final Logger LOGGER = Logger.getLogger("org.esa.cci.sst.regrid");
    private final ProductType productType;
    private final String toolName;
    private final String toolVersion;
    private final String fileFormatVersion;
    private boolean totalUncertainty;
    private double maxTotalUncertainty;

    public RegriddingOutputFileWriter(ProductType productType, String toolName, String toolVersion,
                                      String fileFormatVersion, boolean totalUncertaintyWanted, double maxTotalUncertainty) {
        this.productType = productType;
        this.toolName = toolName;
        this.toolVersion = toolVersion;
        this.fileFormatVersion = fileFormatVersion;

        if (totalUncertaintyWanted && isTotalUncertaintyPossible(productType)) {
            this.totalUncertainty = true;
        }
        this.maxTotalUncertainty = maxTotalUncertainty;
    }

    public static boolean isTotalUncertaintyPossible(ProductType productType) {
        boolean synopticUncertainties = productType.getFileType().hasSynopticUncertainties();
        boolean rightProductType = productType.equals(ProductType.CCI_L3C) || productType.equals(ProductType.CCI_L3U);
        return rightProductType && synopticUncertainties;
    }


    public void writeOutputs(File outputDir, String filenameRegex,
                             SstDepth sstDepth, TemporalResolution temporalResolution,
                             RegionMask regionMask, List<RegriddingTimeStep> timeSteps) throws IOException {

        for (RegriddingTimeStep timeStep : timeSteps) {
            writeOutputs(outputDir, filenameRegex, sstDepth, temporalResolution, regionMask, timeStep);
        }
    }

    private void writeOutputs(File outputDir, String filenameRegex,
                              SstDepth sstDepth, TemporalResolution temporalResolution,
                              RegionMask regionMask, RegriddingTimeStep regriddingTimeStep) throws IOException {

        Date startDate = regriddingTimeStep.getStartDate();
        Date endDate = regriddingTimeStep.getEndDate();
        final DateFormat filenameDateFormat = UTC.getDateFormat("yyyyMMdd");
        final String outputFilename = getOutputFilename(
                filenameDateFormat.format(startDate), filenameDateFormat.format(endDate), regionMask.getName(),
                productType.getProcessingLevel(), "SST_" + sstDepth + "_regridded", "PS", "DM");
        final File file = new File(outputDir, outputFilename);
        LOGGER.info("Writing output file '" + file + "'...");

        CellGrid<? extends AggregationCell> cellGrid = regriddingTimeStep.getCellGrid();
        GridDef gridDef = cellGrid.getGridDef();
        SpatialResolution spatialResolution = SpatialResolution.getFromValue(String.valueOf(gridDef.getResolution()));
        //global attributes
        NetcdfFileWriteable netcdfFile = NetcdfFileWriteable.createNew(file.getPath());
        try {
            netcdfFile.addGlobalAttribute("title", String.format("%s SST_%s anomalies", productType.toString(), sstDepth.toString()));
            netcdfFile.addGlobalAttribute("institution", "IAES, University of Edinburgh");
            netcdfFile.addGlobalAttribute("contact", "c.merchant@ed.ac.uk");
            netcdfFile.addGlobalAttribute("fileFormatVersion", fileFormatVersion);
            netcdfFile.addGlobalAttribute("toolName", toolName);
            netcdfFile.addGlobalAttribute("toolVersion", toolVersion);
            netcdfFile.addGlobalAttribute("generated_at", UTC.getIsoFormat().format(new Date()));
            netcdfFile.addGlobalAttribute("product_type", productType.toString());
            netcdfFile.addGlobalAttribute("sst_depth", sstDepth.toString());
            netcdfFile.addGlobalAttribute("start_date", UTC.getIsoFormat().format(startDate));
            netcdfFile.addGlobalAttribute("end_date", UTC.getIsoFormat().format(endDate));
            netcdfFile.addGlobalAttribute("temporal_resolution", temporalResolution.toString());
            netcdfFile.addGlobalAttribute("geospatial_lon_resolution", spatialResolution.getValue());
            netcdfFile.addGlobalAttribute("geospatial_lat_resolution", spatialResolution.getValue());
            netcdfFile.addGlobalAttribute("region_name", regionMask.getName());
            netcdfFile.addGlobalAttribute("source_filename_regex", filenameRegex);

            //global dimensions
            Dimension latDim = netcdfFile.addDimension("lat", gridDef.getHeight());
            Dimension lonDim = netcdfFile.addDimension("lon", gridDef.getWidth());
            Dimension timeDim = netcdfFile.addDimension("time", gridDef.getTime(), true, false, false);
            Dimension bndsDim = netcdfFile.addDimension("bnds", 2);
            Dimension[] dimensionMeasurementRelated = {timeDim, latDim, lonDim};

            Variable[] variables = createVariables(sstDepth, netcdfFile, latDim, lonDim, bndsDim, dimensionMeasurementRelated);

            //write header
            netcdfFile.create();
            //add data for base - lat lon
            final Map<String, Array> baseArrays = spatialResolution.calculateBaseArrays();
            for (String baseVariable : baseArrays.keySet()) {
                writeDataToNetCdfFile(netcdfFile, baseVariable, baseArrays.get(baseVariable));
            }
            //add data for regridded variables
            int height = cellGrid.getHeight();
            int width = cellGrid.getWidth();
            HashMap<String, VectorContainer> dataMap = prepareDataMap(cellGrid, height, width, variables);

            int[] shape = new int[]{gridDef.getTime(), gridDef.getHeight(), gridDef.getWidth()};
            for (Variable variable : variables) {
                String name = variable.getName();
                float[] vec = dataMap.get(name).getAsFloats();
                writeDataToNetCdfFile(netcdfFile, name, Array.factory(DataType.FLOAT, shape, vec));
            }
        } finally {
            try {
                netcdfFile.flush();
                netcdfFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    Variable[] createVariables(SstDepth sstDepth,
                               NetcdfFileWriteable netcdfFile,
                               Dimension latDim, Dimension lonDim, Dimension bndsDim,
                               Dimension[] dimensionMeasurementRelated) {
        Variable latVar = netcdfFile.addVariable("lat", DataType.FLOAT, new Dimension[]{latDim});
        latVar.addAttribute(new Attribute("units", "degrees_north"));
        latVar.addAttribute(new Attribute("long_name", "latitude"));
        latVar.addAttribute(new Attribute("bounds", "lat_bnds"));

        Variable latBnds = netcdfFile.addVariable("lat_bnds", DataType.FLOAT, new Dimension[]{latDim, bndsDim});
        latBnds.addAttribute(new Attribute("units", "degrees_north"));
        latBnds.addAttribute(new Attribute("long_name", "latitude cell boundaries"));

        Variable lonVar = netcdfFile.addVariable("lon", DataType.FLOAT, new Dimension[]{lonDim});
        lonVar.addAttribute(new Attribute("units", "degrees_east"));
        lonVar.addAttribute(new Attribute("long_name", "longitude"));
        lonVar.addAttribute(new Attribute("bounds", "lon_bnds"));

        Variable lonBnds = netcdfFile.addVariable("lon_bnds", DataType.FLOAT, new Dimension[]{lonDim, bndsDim});
        lonBnds.addAttribute(new Attribute("units", "degrees_east"));
        lonBnds.addAttribute(new Attribute("long_name", "longitude cell boundaries"));

        final FileType fileType = productType.getFileType();
        return fileType.createOutputVariables(netcdfFile, sstDepth, totalUncertainty, dimensionMeasurementRelated);
    }

    /* rescale from 2D grid to 1D vector, one per variable */
    private HashMap<String, VectorContainer> prepareDataMap(CellGrid<? extends AggregationCell> cellGrid, int height, int width, Variable[] variables) {

        HashMap<String, VectorContainer> dataMap = initialiseDataMap(variables, height, width);
        //walk the grid
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                AggregationCell cell = cellGrid.getCell(x, y);
                Number[] results = cell.getResults(); //results never contain totalUncertainty
                int index1D = y * width + x;
                fillInDataMap(variables, dataMap, results, index1D);
            }
        }
        return dataMap;
    }

    HashMap<String, VectorContainer> initialiseDataMap(Variable[] variables, int height, int width) {
        HashMap<String, VectorContainer> dataMap = new HashMap<String, VectorContainer>();
        for (Variable variable : variables) {
            dataMap.put(variable.getName(), new VectorContainer(height * width));
        }
        return dataMap;
    }

    void fillInDataMap(Variable[] variables, HashMap<String, VectorContainer> dataMap, Number[] results, int index1D) {

        double totalUncertainty = calculateTotalUncertainty(variables, results);

        for (int variableCount = 0; variableCount < variables.length; variableCount++) {
            Variable variable = variables[variableCount];
            String variableName = variable.getName();
            VectorContainer vectorContainer = dataMap.get(variableName);

            if (isWantedAndAllowedTotalUncertainty(variableName)) {
                double value = totalUncertainty < maxTotalUncertainty ? totalUncertainty : Double.NaN;
                vectorContainer.put(index1D, value);
                break;
            } else {
                double value = totalUncertainty < maxTotalUncertainty ? results[variableCount].doubleValue() : Double.NaN;
                vectorContainer.put(index1D, value);
            }
        }
    }

    private boolean isWantedAndAllowedTotalUncertainty(String variableName) {
        return CciL3FileType.OUT_VAR_TOTAL_UNCERTAINTY.equals(variableName) && totalUncertainty;
    }


    /*Per convention the ordering is such, that all relevant uncertainties are at the end of the result[] vector*/
    static double calculateTotalUncertainty(Variable[] variables, Number[] results) {

        double uncertaintySum = 0.0;
        for (int i = 0; i < variables.length; i++) {
            final String variable = variables[i].getName();

            if (identifyFirstUncertainty(variable)) {
                while (i < results.length) {
                    double value = results[i].doubleValue();
                    if (!Double.isNaN(value)) {
                        uncertaintySum += (value * value);
                    }
                    i++;
                }
            }
        }
        return Math.sqrt(uncertaintySum);
    }

    private static boolean identifyFirstUncertainty(String variable) {
        return variable.contains("uncertainty") || variable.contains("error");
    }

    static class VectorContainer {
        double[] vec;

        private VectorContainer(int length) {
            this.vec = new double[length];
            Arrays.fill(vec, 0);
        }

        public void put(int i, double value) {
            vec[i] = value;
        }

        public float[] getAsFloats() {
            float[] floats = new float[vec.length];
            for (int i = 0; i < floats.length; i++) {
                floats[i] = (float) vec[i];
            }
            return floats;
        }
    }

    private void writeDataToNetCdfFile(NetcdfFileWriteable netcdfFile, String variable, Array array) throws IOException {
        try {
            netcdfFile.write(variable, array);
        } catch (InvalidRangeException e) {
            LOGGER.throwing("Regridding Tool", "writeDataToNetCdfFile", e);
        }
    }

    /**
     * Generates a filename of the form
     * <code>
     * <i>startOfPeriod</i><b>-</b><i>endOfPeriod</i><b>-</b><i>regionName</i><b>_regridding-ESACCI-</b><i>processingLevel</i><b>_GHRSST-</b><i>sstType</i><b>-</b><i>productString</i><b>-</b><i>additionalSegregator</i><b>-v02.0-fv</b><i>fileVersion</i><b>.nc</b>
     * </code>
     *
     * @param startOfPeriod        Start of period = YYYYMMDD
     * @param endOfPeriod          End of period = YYYYMMDD
     * @param regionName           Region Name or Description
     * @param processingLevel      Processing Level = L3C, L3U or L4
     * @param sstType              SST Type
     * @param productString        Product String (see Table 5 in PSD, e.g. AATSR, OSTIA)
     * @param additionalSegregator Additional Segregator = LT or DM
     * @return The filename.
     */
    String getOutputFilename(String startOfPeriod, String endOfPeriod, String regionName,
                             ProcessingLevel processingLevel, String sstType,
                             String productString, String additionalSegregator) {

        String rdac = productType.getFileType().getRdac(); //ESACCI or ARC
        return String.format("%s-%s-%s-" + rdac + "-%s_GHRSST-%s-%s-%s-v%s-fv%s.nc",
                startOfPeriod, endOfPeriod, regionName,
                processingLevel, sstType, productString, additionalSegregator,
                toolVersion, fileFormatVersion);
    }
}
