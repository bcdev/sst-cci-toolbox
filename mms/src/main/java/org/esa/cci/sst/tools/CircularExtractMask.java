package org.esa.cci.sst.tools;

class CircularExtractMask {

    private final boolean[][] mask;

    CircularExtractMask(int width, int height, double radiusInKm, double pixelSizeInKm) {
        mask = new boolean[width][height];
        final double pixelRadius = radiusInKm / pixelSizeInKm;
        final double pixelRadiusSquared = pixelRadius * pixelRadius;

        final int centerX = width / 2;
        final int centerY = height / 2;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final int xDelta = x - centerX;
                final int yDelta = y - centerY;
                double distanceSquared = xDelta * xDelta + yDelta * yDelta;
                if (distanceSquared <= pixelRadiusSquared) {
                    mask[x][y] = true;
                }
            }
        }
    }

    public boolean getValue(int x, int y) {
        return mask[x][y];
    }
}
