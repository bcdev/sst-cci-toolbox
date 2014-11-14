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

package org.esa.cci.sst.tools.regrid;

import org.esa.cci.sst.grid.*;
import org.esa.cci.sst.common.SpatialResolution;
import org.esa.cci.sst.log.SstLogging;
import org.esa.cci.sst.util.StopWatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Lookup table for 'X0' quantities (LUT2 and LUT3) used for calculating
 * coverage uncertainties.
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
final class RegriddingLUT2 implements LUT {

    private static final GridDef GRID_DEF_005 = GridDef.createGlobal(0.05);
    private static final GridDef GRID_DEF_020 = GridDef.createGlobal(2.00);

    private final Grid grid;

    private RegriddingLUT2(Grid grid) {
        this.grid = grid;
    }

    /**
     * Creates a new lookup table.
     *
     * @param file             The path to the LUT file (spatial resolution of 2.0 degrees).
     * @param targetResolution The spatial resolution of the target grid.
     * @param fillValue        The fill value used in the LUT file.
     *
     * @return the lookup table.
     *
     * @throws IOException when an error occurred.
     */
    static RegriddingLUT2 create(File file, SpatialResolution targetResolution, double fillValue) throws IOException {
        final Logger logger = SstLogging.getLogger();

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

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

        stopWatch.stop();
        logger.info(String.format("Ready creating LUT in %d ms", stopWatch.getElapsedMillis()));

        return new RegriddingLUT2(lutGrid);
    }

    /**
     * Returns the LUT grid.
     *
     * @return the LUT grid.
     */
    @Override
    public Grid getGrid() {
        return grid;
    }

    static Grid readGrid(File file, double fillValue) throws IOException {
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

        return ArrayGrid.create(GRID_DEF_020, data);
    }
}
