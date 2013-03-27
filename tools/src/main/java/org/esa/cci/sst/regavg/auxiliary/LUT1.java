/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.regavg.auxiliary;

import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.XSwap;
import org.esa.cci.sst.common.cellgrid.YFlip;
import org.esa.cci.sst.util.NcUtils;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;

/**
 * Represents LUT1.
 *
 * Enables calculation of coverage/sampling uncertainty for an average via the number of values comprising that average.
 * LUT1 contains values of two parameters (magnitude and exponent) for each 5° monthly grid box.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public class LUT1 {

    private final Grid magnitudeGrid;
    private final Grid exponentGrid;

    public static LUT1 read(File file) throws IOException {
        final NetcdfFile netcdfFile = NetcdfFile.open("file:" + file.getPath().replace("\\", "/"));
        try {
            final GridDef gridDef = GridDef.createGlobal(5.0);
            final Grid magnitudeGrid = NcUtils.readGrid(netcdfFile, "MAGNITUDE", gridDef);
            final Grid exponentGrid = NcUtils.readGrid(netcdfFile, "EXPONENT", gridDef);
            return new LUT1(XSwap.create(YFlip.create(magnitudeGrid)), XSwap.create(YFlip.create(exponentGrid)));
        } finally {
            netcdfFile.close();
        }
    }

    private LUT1(Grid magnitudeGrid, Grid exponentGrid) {
        this.magnitudeGrid = magnitudeGrid;
        this.exponentGrid = exponentGrid;
    }

    public Grid getMagnitudeGrid5() {
        return magnitudeGrid;
    }

    public Grid getExponentGrid5() {
        return exponentGrid;
    }
}
