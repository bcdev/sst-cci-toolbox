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

package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.cellgrid.ArrayGrid;
import org.esa.cci.sst.common.cellgrid.Downscaling;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.Interpolation;
import org.esa.cci.sst.common.cellgrid.YFlip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Lookup table (LUT) for X0 quantities (spatial distance or time) used for calculating
 * coverage uncertainty.
 * <p/>
 * @author Ralf Quast
 * @author Bettina Scholze
 */
public final class LutX0 {

    private static final GridDef GRID_DEF_005 = GridDef.createGlobal(0.05);
    private static final GridDef GRID_DEF_020 = GridDef.createGlobal(2.00);

    private final Grid grid;

    private LutX0(Grid grid) {
        this.grid = grid;
    }

    /**
     * Creates a new lookup table.
     *
     * @param file             The path to the LUT file (spatial resolution of 2.0 degrees).
     * @param fillValue        The fill value used in the LUT file.
     * @param targetResolution The spatial resolution of the target grid.
     *
     * @return the lookup table.
     *
     * @throws IOException when an error occurred.
     */
    public static LutX0 create(File file, double fillValue, SpatialResolution targetResolution) throws IOException {
        final Logger logger = Logger.getLogger("org.esa.cci.sst");

        final long t0 = System.currentTimeMillis();
        logger.info(String.format("Creating LUT for file '%s'", file.getPath()));

        Grid lutGrid;
        // 1. read in 2.00 degrees resolution
        lutGrid = readGrid(file, fillValue);
        // 2. interpolate to 0.05 degrees
        lutGrid = Interpolation.create(lutGrid, GRID_DEF_005);
        // 3. downscale to target resolution
        if (!SpatialResolution.DEGREE_0_05.equals(targetResolution)) {
            final int scale = (int) Math.round(targetResolution.getResolution() / 0.05);
            lutGrid = Downscaling.create(lutGrid, scale);
        }
        // 4. flip
        lutGrid = YFlip.create(lutGrid);

        final long t1 = System.currentTimeMillis();
        logger.info(String.format("Ready creating LUT in%d ms", t1 - t0));

        return new LutX0(lutGrid);
    }

    /**
     * Returns the value of the X0 quantity (either spatial distance or time) in target resolution.
     *
     * @param gridX The x coordinate.
     * @param gridY The y coordinate.
     *
     * @return the value of the X0 quantity.
     */
    public double getXValue(int gridX, int gridY) {
        return getGrid().getSampleDouble(gridX, gridY);
    }

    /**
     * Returns the LUT grid.
     *
     * @return the LUT grid.
     */
    public Grid getGrid() {
        return grid;
    }

    static ArrayGrid readGrid(File file, double fillValue) throws IOException {
        final int w = GRID_DEF_020.getWidth();
        final int h = GRID_DEF_020.getHeight();
        final double[] data = new double[w * h];

        final int columnWidth = 9;
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            for (int y = 0; y < h; y++) {
                final String line = reader.readLine();

                for (int x = 0; x < w; x++) {
                    final String column = line.substring(x * columnWidth, x * columnWidth + columnWidth);
                    double value = Double.parseDouble(column);
                    if (value == fillValue) {
                        value = Double.NaN;
                    }
                    data[w * y + x] = value;
                }
            }
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                //ignore
            }
        }

        return ArrayGrid.createWith2DDoubleArray(GRID_DEF_020, data);
    }
}
