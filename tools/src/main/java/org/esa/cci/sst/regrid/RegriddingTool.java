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

import org.esa.cci.sst.common.RegionMaskList;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.common.file.FileStore;
import org.esa.cci.sst.regrid.auxiliary.LutForStdDeviation;
import org.esa.cci.sst.regrid.auxiliary.LutForSynopticAreas;
import org.esa.cci.sst.regrid.auxiliary.LutForXTimeSpace;
import org.esa.cci.sst.tool.*;
import org.esa.cci.sst.util.ProductType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

    private static final Parameter PARAM_MIN_COVERAGE = new Parameter("minCoverage", "NUM", "0.0",
            "The minimum fractional coverage required for non-missing output. " +
                    "(fraction of valid values in input per grid box in output) ");

    private static final Parameter PARAM_MAX_UNCERTAINTY = new Parameter("maxTotalUncertainty", "NUM", "1.0",
            "The maximum relative total uncertainty allowed for non-missing output.", true);

    public static final Parameter PARAM_COVERAGE_UNCERTAINTY_FILE_STDDEV = new Parameter("coverageUncertainty.StdDev", "FILE",
            "./conf/auxdata/20070321-UKMO-L4HRfnd-GLOB-v01-fv02-OSTIARANanom_stdev.nc",
            "A NetCDF file that provides lookup table 1/3 for coverage uncertainties.");

    public static final Parameter PARAM_COVERAGE_UNCERTAINTY_FILE_X0TIME = new Parameter("coverageUncertainty.x0Time", "FILE",
            "./conf/auxdata/x0_time.txt",
            "A txt file that provides lookup table 2/3 for coverage uncertainties.");

    public static final Parameter PARAM_COVERAGE_UNCERTAINTY_FILE_X0SPACE = new Parameter("coverageUncertainty.x0Space", "FILE",
            "./conf/auxdata/x0_space.txt",
            "A txt file that provides lookup table 3/3 for coverage uncertainties.");

    public static final Parameter PARAM_SYNOPTIC_CORRELATION_FILE = new Parameter("synopticCorrelation", "FILE",
            "./conf/auxdata/TBD.nc",
            "A NetCDF file that provides lookup table for synoptically correlated uncertainties."); //todo bs add LUT

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
        final File lutCuFileStddev = configuration.getExistingFile(PARAM_COVERAGE_UNCERTAINTY_FILE_STDDEV, true);
        final File lutCuFileTime = configuration.getExistingFile(PARAM_COVERAGE_UNCERTAINTY_FILE_X0TIME, true);
        final File lutCuFileSpace = configuration.getExistingFile(PARAM_COVERAGE_UNCERTAINTY_FILE_X0SPACE, true);
        final File lut3File = configuration.getExistingFile(PARAM_SYNOPTIC_CORRELATION_FILE, true);
        boolean totalUncertainty = checkTotalUncertainty(configuration.getBoolean(PARAM_TOTAL_UNCERTAINTY, true));
        double maxTotalUncertainty = Double.parseDouble(configuration.getString(PARAM_MAX_UNCERTAINTY, false));

        Climatology climatology = Climatology.create(climatologyDir, productType.getGridDef());
        FileStore fileStore = FileStore.create(productType, filenameRegex, productDir);
        LutForStdDeviation lutCuStddev = createLutForStdDeviation(lutCuFileStddev);
        LutForXTimeSpace lutCuTime = getLutCoverageUncertainty(lutCuFileTime, spatialResolution, -32768.0);
        LutForXTimeSpace lutCuSpace = getLutCoverageUncertainty(lutCuFileSpace, spatialResolution, 0.0);
        LutForSynopticAreas lutSynopticAreas = new LutForSynopticAreas(temporalResolution, spatialResolution);

        List<RegriddingTimeStep> timeSteps;
        try {
            Aggregator4Regrid aggregator = new Aggregator4Regrid(regionMaskList, fileStore, climatology,
                    lutSynopticAreas, lutCuStddev, lutCuTime, lutCuSpace, sstDepth, minCoverage, spatialResolution);
            timeSteps = aggregator.aggregate(startDate, endDate, temporalResolution);
        } catch (IOException e) {
            throw new ToolException("Regridding failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }

        try {
            RegriddingOutputFileWriter outputWriter = new RegriddingOutputFileWriter(
                    productType, TOOL_NAME, TOOL_VERSION, FILE_FORMAT_VERSION, totalUncertainty, maxTotalUncertainty);
            outputWriter.writeOutputs(outputDir, filenameRegex, sstDepth, temporalResolution, regionMaskList.get(0), timeSteps);
        } catch (IOException e) {
            throw new ToolException("Writing of output failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }
    }

    private boolean checkTotalUncertainty(boolean totalUncertainty) throws ToolException {
        boolean totalUncertaintyPossible = RegriddingOutputFileWriter.isTotalUncertaintyPossible(productType);
        if (totalUncertainty && !totalUncertaintyPossible) {
            throw new ToolException("Parameter 'totalUncertainty' is only available for CCI-L3 products.", ExitCode.USAGE_ERROR);
        }
        return totalUncertainty;
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
        paramList.addAll(Arrays.asList(
                PARAM_REGION,
                PARAM_CLIMATOLOGY_DIR,
                PARAM_MAX_UNCERTAINTY,
                PARAM_TOTAL_UNCERTAINTY,
                PARAM_SPATIAL_RESOLUTION,
                PARAM_START_DATE,
                PARAM_END_DATE,
                PARAM_FILENAME_REGEX,
                PARAM_SST_DEPTH,
                PARAM_OUTPUT_DIR,
                PARAM_PRODUCT_TYPE,
                PARAM_COVERAGE_UNCERTAINTY_FILE_STDDEV,
                PARAM_COVERAGE_UNCERTAINTY_FILE_X0TIME,
                PARAM_COVERAGE_UNCERTAINTY_FILE_X0SPACE,
                PARAM_MIN_COVERAGE,
                PARAM_SYNOPTIC_CORRELATION_FILE,
                PARAM_TEMPORAL_RES));

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

    private LutForStdDeviation createLutForStdDeviation(File lutFile) throws ToolException {
        LutForStdDeviation lutForStdDeviation;
        try {
            lutForStdDeviation = LutForStdDeviation.create(lutFile, productType.getGridDef());
            LOGGER.info(String.format("LUT read from '%s'", lutFile));
        } catch (IOException e) {
            throw new ToolException(e, ExitCode.IO_ERROR);
        }
        return lutForStdDeviation;
    }

    private LutForXTimeSpace getLutCoverageUncertainty(File lutFile, SpatialResolution spatialResolution, double fillValue) throws ToolException {
        LutForXTimeSpace lutForXTimeSpace;
        try {
            lutForXTimeSpace = LutForXTimeSpace.read(lutFile, spatialResolution, fillValue);
            LOGGER.info(String.format("LUT read from '%s'", lutFile));
        } catch (IOException e) {
            throw new ToolException(e, ExitCode.IO_ERROR);
        }
        return lutForXTimeSpace;
    }

}
