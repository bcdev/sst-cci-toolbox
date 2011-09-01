/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.ToolException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Re-writes given variables to given MMD file.
 *
 * @author Thomas Storm
 */
public class MmdUpdater extends BasicTool {

    final List<Variable> variables = new ArrayList<Variable>(5);
    NetcdfFileWriteable mmd;

    protected MmdUpdater() {
        super("mmdupdate.sh", "0.1");
    }

    public static void main(String[] args) {
        final MmdUpdater mmdUpdater = new MmdUpdater();
        final boolean performWork = mmdUpdater.setCommandLineArgs(args);
        if (!performWork) {
            return;
        }
        mmdUpdater.initialize();
        mmdUpdater.run(args);
        mmdUpdater.close();
    }

    @Override
    public void initialize() {
        super.initialize();
        openMmd();
        parseVariables();
    }

    private void run(String[] args) {
        final MmdTool mmdTool = new MmdTool();
        mmdTool.setCommandLineArgs(args);
        mmdTool.initialize();
        mmdTool.writeMmdShuffled(mmd, variables);
    }

    private void close() {
        try {
            mmd.close();
        } catch (IOException e) {
            getLogger().warning("File '" + mmd.getLocation() + "' could not be closed.");
        }
    }

    void openMmd() {
        final String mmdLocation = getConfiguration().getProperty("mms.mmdupdate.mmd");
        try {
            final boolean canOpen = NetcdfFileWriteable.canOpen(mmdLocation);
            if(!canOpen) {
                throw new Exception("Cannot open file '" + mmdLocation + "'.");
            }
            mmd = NetcdfFileWriteable.openExisting(mmdLocation);
        } catch (Exception e) {
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    void parseVariables() {
        final String updateVariables = getConfiguration().getProperty("mms.mmdupdate.variables");
        for (String updateVariable : updateVariables.split(",")) {
            final Variable variable = mmd.findVariable(NetcdfFile.escapeName(updateVariable));
            if(variable == null) {
                getLogger().warning("Variable '" + updateVariable + "' not found in mmd file.");
            }
            variables.add(variable);
        }
    }
}
