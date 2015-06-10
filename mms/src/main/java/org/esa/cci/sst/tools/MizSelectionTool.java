/*
 * Copyright (c) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools;

import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;

import java.io.IOException;

/**
 * Selects matchup records located in the marginal ice zone (MIZ)
 *
 * @author Ralf Quast
 */
public class MizSelectionTool extends BasicTool {
    private String sourceMmdLocation;
    private String targetMmdLocation;

    protected MizSelectionTool() {
        super("miz-selection-tool", "1.0");
    }

    public static void main(String[] args) {
        final MizSelectionTool tool = new MizSelectionTool();
        try {
            if (!tool.setCommandLineArgs(args)) {
                tool.printHelp();
                return;
            }
            tool.initialize();
            tool.run();
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Exception e) {
            tool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.UNKNOWN_ERROR));
        }
    }

    @Override
    public void initialize() {
        // no database functions needed, therefore don't call
        // super.initialize();

        final Configuration config = getConfig();

        sourceMmdLocation = config.getStringValue(Configuration.KEY_MMS_SELECTION_MMD_SOURCE);
        targetMmdLocation = config.getStringValue(Configuration.KEY_MMS_SELECTION_MMD_TARGET);
    }

    private void run() throws IOException {
        /*
        TODO - implement what is defined below: include only those sub-scenes that satisfy both 1) and 2)

        So Ralf, our definition of the marginal ice zone is:

        1) No pixel within the central 101 pixel square exhibits a sea ice fraction greater than zero
        2) Any pixel within the 141 pixel square (but not within the central 101 pixel square) exhibits a sea ice fraction greater than 15%

        Regards,

        Kevin
         */
        throw new ToolException("MIZ selection not implemented.", ToolException.TOOL_ERROR);
    }

}
