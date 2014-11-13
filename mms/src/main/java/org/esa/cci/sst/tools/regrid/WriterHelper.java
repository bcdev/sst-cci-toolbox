package org.esa.cci.sst.tools.regrid;/*
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

import org.esa.cci.sst.grid.GridDef;

class WriterHelper {

    private WriterHelper() {
    }

    static float[] createLatData(GridDef gridDef) {
        final float[] latData = new float[gridDef.getHeight()];

        for (int y = 0; y < gridDef.getHeight(); y++) {
            latData[y] = (float) gridDef.getCenterLat(y);
        }
        return latData;
    }

    static float[] createLonData(GridDef gridDef) {
        final float[] lonData = new float[gridDef.getWidth()];

        for (int x = 0; x < gridDef.getWidth(); x++) {
            lonData[x] = (float) gridDef.getCenterLon(x);
        }
        return lonData;
    }

    static float[][] createLatBoundsData(GridDef gridDef) {
        final float[][] data = new float[gridDef.getHeight()][2];

        for (int y = 0; y < gridDef.getHeight(); y++) {
            data[y][1] = (float) (gridDef.getCenterLat(y) + gridDef.getResolution() * 0.5);
            data[y][0] = (float) (gridDef.getCenterLat(y) - gridDef.getResolution() * 0.5);
        }
        return data;
    }

    static float[][] createLonBoundsData(GridDef gridDef) {
        final float[][] data = new float[gridDef.getWidth()][2];

        for (int x = 0; x < gridDef.getWidth(); x++) {
            data[x][0] = (float) (gridDef.getCenterLon(x) - gridDef.getResolution() * 0.5);
            data[x][1] = (float) (gridDef.getCenterLon(x) + gridDef.getResolution() * 0.5);
        }
        return data;
    }
}
