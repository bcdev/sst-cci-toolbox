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

    public static final Parameter PARAM_SST_DEPTH = new Parameter("sstDepth", "DEPTH", SstDepth.skin.name(),
                                                                  "The SST depth. Must be one of " + Arrays.toString(
                                                                          SstDepth.values()) + ".");
    public static final Parameter PARAM_MIN_COVERAGE = new Parameter("minCoverage", "NUM", "0.5",
                                                                     "Minimum fractional coverage required to yield non-missing grid box output.");
    public static final Parameter PARAM_MAX_UNCERTAINTY = new Parameter("maxUncertainty", "NUM", "0.0",
                                                                        "Maximum relative total uncertainty allowed to yield non-missing output.");
    public static final Parameter PARAM_SPATIAL_RES = new Parameter("spatialRes", "NUM", "0.05",
                                                                    "Spatial resolution of the output grid in degrees. Must be one of " + Arrays.toString(
                                                                            SpatialResolution.getValueSet()) + ".");

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
        return "";
    }

    @Override
    protected String getHeader() {
        return "";
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
        return Arrays.asList(PARAM_SST_DEPTH, PARAM_MIN_COVERAGE, PARAM_MAX_UNCERTAINTY, PARAM_SPATIAL_RES).toArray(new Parameter[4]);
    }

    @Override
    protected void run(Configuration configuration, String[] arguments) throws ToolException {
    }
}
