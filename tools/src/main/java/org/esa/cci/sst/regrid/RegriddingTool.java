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

import org.esa.cci.sst.common.*;
import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.common.calculator.SynopticUncertaintyProvider;
import org.esa.cci.sst.common.file.FileStore;
import org.esa.cci.sst.common.file.ProductType;
import org.esa.cci.sst.tool.*;
import org.esa.cci.sst.util.FileUtil;

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
            "The maximum relative total uncertainty allowed for non-missing output, if greater than zero.");

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
    protected void run(Configuration configuration, String[] arguments) throws ToolException {
        final String productTypeValue = configuration.getMandatoryStringValue(PARAM_PRODUCT_TYPE.getName(), PARAM_PRODUCT_TYPE.getDefaultValue());
        productType = ProductType.valueOf(productTypeValue);


        final RegionMaskList regionMaskList = getRegionMaskList(configuration);


        final boolean totalUncertainty = configuration.getBooleanValue(PARAM_TOTAL_UNCERTAINTY.getName());
        final double maxTotalUncertainty = configuration.getDoubleValue(PARAM_MAX_TOTAL_UNCERTAINTY.getName(), 0.0);

        final String toolHome = configuration.getToolHome();
        final String climatologyDirPath = configuration.getMandatoryStringValue(PARAM_CLIMATOLOGY_DIR.getName(), PARAM_CLIMATOLOGY_DIR.getDefaultValue());
        final File climatologyDir = FileUtil.getExistingFile(climatologyDirPath, toolHome);
        final Climatology climatology = Climatology.create(climatologyDir, productType.getGridDef());

        final String productDir = configuration.getMandatoryStringValue(productType + ".dir", ".");
        final String sourceFilenameRegex = configuration.getStringValue(PARAM_FILENAME_REGEX.getName(), productType.getDefaultFilenameRegex());
        final FileStore fileStore = FileStore.create(productType, sourceFilenameRegex, productDir);

        final String stdefFilePath = configuration.getMandatoryStringValue(PARAM_COVERAGE_UNCERTAINTY_FILE_STDDEV.getName(), PARAM_COVERAGE_UNCERTAINTY_FILE_STDDEV.getDefaultValue());
        final File cuStdDevFile = FileUtil.getExistingFile(stdefFilePath, toolHome);
        final LUT stdDevLut = createLutForStdDeviation(cuStdDevFile);

        final String resolutionString = configuration.getMandatoryStringValue(PARAM_SPATIAL_RESOLUTION.getName(), PARAM_SPATIAL_RESOLUTION.getDefaultValue());
        final SpatialResolution spatialResolution = SpatialResolution.getSpatialResolution(resolutionString);

        final String x0TimeFilePath = configuration.getMandatoryStringValue(PARAM_COVERAGE_UNCERTAINTY_FILE_X0TIME.getName(), PARAM_COVERAGE_UNCERTAINTY_FILE_X0TIME.getDefaultValue());
        final File cuTimeFile = FileUtil.getExistingFile(x0TimeFilePath, toolHome);
        final LUT cuTimeLut = getLutCoverageUncertainty(cuTimeFile, spatialResolution, -32768.0);

        final String x0SpaceFilePath = configuration.getMandatoryStringValue(PARAM_COVERAGE_UNCERTAINTY_FILE_X0SPACE.getName(), PARAM_COVERAGE_UNCERTAINTY_FILE_X0SPACE.getDefaultValue());
        final File cuSpaceFile = FileUtil.getExistingFile(x0SpaceFilePath, toolHome);
        final LUT cuSpaceLut = getLutCoverageUncertainty(cuSpaceFile, spatialResolution, 0.0);

        final String temporalResolutionValue = configuration.getMandatoryStringValue(PARAM_TEMPORAL_RES.getName(), PARAM_TEMPORAL_RES.getDefaultValue());
        final TemporalResolution temporalResolution = TemporalResolution.valueOf(temporalResolutionValue);

        final SynopticUncertaintyProvider synopticUncertaintyProvider = new SynopticUncertaintyProvider(
                spatialResolution, temporalResolution);

        final double minCoverage = configuration.getDoubleValue(PARAM_MIN_COVERAGE.getName(), 0.0);

        final AggregationContext aggregationContext = new AggregationContext();
        final GridDef targetGridDef = GridDef.createGlobal(spatialResolution.getResolution());
        aggregationContext.setTargetGridDef(targetGridDef);
        aggregationContext.setStandardDeviationGrid(stdDevLut.getGrid());
        aggregationContext.setSynopticUncertaintyProvider(synopticUncertaintyProvider);
        aggregationContext.setMinCoverage(minCoverage);
        aggregationContext.setTargetRegionMaskList(regionMaskList);

        final String outputDirPath = configuration.getMandatoryStringValue(PARAM_OUTPUT_DIR.getName(), PARAM_OUTPUT_DIR.getDefaultValue());
        final File targetDir = FileUtil.getExistingDirectory(outputDirPath, toolHome);

        final String sstDepthValue = configuration.getMandatoryStringValue(PARAM_SST_DEPTH.getName(), PARAM_SST_DEPTH.getDefaultValue());
        final SstDepth sstDepth = SstDepth.valueOf(sstDepthValue);

        final Date startDate = configuration.getMandatoryShortUtcDateValue(PARAM_START_DATE.getName(), PARAM_START_DATE.getDefaultValue());
        final Date endDate = configuration.getMandatoryShortUtcDateValue(PARAM_END_DATE.getName(), PARAM_END_DATE.getDefaultValue());

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
            throw new ToolException("Re-gridding failed: " + e.getMessage(), e, ToolException.TOOL_IO_ERROR);
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

    private RegionMaskList getRegionMaskList(Configuration configuration) throws ToolException {
        try {
            final String region = configuration.getStringValue(PARAM_REGION.getName(), PARAM_REGION.getDefaultValue());
            final String spatialResolutionValue = configuration.getMandatoryStringValue(PARAM_SPATIAL_RESOLUTION.getName(), PARAM_SPATIAL_RESOLUTION.getDefaultValue());
            RegionMaskList.setSpatialResolution(SpatialResolution.getSpatialResolution(spatialResolutionValue));
            return RegionMaskList.parse(region);
        } catch (Exception e) {
            throw new ToolException(e, ToolException.TOOL_USAGE_ERROR);
        }
    }

    private LUT createLutForStdDeviation(File file) throws ToolException {
        LUT lut;
        try {
            lut = RegriddingLUT1.create(file, productType.getGridDef());
            logger.info(String.format("LUT read from '%s'", file));
        } catch (IOException e) {
            throw new ToolException(e, ToolException.TOOL_IO_ERROR);
        }
        return lut;
    }

    private LUT getLutCoverageUncertainty(File file, SpatialResolution spatialResolution, double fillValue) throws ToolException {
        LUT lut;
        try {
            lut = RegriddingLUT2.create(file, spatialResolution, fillValue);
            logger.info(String.format("LUT read from '%s'", file));
        } catch (IOException e) {
            throw new ToolException(e, ToolException.TOOL_IO_ERROR);
        }
        return lut;
    }

}
