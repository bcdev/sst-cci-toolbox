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

import org.esa.cci.sst.common.AggregationContext;
import org.esa.cci.sst.common.LUT;
import org.esa.cci.sst.common.RegionMaskList;
import org.esa.cci.sst.common.SpatialResolution;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.common.calculator.SynopticUncertaintyProvider;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.file.FileStore;
import org.esa.cci.sst.common.file.ProductType;
import org.esa.cci.sst.tool.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * The SST-CCI re-gridding tool.
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public class RegriddingTool extends Tool {

    private static final String FILE_FORMAT_VERSION = "1.0";
    private static final String TOOL_NAME = "regrid";
    private static final String TOOL_VERSION = "2.0";
    private static final String TOOL_HEADER = "\n" + "The " + TOOL_NAME + " tool is used to read in the SST CCI L3U, L3C, and L4 products at daily 0.05 ° " +
                                              "latitude by longitude resolution and output on other spatio-temporal resolutions, which are a multiple" +
                                              "of this and divide neatly into 180 degrees. Output are SSTs and their uncertainties.";

    public static final Parameter PARAM_SST_DEPTH = new Parameter("sstDepth", "DEPTH", SstDepth.skin.name(),
                                                                  "The SST depth. Must be one of " + Arrays.toString(
                                                                          SstDepth.values()) + ".");

    public static final Parameter PARAM_SPATIAL_RESOLUTION = new Parameter("spatialRes", "NUM",
                                                                           SpatialResolution.getDefaultResolutionAsString(),
                                                                           "The spatial resolution of the output grid in degrees. Must be one of " + SpatialResolution.getAllResolutionsAsString() + ".");

    public static final Parameter PARAM_TEMPORAL_RES = new Parameter("temporalRes", "NUM",
                                                                     TemporalResolution.monthly + "",
                                                                     "The temporal resolution. Must be one of " + Arrays.toString(
                                                                             TemporalResolution.values()) + ".");

    private static final Parameter PARAM_REGION = new Parameter("region", "REGION",
                                                                "Global=-180,90,180,-90 (NAME=REGION)",
                                                                "The sub-region to be used (optional). Coordinates in the format W,N,E,S.");

    public static final Parameter PARAM_PRODUCT_TYPE = new Parameter("productType", "NAME", null,
                                                                     "The product type. Must be one of " + Arrays.toString(
                                                                             ProductType.values()) + ".");

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

    private static final Parameter PARAM_MAX_TOTAL_UNCERTAINTY = new Parameter("maxTotalUncertainty", "NUM", "0.0",
                                                                               "The maximum relative total uncertainty allowed for non-missing output, if greater than zero.",
                                                                               true);

    public static final Parameter PARAM_COVERAGE_UNCERTAINTY_FILE_STDDEV = new Parameter("coverageUncertainty.StdDev",
                                                                                         "FILE",
                                                                                         "./config/auxdata/20070321-UKMO-L4HRfnd-GLOB-v01-fv02-OSTIARANanom_stdev.nc",
                                                                                         "A NetCDF file that provides lookup table 1/3 for coverage uncertainties.");

    public static final Parameter PARAM_COVERAGE_UNCERTAINTY_FILE_X0TIME = new Parameter("coverageUncertainty.x0Time",
                                                                                         "FILE",
                                                                                         "./config/auxdata/x0_time.txt",
                                                                                         "A txt file that provides lookup table 2/3 for coverage uncertainties.");

    public static final Parameter PARAM_COVERAGE_UNCERTAINTY_FILE_X0SPACE = new Parameter("coverageUncertainty.x0Space",
                                                                                          "FILE",
                                                                                          "./config/auxdata/x0_space.txt",
                                                                                          "A txt file that provides lookup table 3/3 for coverage uncertainties.");

    private ProductType productType;


    public static void main(String[] args) {
        new RegriddingTool().run(args);
    }

    @Override
    protected void run(Configuration configuration, String[] arguments) throws OldToolException {
        final String resolutionString = configuration.getString(PARAM_SPATIAL_RESOLUTION, true);
        final SpatialResolution spatialResolution = SpatialResolution.getSpatialResolution(resolutionString);
        productType = ProductType.valueOf(configuration.getString(PARAM_PRODUCT_TYPE, true));

        final String sourceFilenameRegex = configuration.getString(PARAM_FILENAME_REGEX.getName(),
                                                                   productType.getDefaultFilenameRegex(), false);

        final SstDepth sstDepth = SstDepth.valueOf(configuration.getString(PARAM_SST_DEPTH, true));
        final String productDir = configuration.getString(productType + ".dir", ".", true);
        final Date startDate = configuration.getDate(PARAM_START_DATE, true);
        final Date endDate = configuration.getDate(PARAM_END_DATE, true);
        final TemporalResolution temporalResolution = TemporalResolution.valueOf(
                configuration.getString(PARAM_TEMPORAL_RES, true));
        final File targetDir = configuration.getExistingDirectory(PARAM_OUTPUT_DIR, true);
        final RegionMaskList regionMaskList = getRegionMaskList(configuration);
        final double minCoverage = Double.parseDouble(configuration.getString(PARAM_MIN_COVERAGE, false));
        final File cuStdDevFile = configuration.getExistingFile(PARAM_COVERAGE_UNCERTAINTY_FILE_STDDEV, true);
        final File cuTimeFile = configuration.getExistingFile(PARAM_COVERAGE_UNCERTAINTY_FILE_X0TIME, true);
        final File cuSpaceFile = configuration.getExistingFile(PARAM_COVERAGE_UNCERTAINTY_FILE_X0SPACE, true);

        final boolean totalUncertainty = configuration.getBoolean(PARAM_TOTAL_UNCERTAINTY, true);
        final double maxTotalUncertainty = Double.parseDouble(
                configuration.getString(PARAM_MAX_TOTAL_UNCERTAINTY, false));

        final File climatologyDir = configuration.getExistingDirectory(PARAM_CLIMATOLOGY_DIR, true);
        final Climatology climatology = Climatology.create(climatologyDir, productType.getGridDef());

        final FileStore fileStore = FileStore.create(productType, sourceFilenameRegex, productDir);
        final LUT stdDevLut = createLutForStdDeviation(cuStdDevFile);
        final LUT cuTimeLut = getLutCoverageUncertainty(cuTimeFile, spatialResolution, -32768.0);
        final LUT cuSpaceLut = getLutCoverageUncertainty(cuSpaceFile, spatialResolution, 0.0);
        final SynopticUncertaintyProvider synopticUncertaintyProvider = new SynopticUncertaintyProvider(
                spatialResolution, temporalResolution);

        final AggregationContext aggregationContext = new AggregationContext();
        final GridDef targetGridDef = GridDef.createGlobal(spatialResolution.getResolution());
        aggregationContext.setTargetGridDef(targetGridDef);
        aggregationContext.setStandardDeviationGrid(stdDevLut.getGrid());
        aggregationContext.setSynopticUncertaintyProvider(synopticUncertaintyProvider);
        aggregationContext.setMinCoverage(minCoverage);
        aggregationContext.setTargetRegionMaskList(regionMaskList);

        final Writer writer = new Writer(
                productType, TOOL_NAME, TOOL_VERSION, FILE_FORMAT_VERSION, totalUncertainty, maxTotalUncertainty,
                targetDir, sourceFilenameRegex, sstDepth, temporalResolution, regionMaskList.get(0));
        final RegriddingAggregator aggregator = new RegriddingAggregator(fileStore,
                                                                         climatology,
                                                                         sstDepth,
                                                                         aggregationContext,
                                                                         cuTimeLut,
                                                                         cuSpaceLut);
        try {
            aggregator.aggregate(startDate, endDate, temporalResolution, writer);
        } catch (IOException e) {
            throw new OldToolException("Re-gridding failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }
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
    protected String getToolHome() {
        return System.getProperty(getName() + ".home", ".");
    }

    @Override
    protected Parameter[] getParameters() {
        ArrayList<Parameter> paramList = new ArrayList<Parameter>();
        paramList.addAll(Arrays.asList(
                PARAM_REGION,
                PARAM_CLIMATOLOGY_DIR,
                PARAM_MAX_TOTAL_UNCERTAINTY,
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
                PARAM_TEMPORAL_RES));

        ProductType[] values = ProductType.values();
        for (ProductType value : values) {
            paramList.add(new Parameter(value.name() + ".dir", "DIR", null,
                                        "Directory that hosts the products of type '" + value.name() + "'."));
        }

        return paramList.toArray(new Parameter[paramList.size()]);
    }

    private RegionMaskList getRegionMaskList(Configuration configuration) throws OldToolException {
        try {
            final String region = configuration.getString(PARAM_REGION, false);
            RegionMaskList.setSpatialResolution(
                    SpatialResolution.getSpatialResolution(configuration.getString(PARAM_SPATIAL_RESOLUTION, true)));
            return RegionMaskList.parse(region);
        } catch (Exception e) {
            throw new OldToolException(e, ExitCode.USAGE_ERROR);
        }
    }

    private LUT createLutForStdDeviation(File file) throws OldToolException {
        LUT lut;
        try {
            lut = RegriddingLUT1.create(file, productType.getGridDef());
            LOGGER.info(String.format("LUT read from '%s'", file));
        } catch (IOException e) {
            throw new OldToolException(e, ExitCode.IO_ERROR);
        }
        return lut;
    }

    private LUT getLutCoverageUncertainty(File file, SpatialResolution spatialResolution,
                                                     double fillValue) throws OldToolException {
        LUT lut;
        try {
            lut = RegriddingLUT2.create(file, spatialResolution, fillValue);
            LOGGER.info(String.format("LUT read from '%s'", file));
        } catch (IOException e) {
            throw new OldToolException(e, ExitCode.IO_ERROR);
        }
        return lut;
    }

}
