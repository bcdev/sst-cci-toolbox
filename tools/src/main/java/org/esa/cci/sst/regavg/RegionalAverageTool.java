package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.ProductTree;
import org.esa.cci.sst.util.UTC;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ExitCode;
import org.esa.cci.sst.tool.Parameter;
import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.tool.ToolException;
import ucar.ma2.Array;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
        for (int i = 0; i < values.length; i++) {
            ProductType value = values[i];
            paramList.add(new Parameter(value.name() + ".dir", "DIR", null, "Directory that hosts the products of type '" + value.name() + "'."));
        }
        return paramList.toArray(new Parameter[0]);
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

        Climatology climatology = Climatology.open(climatologyDir);
        Climatology.Dataset[] datasets = climatology.getDatasets();
        ProductTree productTree = collectFiles(productType, productDir);
        try {
            average(productType, productTree, startDate, endDate, temporalResolution);
        } catch (IOException e) {
            throw new ToolException("Averaging failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }
    }

    private void average(ProductType productType, ProductTree productTree, Date startDate, Date endDate, TemporalResolution temporalResolution) throws IOException {
        Calendar calendar = UTC.createCalendar(startDate);
        while (calendar.getTime().before(endDate)) {
            Date date1 = calendar.getTime();
            if (temporalResolution == TemporalResolution.daily) {
                calendar.add(Calendar.DATE, 1);
            } else if (temporalResolution == TemporalResolution.monthly) {
                calendar.add(Calendar.MONTH, 1);
            } else if (temporalResolution == TemporalResolution.seasonal) {
                calendar.add(Calendar.MONTH, 3);
            } else /*if (temporalResolution == TemporalResolution.anual)*/ {
                calendar.add(Calendar.YEAR, 1);
            }
            Date date2 = calendar.getTime();

            computeOutputTimePoint(productType, productTree, temporalResolution, date1, date2);
        }
    }

    private void computeOutputTimePoint(ProductType productType, ProductTree productTree, TemporalResolution temporalResolution, Date date1, Date date2) throws IOException {
        DateFormat isoDateFormat = UTC.getIsoFormat();
        System.out.printf("Computing output point for time range from %s to %s%n",
                          isoDateFormat.format(date1), isoDateFormat.format(date2));

        if (temporalResolution.ordinal() <= TemporalResolution.monthly.ordinal()) {
            Calendar calendar = UTC.createCalendar(date1);
            while (calendar.getTime().before(date2)) {
                File[] files = productTree.get(calendar.getTime());
                if (files.length > 0) {
                    System.out.println("  " + files.length + " file(s) found");
                    Aggregator aggregator = new Aggregator();
                    aggregator.add("sst_skin", new Aggregator.MeanAcccumulator());
                    aggregator.add("uncertainty", new Aggregator.MeanSqrAcccumulator());
                    aggregator.aggregate(files, new Aggregator.Processor() {
                        @Override
                        public void process(String name, int x, int y, int w, int h, Array result) {
                            System.out.println("Computed average for " + name + ", line " + y);

                        }
                    });
                }
                calendar.add(Calendar.DATE, 1);
            }
        } else {
            Calendar calendar = UTC.createCalendar(date1);
            while (calendar.getTime().before(date2)) {
                File[] files = productTree.get(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
                new Aggregator().aggregate(files, new Aggregator.Processor() {
                    @Override
                    public void process(String name, int x, int y, int w, int h, Array result) {

                    }
                });
                calendar.add(Calendar.MONTH, 1);
            }
        }
    }

    private ProductTree collectFiles(ProductType productType, String... inputPaths) {
        ProductTree productTree = new ProductTree();

        for (String inputPath : inputPaths) {
            collectFiles(productType, new File(inputPath), productTree);
        }

        return productTree;
    }

    private void collectFiles(ProductType productType, File file, ProductTree productTree) {
        if (file.isDirectory()) {
            File[] files1 = file.listFiles(new InputFileFilter());
            if (files1 != null) {
                for (File file1 : files1) {
                    collectFiles(productType, file1, productTree);
                }
            }
        } else if (file.isFile()) {
            productTree.add(productType.getDate(file.getName()), file);
        } else {
            warn("Not a file or file does not exist: " + file);
        }
    }

    private static class InputFileFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isDirectory()
                    || file.getName().endsWith(".nc")
                    || file.getName().endsWith(".nc.gz");
        }
    }


}
