package org.esa.cci.sst.regavg;

import org.esa.cci.sst.tool.*;
import org.esa.cci.sst.util.UTC;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * The SST_cci Regional-Average tool.
 *
 * @author Norman Fomferra
 */
public final class RegionalAverageTool extends Tool {

    private static final String FILE_FORMAT_VERSION = "1.1";

    private static final String TOOL_NAME = "regavg";
    private static final String TOOL_VERSION = "1.0_b01";
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

    public static final Parameter PARAM_SST_DEPTH = new Parameter("sstDepth", "DEPTH", SstDepth.skin + "",
                                                                  "The SST depth. Must be one of " + Arrays.toString(SstDepth.values()) + ".");
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
    public static final Parameter PARAM_START_DATE = new Parameter("startDate", "DATE", "1990-01-01",
                                                                   "The start date for the analysis given in the format YYYY-MM-DD");
    public static final Parameter PARAM_END_DATE = new Parameter("endDate", "DATE", "2020-12-31",
                                                                 "The end date for the analysis given in the format YYYY-MM-DD");
    public static final Parameter PARAM_CLIMATOLOGY_DIR = new Parameter("climatologyDir", "DIR", "./climatology",
                                                                        "The directory path to the reference climatology.");
    public static final Parameter PARAM_TEMPORAL_RES = new Parameter("temporalRes", "NUM", TemporalResolution.monthly + "",
                                                                     "The temporal resolution. Must be one of " + Arrays.toString(TemporalResolution.values()) + ".");
    public static final Parameter PARAM_PRODUCT_TYPE = new Parameter("productType", "NAME", null,
                                                                     "The product type. Must be one of " + Arrays.toString(ProductType.values()) + ".");
    public static final Parameter PARAM_FILENAME_REGEX = new Parameter("filenameRegex", "REGEX", null,
                                                                       "The input filename pattern. REGEX is Regular Expression that usually dependends on the parameter " +
                                                                               "'productType'. E.g. the default value for the product type '" + ProductType.ARC_L3U + "' " +
                                                                               "is '" + ProductType.ARC_L3U.getDefaultFilenameRegex() + "'. For example, if you only want " +
                                                                               "to include daily (D) L3 AATSR (ATS) files with night observations only, dual view, 3 channel retrieval, " +
                                                                               "bayes cloud screening (nD3b) you could use the regex \'ATS_AVG_3PAARC\\\\d{8}_D_nD3b[.]nc[.]gz\'.");
    public static final Parameter PARAM_OUTPUT_DIR = new Parameter("outputDir", "DIR", ".",
                                                                   "The output directory.");

    public static final Parameter PARAM_LUT1_FILE = new Parameter("lut1File", "FILE", "conf/auxdata/coverage_uncertainty_parameters.nc",
                                                                  "A NetCDF file that provides lookup table 1.");

    public static final Parameter PARAM_LUT2_FILE = new Parameter("lut2File", "FILE", "conf/auxdata/RegionalAverage_LUT2.txt",
                                                                  "A plain text file that provides lookup table 2.");

    public static final Parameter PARAM_WRITE_TEXT = new Parameter("writeText", null, null,
                                                                   "Also writes results to a plain text file 'regavg-output-<date>.txt'.");

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
    protected String getToolHome() {
        return System.getProperty(TOOL_NAME + ".home", ".");
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
                PARAM_LUT1_FILE,
                PARAM_LUT2_FILE,
                PARAM_PRODUCT_TYPE,
                PARAM_FILENAME_REGEX,
                PARAM_OUTPUT_DIR,
                PARAM_WRITE_TEXT));
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
        String filenameRegex = configuration.getString(PARAM_FILENAME_REGEX.getName(), productType.getDefaultFilenameRegex(), false);
        SstDepth sstDepth = SstDepth.valueOf(configuration.getString(PARAM_SST_DEPTH, true));
        String productDir = configuration.getString(productType + ".dir", null, true);
        Date startDate = configuration.getDate(PARAM_START_DATE, true);
        Date endDate = configuration.getDate(PARAM_END_DATE, true);
        TemporalResolution temporalResolution = TemporalResolution.valueOf(configuration.getString(PARAM_TEMPORAL_RES, true));
        File outputDir = configuration.getExistingDirectory(PARAM_OUTPUT_DIR, true);
        RegionMaskList regionMaskList = parseRegionList(configuration);
        File lut1File = configuration.getExistingFile(PARAM_LUT1_FILE, true);
        File lut2File = configuration.getExistingFile(PARAM_LUT2_FILE, true);
        boolean writeText = configuration.getBoolean(PARAM_WRITE_TEXT, false);

        Climatology climatology = Climatology.create(climatologyDir, productType.getGridDef());
        FileStore fileStore = FileStore.create(productType, filenameRegex, productDir);
        LUT1 lut1 = getLUT1(lut1File);
        LUT2 lut2 = getLUT2(lut2File);

        List<Aggregator.TimeStep> timeSteps;
        try {
            Aggregator aggregator = new Aggregator(regionMaskList, fileStore, climatology, lut1, lut2, sstDepth);
            timeSteps = aggregator.aggregate(startDate, endDate, temporalResolution);
        } catch (IOException e) {
            throw new ToolException("Averaging failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }

        try {
            writeOutputs(outputDir, writeText, productType, filenameRegex,
                         sstDepth, startDate, endDate, temporalResolution, regionMaskList, timeSteps);
        } catch (IOException e) {
            throw new ToolException("Writing of output failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
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

    private LUT2 getLUT2(File lut2File) throws ToolException {
        LUT2 lut2;
        try {
            lut2 = LUT2.read(lut2File);
            LOGGER.info(String.format("LUT-2 read from '%s'", lut2File));
        } catch (IOException e) {
            throw new ToolException(e, ExitCode.IO_ERROR);
        }
        return lut2;
    }

    private void writeOutputs(File outputDir,
                              boolean writeText,
                              ProductType productType,
                              String filenameRegex,
                              SstDepth sstDepth,
                              Date startDate,
                              Date endDate,
                              TemporalResolution temporalResolution,
                              RegionMaskList regionMaskList,
                              List<Aggregator.TimeStep> timeSteps) throws IOException {

        final PrintWriter textWriter = getTextWriter(writeText);

        DateFormat filenameDateFormat = UTC.getDateFormat("yyyyMMdd");
        for (int regionIndex = 0; regionIndex < regionMaskList.size(); regionIndex++) {
            RegionMask regionMask = regionMaskList.get(regionIndex);
            String outputFilename = getOutputFilename(filenameDateFormat.format(startDate),
                                                      filenameDateFormat.format(endDate),
                                                      regionMask.getName(),
                                                      productType.getProcessingLevel(),
                                                      "SST_" + sstDepth + "_average",
                                                      "PS",
                                                      "DM"
            );
            File file = new File(outputDir, outputFilename);
            LOGGER.info("Writing output file '" + file + "'...");
            writeOutputFile(file, textWriter, productType, filenameRegex, sstDepth, startDate, endDate, temporalResolution, regionMask, regionIndex, timeSteps);
        }

        if (textWriter != null) {
            textWriter.close();
        }
    }

    private PrintWriter getTextWriter(boolean writeText) throws IOException {
        final PrintWriter writer;
        if (writeText) {
            String fileName = String.format("%s-output-%s.txt", TOOL_NAME, new SimpleDateFormat("yyyyMMdd'T'hhmmss").format(new Date()));
            writer = new PrintWriter(new FileWriter(fileName));
        } else {
            writer = null;
        }
        return writer;
    }

    private static void writeOutputFile(File file,
                                        PrintWriter textWriter,
                                        ProductType productType,
                                        String filenameRegex, SstDepth sstDepth,
                                        Date startDate,
                                        Date endDate,
                                        TemporalResolution temporalResolution,
                                        RegionMask regionMask, int regionIndex,
                                        List<Aggregator.TimeStep> timeSteps) throws IOException {

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
            netcdfFile.addGlobalAttribute("region_name", regionMask.getName());
            netcdfFile.addGlobalAttribute("filename_regex", filenameRegex);

            int numSteps = timeSteps.size();
            Dimension timeDimension = netcdfFile.addDimension("time", numSteps, true, false, false);
            Dimension[] dims = {timeDimension};

            Variable startTimeVar = netcdfFile.addVariable("start_time", DataType.FLOAT, dims);
            startTimeVar.addAttribute(new Attribute("units", "seconds"));
            startTimeVar.addAttribute(new Attribute("long_name", "reference start time of averaging period in seconds until 1981-01-01T00:00:00"));

            Variable endTimeVar = netcdfFile.addVariable("end_time", DataType.FLOAT, dims);
            endTimeVar.addAttribute(new Attribute("units", "seconds"));
            endTimeVar.addAttribute(new Attribute("long_name", "reference end time of averaging period in seconds until 1981-01-01T00:00:00"));

            Variable[] outputVariables = productType.getFileType().createOutputVariables(netcdfFile, sstDepth, dims);
            Array[] outputArrays = new Array[outputVariables.length];
            for (int i = 0; i < outputVariables.length; i++) {
                Variable outputVariable = outputVariables[i];
                outputArrays[i] = Array.factory(outputVariable.getDataType(), new int[]{numSteps});
            }

            long millisSince1981 = UTC.createCalendar(1981).getTimeInMillis();

            float[] startTime = new float[numSteps];
            float[] endTime = new float[numSteps];
            for (int t = 0; t < numSteps; t++) {
                Aggregator.TimeStep timeStep = timeSteps.get(t);
                startTime[t] = (timeStep.getStartDate().getTime() - millisSince1981) / 1000.0F;
                endTime[t] = (timeStep.getEndDate().getTime() - millisSince1981) / 1000.0F;
                Number[] results = timeStep.getRegionalAggregationResults(regionIndex);
                for (int i = 0; i < results.length; i++) {
                    outputArrays[i].setObject(t, results[i]);
                }
            }

            netcdfFile.create();

            netcdfFile.write(startTimeVar.getName(), Array.factory(DataType.FLOAT, new int[]{numSteps}, startTime));
            netcdfFile.write(endTimeVar.getName(), Array.factory(DataType.FLOAT, new int[]{numSteps}, endTime));
            for (int i = 0; i < outputVariables.length; i++) {
                Variable outputVariable = outputVariables[i];
                netcdfFile.write(outputVariable.getName(), outputArrays[i]);
            }

            if (textWriter != null) {
                outputText(textWriter, getNames(outputVariables), regionMask.getName(), regionIndex, timeSteps);
            }

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

    private static void outputText(PrintWriter textWriter, String[] outputNames, String regionName, int regionIndex, List<Aggregator.TimeStep> timeSteps) {
        textWriter.println();
        textWriter.printf("%s\t%s\t%s\t%s\t%s\n", "region", "start", "end", "step", cat(outputNames, "\t"));
        DateFormat dateFormat = UTC.getDateFormat("yyyy-MM-dd");
        for (int t = 0; t < timeSteps.size(); t++) {
            Aggregator.TimeStep timeStep = timeSteps.get(t);
            textWriter.printf("%s\t%s\t%s\t%s\t%s\n",
                              regionName,
                              dateFormat.format(timeStep.getStartDate()),
                              dateFormat.format(timeStep.getEndDate()),
                              t + 1,
                              cat(timeStep.getRegionalAggregationResults(regionIndex), "\t"));
        }
    }

    public static String cat(Object[] values, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(value);
        }
        return sb.toString();
    }

    private RegionMaskList parseRegionList(Configuration configuration) throws ToolException {
        try {
            return RegionMaskList.parse(configuration.getString(PARAM_REGION_LIST, false));
        } catch (Exception e) {
            throw new ToolException(e, ExitCode.USAGE_ERROR);
        }
    }

    public static String[] getNames(Variable[] vars) {
        String[] names = new String[vars.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = vars[i].getName();
        }
        return names;
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
     * @param sstType              SST Type
     * @param productString        Product String (see Table 5 in PSD) // todo - find out from PSD what productString is
     * @param additionalSegregator Additional Segregator = LT or DM  // todo - find out from PSD what additionalSegregator is
     * @return The filename.
     */
    public static String getOutputFilename(String startOfPeriod, String endOfPeriod, String regionName, ProcessingLevel processingLevel, String sstType, String productString, String additionalSegregator) {

        return String.format("%s-%s-%s_average-ESACCI-%s_GHRSST-%s-%s-%s-v%s-fv%s.nc",
                             startOfPeriod,
                             endOfPeriod,
                             regionName,
                             processingLevel,
                             sstType,
                             productString,
                             additionalSegregator,
                             TOOL_VERSION,
                             FILE_FORMAT_VERSION);
    }

}
