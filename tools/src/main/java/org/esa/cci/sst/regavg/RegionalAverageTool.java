package org.esa.cci.sst.regavg;

import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ExitCode;
import org.esa.cci.sst.tool.Parameter;
import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.util.Cell;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * The SST_cci Regional-Average tool.
 *
 * @author Norman Fomferra
 */
public class RegionalAverageTool extends Tool {

    private static final String TOOL_NAME = "regavg";
    private static final String TOOL_VERSION = TOOL_NAME + ", version 0.1 (C) 2011-2013 by the ESA SST_cci project";
    private static final String TOOL_SYNTAX = TOOL_NAME + " [OPTIONS]";
    private static final String TOOL_HEADER = "\n" +
            "The regavg tool is used to generate regional average time-series from ARC (L2P, L3U) and " +
            "SST_cci (L3U, L3P, L4) product files given a time interval and a list of regions. An output " +
            "NetCDF file will be written for each region.\n" +
            "OPTIONS may be one or more of the following:\n";

    private static final String TOOL_FOOTER = "\n" +
            "All parameter options may also be read from a key-value-pair file. The tool will always try " +
            "to read settings in the default configuration file './regavg.properties'. Optionally, a " +
            "configuration file may be provided using the -c <FILE> option (see above)." +
            "Command-line options overwrite the settings given by -c, which again overwrite settings in " +
            "default configuration file.\n";

    public static final Parameter PARAM_SST_DEPTH = new Parameter("sstDepth", "DEPTH", SstDepth.skin + "", "The SST depth. Must be one of " + Arrays.toString(SstDepth.values()) + ".");
    public static final Parameter PARAM_REGION_LIST = new Parameter("regionList", "NAME=REGION[;...]", "Global=-180,90,180,-90",
                                                                    "A semicolon-separated list of NAME=REGION pairs. "
                                                                            + "REGION may be given as coordinates in the format W,N,E,S "
                                                                            + "or as name of a file that provides a region mask in plain text form. "
                                                                            + "The region mask file contains 72 x 36 5-degree grid cells. "
                                                                            + "Colums correspond to range -180 (first column) to +180 (last column) degrees longitude, "
                                                                            + "while lines correspond to +90 (first line) to -90 (last line) degrees latitude. "
                                                                            + "Cells can be '0' or '1', where "
                                                                            + "a '1' indicates that the region represented by the cell will be considered "
                                                                            + "in the averaging process.");
    public static final Parameter PARAM_START_DATE = new Parameter("startDate", "DATE", "1990-01-01", "The start date for the analysis given in the format YYYY-MM-DD");
    public static final Parameter PARAM_END_DATE = new Parameter("endDate", "DATE", "2020-12-31", "The end date for the analysis given in the format YYYY-MM-DD");
    public static final Parameter PARAM_CLIMATOLOGY_DIR = new Parameter("climatologyDir", "DIR", "./climatology", "The directory path to the reference climatology.");
    public static final Parameter PARAM_LUT_DIR = new Parameter("lutPath", "DIR", "./lut", "The directory path to the variance LUTs.");
    public static final Parameter PARAM_TEMPORAL_RES = new Parameter("temporalRes", "NUM", TemporalResolution.monthly + "", "The temporal resolution. Must be one of " + Arrays.toString(TemporalResolution.values()) + ".");
    public static final Parameter PARAM_PRODUCT_TYPE = new Parameter("productType", "NAME", null, "The product type. Must be one of " + Arrays.toString(ProductType.values()) + ".");
    public static final Parameter PARAM_OUTPUT_DIR = new Parameter("outputDir", "DIR", ".", "The output directory.");

    public static void main(String[] arguments) {
        new RegionalAverageTool().run(arguments);
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
        return TOOL_SYNTAX;
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
    protected Parameter[] getParameters() {
        ArrayList<Parameter> paramList = new ArrayList<Parameter>();
        paramList.addAll(Arrays.asList(
                PARAM_SST_DEPTH,
                PARAM_TEMPORAL_RES,
                PARAM_REGION_LIST,
                PARAM_START_DATE,
                PARAM_END_DATE,
                PARAM_CLIMATOLOGY_DIR,
                PARAM_LUT_DIR,
                PARAM_PRODUCT_TYPE,
                PARAM_OUTPUT_DIR));
        ProductType[] values = ProductType.values();
        for (ProductType value : values) {
            paramList.add(new Parameter(value.name() + ".dir", "DIR", null, "Directory that hosts the products of type '" + value.name() + "'."));
        }
        return paramList.toArray(new Parameter[paramList.size()]);
    }

    @Override
    protected void run(Configuration configuration, String[] arguments) throws ToolException {

        File climatologyDir = configuration.getExistingDirectory(PARAM_CLIMATOLOGY_DIR, true);
        ProductType productType = ProductType.valueOf(configuration.getString(PARAM_PRODUCT_TYPE, true));
        SstDepth sstDepth = SstDepth.valueOf(configuration.getString(PARAM_SST_DEPTH, true));
        String productDir = configuration.getString(productType + ".dir", null, true);
        Date startDate = configuration.getDate(PARAM_START_DATE, true);
        Date endDate = configuration.getDate(PARAM_END_DATE, true);
        TemporalResolution temporalResolution = TemporalResolution.valueOf(configuration.getString(PARAM_TEMPORAL_RES, true));
        File outputDir = configuration.getExistingDirectory(PARAM_OUTPUT_DIR, true);
        RegionMaskList regionMaskList = parseRegionList(configuration);

        Climatology climatology = Climatology.create(climatologyDir, productType.getGridDef());
        ProductStore productStore = ProductStore.create(productType, productDir);

        List<RegionalAveraging.OutputTimeStep> outputTimeSteps;
        try {
            outputTimeSteps = RegionalAveraging.computeOutputTimeSteps(productStore, climatology, sstDepth, startDate, endDate, temporalResolution, regionMaskList);
        } catch (IOException e) {
            throw new ToolException("Averaging failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }

        try {
            writeOutputs(outputDir, productType, sstDepth, startDate, endDate, temporalResolution, regionMaskList, outputTimeSteps);
        } catch (IOException e) {
            throw new ToolException("Writing of output failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }
    }

    private void writeOutputs(File outputDir,
                              ProductType productType,
                              SstDepth sstDepth,
                              Date startDate,
                              Date endDate,
                              TemporalResolution temporalResolution,
                              RegionMaskList regionMaskList,
                              List<RegionalAveraging.OutputTimeStep> outputTimeSteps) throws IOException {

        for (int regionIndex = 0; regionIndex < regionMaskList.size(); regionIndex++) {
            RegionMask regionMask = regionMaskList.get(regionIndex);
            dump("SST_" + sstDepth, regionMask.getName(), regionIndex, outputTimeSteps);
        }

        DateFormat filenameDateFormat = UTC.getDateFormat("yyyyMMdd");
        for (int regionIndex = 0; regionIndex < regionMaskList.size(); regionIndex++) {
            RegionMask regionMask = regionMaskList.get(regionIndex);
            String outputFilename = getOutputFilename(filenameDateFormat.format(startDate),
                                                      filenameDateFormat.format(endDate),
                                                      regionMask.getName(),
                                                      productType.getProcessingLevel(),
                                                      "SST_" + sstDepth,
                                                      "PS",
                                                      "DM",
                                                      "01.0");
            File file = new File(outputDir, outputFilename);
            LOGGER.info("Writing output file '" + file + "'...");
            writeOutputFile(file, productType, sstDepth, startDate, endDate, temporalResolution, regionMask, regionIndex, outputTimeSteps);
        }
    }

    private void dump(String sstName, String regionName, int regionIndex, List<RegionalAveraging.OutputTimeStep> outputTimeSteps) {
        System.out.printf("-------------------------------------\n");
        System.out.printf("%s\t%s\t%s\t%s\t%s\n", "region", "start", "end", "step", sstName);
        DateFormat dateFormat = UTC.getDateFormat("yyyy-MM-dd");
        for (int t = 0; t < outputTimeSteps.size(); t++) {
            RegionalAveraging.OutputTimeStep outputTimeStep = outputTimeSteps.get(t);
            Date date1 = outputTimeStep.date1;
            Date date2 = outputTimeStep.date2;
            Cell cell = outputTimeStep.regionalAverages.get(regionIndex);
            System.out.printf("%s\t%s\t%s\t%s\t%s\n",
                              regionName,
                              dateFormat.format(date1),
                              dateFormat.format(date2),
                              t + 1,
                              cell.getMean());
        }
    }

    private static void writeOutputFile(File file, ProductType productType,
                                        SstDepth sstDepth,
                                        Date startDate,
                                        Date endDate,
                                        TemporalResolution temporalResolution,
                                        RegionMask regionMask, int regionIndex,
                                        List<RegionalAveraging.OutputTimeStep> outputTimeSteps) throws IOException {

        NetcdfFileWriteable netcdfFile = NetcdfFileWriteable.createNew(file.getPath());
        try {
            netcdfFile.addGlobalAttribute("title", String.format("%s SST_%s anomalies", productType.toString(), sstDepth.toString()));
            netcdfFile.addGlobalAttribute("institution", "IAES, University of Edinburgh");
            netcdfFile.addGlobalAttribute("contact", "c.merchant@ed.ac.uk");
            netcdfFile.addGlobalAttribute("tool_name", TOOL_NAME);
            netcdfFile.addGlobalAttribute("tool_version", TOOL_VERSION);
            netcdfFile.addGlobalAttribute("generated_at", UTC.getIsoFormat().format(new Date()));
            netcdfFile.addGlobalAttribute("product_type", productType.toString());
            netcdfFile.addGlobalAttribute("sst_depth", sstDepth.toString());
            netcdfFile.addGlobalAttribute("start_date", UTC.getIsoFormat().format(startDate));
            netcdfFile.addGlobalAttribute("end_date", UTC.getIsoFormat().format(endDate));
            netcdfFile.addGlobalAttribute("temporal_resolution", temporalResolution.toString());
            netcdfFile.addGlobalAttribute("region_name", regionMask.getName());

            int numSteps = outputTimeSteps.size();
            Dimension timeDimension = netcdfFile.addDimension("time", numSteps, true, false, false);

            Variable startTimeVar = netcdfFile.addVariable("start_time", DataType.FLOAT, new Dimension[]{timeDimension});
            startTimeVar.addAttribute(new Attribute("units", "seconds"));
            startTimeVar.addAttribute(new Attribute("long_name", "reference start time of averaging period in seconds until 1981-01-01T00:00:00"));

            Variable endTimeVar = netcdfFile.addVariable("end_time", DataType.FLOAT, new Dimension[]{timeDimension});
            endTimeVar.addAttribute(new Attribute("units", "seconds"));
            endTimeVar.addAttribute(new Attribute("long_name", "reference end time of averaging period in seconds until 1981-01-01T00:00:00"));

            Variable sstAnomalyMeanVar = netcdfFile.addVariable("sst_" + sstDepth + "_anomaly_mean", DataType.FLOAT, new Dimension[]{timeDimension});
            sstAnomalyMeanVar.addAttribute(new Attribute("units", "kelvin"));
            sstAnomalyMeanVar.addAttribute(new Attribute("long_name", "mean of sst anomaly in kelvin."));
            sstAnomalyMeanVar.addAttribute(new Attribute("_FillValue", Double.NaN));

            // Actually not required by Nick's tool spec.
            Variable sstAnomalySigmaVar = netcdfFile.addVariable("sst_" + sstDepth + "_anomaly_sigma", DataType.FLOAT, new Dimension[]{timeDimension});
            sstAnomalySigmaVar.addAttribute(new Attribute("units", "kelvin"));
            sstAnomalySigmaVar.addAttribute(new Attribute("long_name", "sigma of sst anomaly in kelvin."));
            sstAnomalySigmaVar.addAttribute(new Attribute("_FillValue", Double.NaN));

            // Actually not required by Nick's tool spec.
            Variable sstAnomalyCountVar = netcdfFile.addVariable("sst_" + sstDepth + "_anomaly_count", DataType.INT, new Dimension[]{timeDimension});
            sstAnomalyCountVar.addAttribute(new Attribute("units", "1"));
            sstAnomalyCountVar.addAttribute(new Attribute("long_name", "counts of sst anomaly contributions."));

            long millisSince1981 = UTC.createCalendar(1981).getTimeInMillis();

            float[] startTime = new float[numSteps];
            float[] endTime = new float[numSteps];
            float[] sstAnomalyMean = new float[numSteps];
            float[] sstAnomalySigma = new float[numSteps];
            int[] sstAnomalyCount = new int[numSteps];
            for (int t = 0; t < numSteps; t++) {
                RegionalAveraging.OutputTimeStep outputTimeStep = outputTimeSteps.get(t);
                startTime[t] = (outputTimeStep.date1.getTime() - millisSince1981) / 1000.0F;
                endTime[t] = (outputTimeStep.date2.getTime() - millisSince1981) / 1000.0F;
                sstAnomalyMean[t] = (float) outputTimeStep.regionalAverages.get(regionIndex).getMean();
                sstAnomalySigma[t] = (float) outputTimeStep.regionalAverages.get(regionIndex).getSigma();
                sstAnomalyCount[t] = (int) outputTimeStep.regionalAverages.get(regionIndex).getAccuCount();
            }

            netcdfFile.create();

            netcdfFile.write(startTimeVar.getName(), Array.factory(DataType.FLOAT, new int[]{numSteps}, startTime));
            netcdfFile.write(endTimeVar.getName(), Array.factory(DataType.FLOAT, new int[]{numSteps}, endTime));
            netcdfFile.write(sstAnomalyMeanVar.getName(), Array.factory(DataType.FLOAT, new int[]{numSteps}, sstAnomalyMean));
            netcdfFile.write(sstAnomalySigmaVar.getName(), Array.factory(DataType.FLOAT, new int[]{numSteps}, sstAnomalySigma));
            netcdfFile.write(sstAnomalyCountVar.getName(), Array.factory(DataType.INT, new int[]{numSteps}, sstAnomalyCount));

        } catch (InvalidRangeException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                netcdfFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private RegionMaskList parseRegionList(Configuration configuration) throws ToolException {
        try {
            return RegionMaskList.parse(configuration.getString(PARAM_REGION_LIST, false));
        } catch (Exception e) {
            throw new ToolException(e, ExitCode.USAGE_ERROR);
        }
    }

    /**
     * Generates a filename of the form
     * <code>
     * <i>startOfPeriod</i><b>-</b><i>endOfPeriod</i><b>-</b><i>regionName</i><b>_average-ESACCI-</b><i>processingLevel</i><b>_GHRSST-</b><i>sstType</i><b>-</b><i>productString</i><b>-</b><i>additionalSegregator</i><b>-v02.0-fv</b><i>fileVersion</i><b>.nc</b>
     * </code>
     *
     * @param startOfPeriod        Start of period = YYYYMMDD
     * @param endOfPeriod          End of period = YYYYMMDD
     * @param regionName           Region Name or Description
     * @param processingLevel      Processing Level = L3C, L3U or L4
     * @param sstType              SST Type (see Table 4)
     * @param productString        Product String (see Table 5 in PSD)
     * @param additionalSegregator Additional Segregator = LT or DM
     * @param fileVersion          File Version, e.g. 0.10
     * @return The filename.
     */
    public static String getOutputFilename(String startOfPeriod, String endOfPeriod, String regionName, ProcessingLevel processingLevel, String sstType, String productString, String additionalSegregator, String fileVersion) {
        return String.format("%s-%s-%s_average-ESACCI-%s_GHRSST-%s-%s-%s-v02.0-fv%s.nc",
                             startOfPeriod,
                             endOfPeriod,
                             regionName,
                             processingLevel,
                             sstType,
                             productString,
                             additionalSegregator,
                             fileVersion);
    }
}
