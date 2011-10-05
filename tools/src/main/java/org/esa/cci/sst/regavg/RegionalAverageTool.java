package org.esa.cci.sst.regavg;

import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ExitCode;
import org.esa.cci.sst.tool.Parameter;
import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.util.Cell;
import org.esa.cci.sst.util.UTC;

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
    private static final String TOOL_VERSION = TOOL_NAME + ", version 1.0 (C) 2011-2013 by the ESA SST_cci project";
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

    public static final Parameter PARAM_SST_DEPTH = new Parameter("sstDepth", "DEPTH", SstDepth.SKIN + "", "The SST depth. Must be one of " + Arrays.toString(SstDepth.values()) + ".");
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
    public static final Parameter PARAM_START_DATE = new Parameter("startDate", "DATE", "1990-01-01", "The start date for the analysis given in the format YYYYMMDD");
    public static final Parameter PARAM_END_DATE = new Parameter("endDate", "DATE", "2020-12-31", "The end date for the analysis given in the format YYYYMMDD");
    public static final Parameter PARAM_CLIMATOLOGY_DIR = new Parameter("climatologyDir", "DIR", "./climatology", "The directory path to the reference climatology.");
    public static final Parameter PARAM_LUT_DIR = new Parameter("lutPath", "DIR", "./lut", "The directory path to the variance LUTs.");
    public static final Parameter PARAM_TEMPORAL_RES = new Parameter("temporalRes", "NUM", TemporalResolution.monthly + "", "The temporal resolution. Must be one of " + Arrays.toString(TemporalResolution.values()) + ".");
    public static final Parameter PARAM_PRODUCT_TYPE = new Parameter("productType", "NAME", null, "The product type. Must be one of " + Arrays.toString(ProductType.values()) + ".");
    public static final Parameter PARAM_OUTPUT_DIR = new Parameter("outputDir", "DIR", ".", "The output directory.");

    public static void main(String[] arguments) {
        new RegionalAverageTool().run(arguments);
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
        String productDir = configuration.getString(productType + ".dir", null, true);
        Date startDate = configuration.getDate(PARAM_START_DATE, true);
        Date endDate = configuration.getDate(PARAM_END_DATE, true);
        TemporalResolution temporalResolution = TemporalResolution.valueOf(configuration.getString(PARAM_TEMPORAL_RES, true));
        File outputDir = configuration.getExistingDirectory(PARAM_OUTPUT_DIR, true);
        RegionMaskList regionMaskList = parseRegionList(configuration);

        Climatology climatology = Climatology.open(climatologyDir, productType.getGridDef());
        ProductStore productStore = ProductStore.create(productType, productDir);
        List<RegionalAveraging.OutputTimeStep> outputTimeSteps;
        try {
            outputTimeSteps = RegionalAveraging.computeOutputTimeSteps(productStore, climatology, startDate, endDate, temporalResolution, regionMaskList);
        } catch (IOException e) {
            throw new ToolException("Averaging failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }

        writeOutputs(outputDir, productType, startDate, endDate, regionMaskList, outputTimeSteps);

    }

    private void writeOutputs(File outputDir, ProductType productType, Date startDate, Date endDate, RegionMaskList regionMaskList, List<RegionalAveraging.OutputTimeStep> outputTimeSteps) {
        DateFormat outputDataFormat = UTC.getDateFormat("yyyyMMdd");
        for (int i = 0; i < regionMaskList.size(); i++) {
            RegionMask regionMask = regionMaskList.get(i);
            String outputFilename = RegionalAverageTool.getOutputFilename(outputDataFormat.format(startDate),
                                                                          outputDataFormat.format(endDate),
                                                                          regionMask.getName(),
                                                                          productType.getProcessingLevel(),
                                                                          "SSTskin",
                                                                          "PS",
                                                                          "DM",
                                                                          "01.0");
            File file = new File(outputDir, outputFilename);
            System.out.println("Output " + file + ":");
            for (RegionalAveraging.OutputTimeStep outputTimeStep : outputTimeSteps) {
                Date date1 = outputTimeStep.date1;
                Date date2 = outputTimeStep.date2;
                Cell cell = outputTimeStep.regionalAverages.get(i);
                System.out.printf("  %s - %s: %s\n", outputDataFormat.format(date1), outputDataFormat.format(date2), cell.getMean());
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

}
