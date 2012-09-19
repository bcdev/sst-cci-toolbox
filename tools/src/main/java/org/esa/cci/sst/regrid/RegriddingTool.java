/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.ProcessingLevel;
import org.esa.cci.sst.common.RegionMaskList;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.common.auxiliary.LUT1;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cellgrid.CellGrid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.RegionMask;
import org.esa.cci.sst.common.file.FileStore;
import org.esa.cci.sst.regrid.auxiliary.LUT3;
import org.esa.cci.sst.tool.*;
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

/**
 * A command line tool to regrid ARC/SST-CCI products from into coarser spatial and time resolutions.
 * Call RegriddingTool#main with option -h to access the full description of the tool.
 *
 * @author Ralf Quast, Bettina Scholze
 */
public class RegriddingTool extends Tool {

    private static final String FILE_FORMAT_VERSION = "1.1";
    private static final String TOOL_NAME = "regrid";
    private static final String TOOL_VERSION = "0.1";
    private static final String TOOL_HEADER = "\n" + "The " + TOOL_NAME + " tool is used to read in the SST CCI L3U, L3C, and L4 products at daily 0.05 ° " +
            "latitude by longitude resolution and output on other spatio-temporal resolutions, which are a multiple" +
            "of this and divide neatly into 180 degrees. Output are SSTs and their uncertainties.";
    private static final String TOOL_FOOTER = "";

    //important for input selection of which files ("product types") should be regridded.
    public static final Parameter PARAM_SST_DEPTH = new Parameter("sstDepth", "DEPTH", SstDepth.skin.name(),
            "The SST depth. Must be one of " + Arrays.toString(SstDepth.values()) + ".");

    public static final Parameter PARAM_SPATIAL_RESOLUTION = new Parameter("spatialRes", "NUM",
            SpatialResolution.getDefaultValueAsString(),
            "The spatial resolution of the output grid in degrees. Must be one of " + SpatialResolution.getValuesAsString() + ".");

    public static final Parameter PARAM_TEMPORAL_RES = new Parameter("temporalRes", "NUM", TemporalResolution.monthly + "",
            "The temporal resolution. Must be one of " + Arrays.toString(TemporalResolution.values()) + ".");

    private static final Parameter PARAM_REGION = new Parameter("region", "REGION", "Global=-180,90,180,-90 (NAME=REGION)",
            "The sub-region to be used (optional). Coordinates in the format W,N,E,S.");

    public static final Parameter PARAM_PRODUCT_TYPE = new Parameter("productType", "NAME", null,
            "The product type. Must be one of " + Arrays.toString(ProductType.values()) + ".");

    public static final Parameter PARAM_FILENAME_REGEX = new Parameter("filenameRegex", "REGEX", null,
            "The input filename pattern. REGEX is Regular Expression that usually dependends on the parameter " +
                    "'productType'. E.g. the default value for the product type '" + ProductType.ARC_L3U + "' " +
                    "is '" + ProductType.ARC_L3U.getDefaultFilenameRegex() + "'. For example, if you only want " +
                    "to include daily (D) L3 AATSR (ATS) files with night observations only, dual view, 3 channel retrieval, " +
                    "bayes cloud screening (nD3b) you could use the regex \'ATS_AVG_3PAARC\\\\d{8}_D_nD3b[.]nc[.]gz\'.");

    public static final Parameter PARAM_OUTPUT_DIR = new Parameter("outputDir", "DIR", ".", "The output directory.");

    public static final Parameter PARAM_START_DATE = new Parameter("startDate", "DATE", "1990-01-01",
            "The start date for the analysis given in the format YYYY-MM-DD");

    public static final Parameter PARAM_END_DATE = new Parameter("endDate", "DATE", "2020-12-31",
            "The end date for the analysis given in the format YYYY-MM-DD");

    private static final Parameter PARAM_TOTAL_UNCERTAINTY = new Parameter("totalUncertainty", "BOOL", "false",
            "A Boolean variable indicating whether total or " +
                    "separated uncertainties are written to the output file. Must be either 'true' or 'false'.");

    private static final Parameter PARAM_CLIMATOLOGY_DIR = new Parameter("climatologyDir", "DIR", "./climatology",
            "The directory path to the reference climatology.");

    private static final Parameter PARAM_MIN_COVERAGE = new Parameter("minCoverage", "NUM", "0.5",
            "The minimum fractional coverage " +
                    "required for non-missing output. (fraction of valid values in input per grid box in output) ");

    //optional due to specification
    private static final Parameter PARAM_MAX_UNCERTAINTY = new Parameter("maxUncertainty", "NUM", "",
            "The maximum relative total uncertainty allowed for non-missing output.", true);  //todo bs implement

    public static final Parameter PARAM_COVERAGE_UNCERTAINTY_FILE = new Parameter("coverageUncertaintyFile", "FILE",
            "./conf/auxdata/coverage_uncertainty_parameters.nc",
            "A NetCDF file that provides lookup table for coverage uncertainties."); //todo bs resolutions

    public static final Parameter PARAM_SYNOPTIC_CORRELATION_FILE = new Parameter("synopticCorrelationFile", "FILE",
            "./conf/auxdata/TBC",
            "A NetCDF file that provides lookup table for synoptically correlated uncertainties."); //todo bs add lut

    private ProductType productType;


    public static void main(String[] args) {
        new RegriddingTool().run(args);
    }

    @Override
    protected void run(Configuration configuration, String[] arguments) throws ToolException {
        final SpatialResolution spatialResolution = fetchSpatialResolution(configuration);
        final File climatologyDir = configuration.getExistingDirectory(PARAM_CLIMATOLOGY_DIR, true);
        productType = ProductType.valueOf(configuration.getString(PARAM_PRODUCT_TYPE, true));
        final String filenameRegex = configuration.getString(PARAM_FILENAME_REGEX.getName(),
                productType.getDefaultFilenameRegex(), false);
        final SstDepth sstDepth = SstDepth.valueOf(configuration.getString(PARAM_SST_DEPTH, true));
        final String productDir = configuration.getString(productType + ".dir", null, true);
        final Date startDate = configuration.getDate(PARAM_START_DATE, true);
        final Date endDate = configuration.getDate(PARAM_END_DATE, true);
        final TemporalResolution temporalResolution = TemporalResolution.valueOf(configuration.getString(PARAM_TEMPORAL_RES, true));
        final File outputDir = configuration.getExistingDirectory(PARAM_OUTPUT_DIR, true);
        final RegionMaskList regionMaskList = parseRegionListInTargetSpatialResolution(configuration);
        final double minCoverage = Double.parseDouble(configuration.getString(PARAM_MIN_COVERAGE, false));
        final File lut1File = configuration.getExistingFile(PARAM_COVERAGE_UNCERTAINTY_FILE, true);
        final File lut3File = configuration.getExistingFile(PARAM_SYNOPTIC_CORRELATION_FILE, true);
        //todo
//        String maxUncertainty = configuration.getString(PARAM_MAX_UNCERTAINTY, false);

        Climatology climatology = Climatology.create(climatologyDir, productType.getGridDef());
        FileStore fileStore = FileStore.create(productType, filenameRegex, productDir);
        LUT1 lut1 = getLUT1(lut1File); //coverage uncertainty (magnitude5, exponent5)
        LUT3 lut3 = getLUT3(lut3File, spatialResolution);

        // Enable for debugging
        // printGrid(climatology);

        List<RegriddingTimeStep> timeSteps;
        try {
            Aggregator4Regrid aggregator = new Aggregator4Regrid(regionMaskList, fileStore, climatology,
                    lut1, null, lut3, sstDepth, minCoverage, spatialResolution);
            timeSteps = aggregator.aggregate(startDate, endDate, temporalResolution);
        } catch (IOException e) {
            throw new ToolException("Regridding failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }

        try {
            writeOutputs(outputDir, productType, filenameRegex, sstDepth, temporalResolution, regionMaskList.get(0), timeSteps);
        } catch (IOException e) {
            throw new ToolException("Writing of output failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }
    }

    private SpatialResolution fetchSpatialResolution(Configuration configuration) throws ToolException {
        return SpatialResolution.getFromValue(configuration.getString(PARAM_SPATIAL_RESOLUTION, true));
    }

    @Override
    protected String getName() {
        return TOOL_NAME;
    }

    @Override
    protected String getVersion() {
        return TOOL_VERSION;
    }

    @Override
    protected String getSyntax() {
        return getName() + " [OPTIONS]";
    }

    @Override
    protected String getHeader() {
        return TOOL_HEADER;
    }

    @Override
    protected String getFooter() {
        return TOOL_FOOTER;
    }

    @Override
    protected String getToolHome() {
        return System.getProperty(getName() + ".home", ".");
    }

    @Override
    protected Parameter[] getParameters() {
        ArrayList<Parameter> paramList = new ArrayList<Parameter>();
        paramList.addAll(
                Arrays.asList(PARAM_REGION, PARAM_CLIMATOLOGY_DIR, PARAM_MAX_UNCERTAINTY, PARAM_TOTAL_UNCERTAINTY,
                        PARAM_SPATIAL_RESOLUTION, PARAM_START_DATE, PARAM_END_DATE, PARAM_FILENAME_REGEX,
                        PARAM_SST_DEPTH, PARAM_OUTPUT_DIR, PARAM_PRODUCT_TYPE, PARAM_COVERAGE_UNCERTAINTY_FILE,
                        PARAM_MIN_COVERAGE, PARAM_SYNOPTIC_CORRELATION_FILE));

        ProductType[] values = ProductType.values();
        for (ProductType value : values) {
            paramList.add(new Parameter(value.name() + ".dir", "DIR", null,
                    "Directory that hosts the products of type '" + value.name() + "'."));
        }

        return paramList.toArray(new Parameter[paramList.size()]);
    }

    private RegionMaskList parseRegionListInTargetSpatialResolution(Configuration configuration) throws ToolException {
        try {
            String region = configuration.getString(PARAM_REGION, false);
            RegionMaskList.setSpatialResolution(fetchSpatialResolution(configuration));
            return RegionMaskList.parse(region);
        } catch (Exception e) {
            throw new ToolException(e, ExitCode.USAGE_ERROR);
        }
    }

    private LUT1 getLUT1(File lut1File) throws ToolException {
        LUT1 lut1;
        try {
            lut1 = LUT1.read(lut1File);
            LOGGER.info(String.format("LUT-1 read from '%s'", lut1File));
        } catch (IOException e) {
            throw new ToolException(e, ExitCode.IO_ERROR);
        }
        return lut1;
    }

    private LUT3 getLUT3(File lut3File, SpatialResolution spatialResolution) throws ToolException {
        return LUT3.read(lut3File, spatialResolution);
    }

    private void writeOutputs(File outputDir, ProductType productType, String filenameRegex,
                              SstDepth sstDepth, TemporalResolution temporalResolution,
                              RegionMask regionMask, List<RegriddingTimeStep> timeSteps) throws IOException {

        for (RegriddingTimeStep timeStep : timeSteps) {
            writeOutputs(outputDir, productType, filenameRegex, sstDepth,
                    temporalResolution, regionMask, timeStep);
        }
    }

    private void writeOutputs(File outputDir, ProductType productType, String filenameRegex,
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
            netcdfFile.addGlobalAttribute("file_format_version", FILE_FORMAT_VERSION);
            netcdfFile.addGlobalAttribute("tool_name", TOOL_NAME);
            netcdfFile.addGlobalAttribute("tool_version", TOOL_VERSION);
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

            Variable[] variables = productType.getFileType().createOutputVariables(netcdfFile, sstDepth, dimensionMeasurementRelated);

            //write header
            netcdfFile.create();

            //add data for base
            final Map<String, Array> baseArrays = spatialResolution.calculateBaseArrays();
            for (String baseVariable : baseArrays.keySet()) {
                writeDataToNetCdfFile(netcdfFile, baseVariable, baseArrays.get(baseVariable));
            }

            //add data for regridded variables
            int height = cellGrid.getHeight();
            int width = cellGrid.getWidth();
            HashMap<String, VectorContainer> data = new HashMap<String, VectorContainer>();
            for (Variable variable : variables) {
                data.put(variable.getName(), new VectorContainer(height * width));
            }

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    AggregationCell cell = cellGrid.getCell(x, y);
                    Number[] results = cell.getResults();
                    assert results.length == variables.length : "results must have same length like variables";
                    int index1D = y * width + x;

                    for (int variableCount = 0; variableCount < variables.length; variableCount++) {
                        Variable variable = variables[variableCount];
                        VectorContainer vectorContainer = data.get(variable.getName());
                        vectorContainer.put(index1D, results[variableCount].doubleValue());
                    }
                }
            }

            int[] shape = new int[]{gridDef.getTime(), gridDef.getHeight(), gridDef.getWidth()};
            for (Variable variable : variables) {
                String name = variable.getName();
                float[] vec = data.get(name).getAsFloats();
                writeDataToNetCdfFile(netcdfFile, name, Array.factory(DataType.FLOAT, shape, vec));
            }
            //copy data for time todo

        } finally {
            try {
                netcdfFile.flush();
                netcdfFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static class VectorContainer {
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
     * @param productString        Product String (see Table 5 in PSD) // todo - find out from PSD what productString is
     * @param additionalSegregator Additional Segregator = LT or DM  // todo - find out from PSD what additionalSegregator is
     * @return The filename.
     */
    public String getOutputFilename(String startOfPeriod, String endOfPeriod, String regionName,
                                    ProcessingLevel processingLevel, String sstType,
                                    String productString, String additionalSegregator) {

        String rdac = productType.getFileType().getRdac(); //ESACCI or ARC
        return String.format("%s-%s-%s_regrid-" + rdac + "-%s_GHRSST-%s-%s-%s-v%s-fv%s.nc",
                startOfPeriod, endOfPeriod, regionName,
                processingLevel, sstType, productString, additionalSegregator,
                TOOL_VERSION, FILE_FORMAT_VERSION);
    }
}
