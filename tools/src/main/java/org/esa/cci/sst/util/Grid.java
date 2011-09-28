package org.esa.cci.sst.util;

/**
 * Represents a lat/lon grid.
 *
 * @author Norman Fomferra
 */
public class Grid {
    private final double northing;
    private final double easting;
    private final double resolution;

    public static Grid createGlobalGrid(double resolution) {
        return new Grid(-180.0, 90.0, resolution);
    }

    public Grid(double easting, double northing, double resolution) {
        this.easting = easting;
        this.northing = northing;
        this.resolution = resolution;
    }

    public final double getCenterLon(int x) {
        return easting + resolution * (x + 0.5);
    }

    public final double getCenterLat(int y) {
        return northing - resolution * (y + 0.5);
    }

    public final int getGridX(double lon) {
        return (int) ((lon - easting) / resolution);
    }

    public final int getGridY(double lat) {
        return (int) ((lat - northing) / -resolution);
    }

}
