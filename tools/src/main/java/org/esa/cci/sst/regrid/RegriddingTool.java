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

import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.Parameter;
import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.tool.ToolException;

import java.util.Arrays;

public class RegriddingTool extends Tool {

    public static final Parameter PARAM_SST_DEPTH =
            new Parameter("sstDepth", "DEPTH", SstDepth.skin.name(),
                          "The SST depth. Must be one of " + Arrays.toString(
                                  SstDepth.values()) + ".");
    public static final Parameter PARAM_MIN_COVERAGE =
            new Parameter("minCoverage", "NUM", "0.5",
                          "The minimum fractional coverage required for non-missing output.");
    public static final Parameter PARAM_MAX_UNCERTAINTY =
            new Parameter("maxUncertainty", "NUM", "",
                          "The maximum relative total uncertainty allowed for non-missing output.", true);
    public static final Parameter PARAM_SPATIAL_RES =
            new Parameter("spatialRes", "NUM", "0.05",
                          "The spatial resolution of the output grid in degrees. Must be one of " + Arrays.toString(
                                  SpatialResolution.getValueSet()) + ".");
    public static final Parameter PARAM_REGION =
            new Parameter("region", "REGION", "-180,90,180,-90",
                          "The sub-region to be used (optional). Must be a list of coordinates in the format W,N,E,S.");
    public static final Parameter PARAM_TOTAL_UNCERTAINTY =
            new Parameter("totalUncertainty", "BOOL", "false",
                          "A Boolean variable indicating whether total or separated uncertainties are written to the " +
                          "output file. Must be either 'true' or 'false'.");
    public static final Parameter PARAM_CLIMATOLOGY_DIR =
            new Parameter("climatologyDir", "DIR", "./climatology",
                          "The directory path to the reference climatology.");

    @Override
    protected String getName() {
        return "regrid";
    }

    @Override
    protected String getVersion() {
        return "0.1";
    }

    @Override
    protected String getSyntax() {
        return getName() + " [OPTIONS]";
    }

    @Override
    protected String getHeader() {
        return "The " + getName() + " tool is used to read in the SST CCI L3U, L3C, and L4 products at daily 0.05° " +
               "latitude by longitude resolution and output on other spatio-temporal resolutions, which are a multiple" +
               "of this and divide neatly into 180 degrees. Output are SSTs and their uncertainties.";
    }

    @Override
    protected String getFooter() {
        return "";
    }

    @Override
    protected String getToolHome() {
        return System.getProperty(getName() + ".home", ".");
    }

    @Override
    protected Parameter[] getParameters() {
        return Arrays.asList(PARAM_SST_DEPTH, PARAM_MIN_COVERAGE, PARAM_MAX_UNCERTAINTY, PARAM_SPATIAL_RES,
                             PARAM_REGION, PARAM_TOTAL_UNCERTAINTY, PARAM_CLIMATOLOGY_DIR).toArray(
                new Parameter[7]);
    }

    @Override
    protected void run(Configuration configuration, String[] arguments) throws ToolException {
    }
}
