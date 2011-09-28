package org.esa.cci.sst.regavg;

import org.esa.cci.sst.tool.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * The SST_cci Regional-Average tool.
 *
 * @author Norman Fomferra
 */
public class RegionalAverageTool extends Tool {

    private static final String TOOL_NAME = "regavg";
    private static final String TOOL_VERSION = TOOL_NAME + ", version 0.1 (C) 2011-2013 by the ESA SST_cci project";
    private static final String TOOL_SYNTAX = TOOL_NAME + " [OPTION]...";
    private static final String TOOL_HEADER = "\n" +
            "This tool is used to generate regional average time-series from an input set of 0.05 deg. ARC files, " +
            "and SST_cci L3U and L3C given in the specified DIR or as list of FILEs. OPTION may be one or more of " +
            "the following:\n";

    public static final Parameter PARAM_SST_DEPTH = new Parameter("sstDepth", "NUM", "0.2", "The SST depth in meters.");
    public static final Parameter PARAM_REGION_LIST = new Parameter("regionList", "NAME=REGION;...", "Global=-180,90,180,-90",
                                                                    "A semicolon-separated list of NAME=REGION pairs. " +
            "REGION may be given as coordinates in the format W,N,E,S or as name of a text file that provides a region mask." +
                                                                            " The region mask file contains 72 columns x 36 lines where a zero character indicates 5-degree cells that shall not be used in the averaging process.");
    public static final Parameter PARAM_REGION_NAME = new Parameter("regionName", "NAME", "Global", "The name of a predefined region.");
    public static final Parameter PARAM_REGION_WKT = new Parameter("regionWKT", "WKT", "POLYGON((-180 -90, 180 -90, 180 90, -180 90, -180 -90))", "The region geometry given in geometry well-known-text (WKT) format");
    public static final Parameter PARAM_REGION_MASK = new Parameter("regionMask", "FILE", null, "The region given as mask. Must be NetCDF file containing a global, numerical 0.05 degree mask named 'region_mask'. Zero values indicate grid cells that will not be used.");
    public static final Parameter PARAM_START_DATE = new Parameter("startDate", "DATE", "1990-01-01", "The start date for the analysis given in the format YYYYMMDD");
    public static final Parameter PARAM_END_DATE = new Parameter("endDate", "DATE", "2020-12-31", "The end date for the analysis given in the format YYYYMMDD");
    public static final Parameter PARAM_CLIMATOLOGY_DIR = new Parameter("climatologyDir", "DIR", "./climatology", "The directory path to the reference climatology.");
    public static final Parameter PARAM_LUT_DIR = new Parameter("lutPath", "DIR", "./lut", "The directory path to the variance LUTs.");
    public static final Parameter PARAM_TEMPORAL_RES = new Parameter("temporalRes", "NUM", TemporalResolution.monthly + "", "The temporal resolution. Must be one of " + Arrays.toString(TemporalResolution.values()) + ".");
    public static final Parameter PARAM_PRODUCT_TYPE = new Parameter("productType", "NAME", "ARC", "The product type. Must be one of " + Arrays.toString(ProductType.values()) + ".");
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
    protected Parameter[] getParameters() {
        ArrayList<Parameter> paramList = new ArrayList<Parameter>();
        paramList.addAll(Arrays.asList(
                PARAM_SST_DEPTH,
                PARAM_TEMPORAL_RES,
                PARAM_REGION_LIST,
                PARAM_REGION_NAME,
                PARAM_REGION_WKT,
                PARAM_REGION_MASK,
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
        RegionList regionList = parseRegionList(configuration);

        Climatology climatology = Climatology.open(climatologyDir);
        Climatology.Dataset[] datasets = climatology.getDatasets();
        ProductStore productStore = ProductStore.create(productType, productDir);
        try {
            RegionalAverager.computeOutputTimeSteps(productStore, startDate, endDate, temporalResolution, regionList);
        } catch (IOException e) {
            throw new ToolException("Averaging failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }
    }

    private RegionList parseRegionList(Configuration configuration) throws  ToolException {
        try {
            return RegionList.parse(configuration.getString(PARAM_REGION_LIST, false));
        } catch (Exception e) {
            throw new ToolException(e, ExitCode.USAGE_ERROR);
        }
    }

}
