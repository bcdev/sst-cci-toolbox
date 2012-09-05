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

package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.util.NcUtils;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.Arrays;

/**
 * Not yet implemented.
 *
 * @author Norman Fomferra
 */
public class CciL3UFileType extends UnsupportedFileType {
    public final static CciL3UFileType INSTANCE = new CciL3UFileType();
    public final GridDef GRID_DEF = GridDef.createGlobalGrid(7200, 3600); //source per default on 0.05 ° resolution

    @Override
    public Grid[] readSourceGrids(NetcdfFile file, SstDepth sstDepth) throws IOException {
        Grid[] grids = new Grid[6];

        if (sstDepth == SstDepth.depth_20 || sstDepth == SstDepth.depth_100) {
            grids[0] = NcUtils.readGrid(file, "sea_surface_temperature_depth", getGridDef(), 0);
        } else /*if (sstDepth == SstDepth.skin)*/ {
            grids[0] = NcUtils.readGrid(file, "sea_surface_temperature", getGridDef(), 0);
        }
        grids[1] = NcUtils.readGrid(file, "quality_level", getGridDef(), 0);
        grids[2] = NcUtils.readGrid(file, "uncorrelated_uncertainty", getGridDef(), 0);
        grids[3] = NcUtils.readGrid(file, "large_scale_correlated_uncertainty", getGridDef(), 0);
        grids[4] = NcUtils.readGrid(file, "synoptically_correlated_uncertainty", getGridDef(), 0);
        try {
            NcUtils.getVariable(file, "adjustment_uncertainty");
        } catch (IOException e) {
            return Arrays.copyOf(grids, 5);
        }
        grids[5] = NcUtils.readGrid(file, "adjustment_uncertainty", getGridDef(), 0);

        return grids;
    }

    @Override
    public GridDef getGridDef() {
        return GRID_DEF;
    }
}
