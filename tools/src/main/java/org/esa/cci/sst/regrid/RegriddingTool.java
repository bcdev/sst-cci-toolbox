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

import org.esa.cci.sst.tool.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class RegriddingTool extends Tool {
    private static final String TOOL_NAME = "regrid";
    private static final String TOOL_VERSION = "0.1";
    private static final String TOOL_HEADER = "\n" + "The " + TOOL_NAME + " tool is used to read in the SST CCI L3U, L3C, and L4 products at daily 0.05° " +
            "latitude by longitude resolution and output on other spatio-temporal resolutions, which are a multiple" +
            "of this and divide neatly into 180 degrees. Output are SSTs and their uncertainties.";
    private static final String TOOL_FOOTER = "";

    //important for input selection of which files ("product types") should be regridded.
//    private static final Parameter PARAM_SST_DEPTH = new Parameter("sstDepth", "DEPTH", SstDepth.sea_surface_temperature.name(),
//            "The SST depth. Must be one of " + Arrays.toString(SstDepth.values()) + ".");

    private static final Parameter PARAM_SPATIAL_RESOLUTION = new Parameter("spatialRes", "NUM", SpatialResolution.getDefaultValueAsString(), "The spatial " +
            "resolution of the output grid in degrees. Must be one of "
            + SpatialResolution.getValuesAsString() + ".");

    private static final Parameter PARAM_REGION = new Parameter("region", "REGION", "-180,90,180,-90",
            "The sub-region to be used (optional). Must be a list of coordinates in the format W,N,E,S.");

    public static final Parameter PARAM_PRODUCT_TYPE = new Parameter("productType", "NAME", null,
            "The product type. Must be one of " + Arrays.toString(ProductType.values()) + ".");

    public static final Parameter PARAM_OUTPUT_DIR = new Parameter("outputDir", "DIR", ".", "The output directory.");

    public static final Parameter PARAM_START_DATE = new Parameter("startDate", "DATE", "1990-01-01",
            "The start date for the analysis given in the format YYYY-MM-DD");

    public static final Parameter PARAM_END_DATE = new Parameter("endDate", "DATE", "2020-12-31",
            "The end date for the analysis given in the format YYYY-MM-DD");

//    private static final Parameter PARAM_TOTAL_UNCERTAINTY = new Parameter("totalUncertainty", "BOOL", "false", "A Boolean variable indicating whether total or " +
//            "separated uncertainties are written to the output file. Must be either 'true' or 'false'.");
//
//    private static final Parameter PARAM_CLIMATOLOGY_DIR = new Parameter("climatologyDir", "DIR", "./climatology", "The directory path to the reference climatology.");
//
//    private static final Parameter PARAM_MIN_COVERAGE = new Parameter("minCoverage", "NUM", "0.5", "The minimum fractional coverage " +
//            "required for non-missing output.");
//
//    private static final Parameter PARAM_MAX_UNCERTAINTY = new Parameter("maxUncertainty", "NUM", "",
//            "The maximum relative total uncertainty allowed for non-missing output.", true);


    public static void main(String[] args) {
        new RegriddingTool().run(args);
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
        // PARAM_CLIMATOLOGY_DIR, PARAM_MIN_COVERAGE, PARAM_MAX_UNCERTAINTY, PARAM_TOTAL_UNCERTAINTY, PARAM_SST_DEPTH
        paramList.addAll(Arrays.asList(PARAM_SPATIAL_RESOLUTION, PARAM_START_DATE, PARAM_END_DATE,
                PARAM_REGION, PARAM_OUTPUT_DIR, PARAM_PRODUCT_TYPE));

        ProductType[] values = ProductType.values();
        for (ProductType value : values) {
            paramList.add(new Parameter(value.name() + ".dir", "DIR", null, "Directory that hosts the products of type '" + value.name() + "'."));
        }

        return paramList.toArray(new Parameter[paramList.size()]);
    }

    @Override
    protected void run(Configuration configuration, String[] arguments) throws ToolException {
        final ProductType productType = ProductType.valueOf(configuration.getString(PARAM_PRODUCT_TYPE, true));
        final String productDirectory = configuration.getString(productType + ".dir", null, true);
        final String targetResolution = configuration.getString(PARAM_SPATIAL_RESOLUTION, true);
        final Date to = configuration.getDate(PARAM_END_DATE, true);
        final Date from = configuration.getDate(PARAM_START_DATE, true);

        String filenameRegex = ".+";
        FileStore fileStore = FileStore.create(productType, filenameRegex, productDirectory);
        Regridder regridder = new Regridder(fileStore);

        try {
           regridder.doIt(from, to);
        } catch (IOException e) {
            throw new ToolException("Regridding failed: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }
    }
}
