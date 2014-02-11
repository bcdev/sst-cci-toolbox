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

package org.esa.cci.sst.tools.nwp;

import java.io.IOException;

/**
 * Main class for NWP generation.
 *
 * @author Thomas Storm
 */
public class Nwp {

    @SuppressWarnings({"ConstantConditions"})
    public static void main(String[] args) throws IOException, InterruptedException {
        final NwpTool nwpTool = new NwpTool(args);
        final boolean forMatchupPoints = nwpTool.extractNwpForMatchupPoints();
        if (forMatchupPoints) {
            nwpTool.createMatchupAnFile();
            nwpTool.createMatchupFcFile();
        } else {
            nwpTool.createMergedFile();
        }
    }

    private Nwp() {
    }

}
