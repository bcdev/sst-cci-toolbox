package org.esa.cci.sst.util;

/**
 * Represents a lat/lon grid.
 *
 * @author Norman Fomferra
 */
public class Grid {
    private final int width;
    private final int height;
    private final double northing;
    private final double easting;
    private final double resolutionX;
    private final double resolutionY;

    public static Grid createGlobalGrid(double resolution) {
        int width = (int) (360.0 / resolution);
        int height = (int) (180.0 / resolution);
        return new Grid(width, height, -180.0, 90.0, resolution, resolution);
    }

    public static Grid createGlobalGrid(int width, int height) {
        return new Grid(width, height, -180.0, 90.0, 360.0 / width, 180.0 / height);
    }

    private Grid(int width, int height, double northing, double easting, double resolutionX, double resolutionY) {
        this.width = width;
        this.height = height;
        this.northing = northing;
        this.easting = easting;
        this.resolutionX = resolutionX;
        this.resolutionY = resolutionY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public final double getCenterLon(int x) {
        return easting + resolutionX * (x + 0.5);
    }

    public final double getCenterLat(int y) {
        return northing - resolutionY * (y + 0.5);
    }

    public final int getGridX(double lon, boolean crop) {
        int gridX = (int) ((lon - easting) / resolutionX);
        if (crop) {
            if (gridX < 0) {
                return 0;
            }
            if (gridX >= width) {
                return width - 1;
            }
        }
        return gridX;
    }

    public final int getGridY(double lat, boolean crop) {
        int gridY = (int) ((lat - northing) / -resolutionY);
        if (crop) {
            if (gridY < 0) {
                return 0;
            }
            if (gridY >= height) {
                return height - 1;
            }
        }
        return gridY;
    }

}
