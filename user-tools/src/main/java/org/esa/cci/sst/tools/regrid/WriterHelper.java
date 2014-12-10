package org.esa.cci.sst.tools.regrid;

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
