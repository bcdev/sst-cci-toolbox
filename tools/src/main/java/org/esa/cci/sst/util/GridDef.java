package org.esa.cci.sst.util;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * The layout and geo-coding of a {@link CellGrid}.
 *
 * @author Norman Fomferra
 */
public class GridDef {
    private final int width;
    private final int height;
    private final double northing;
    private final double easting;
    private final double resolutionX;
    private final double resolutionY;

    public static GridDef createGlobal(double resolution) {
        int width = (int) (360.0 / resolution);
        int height = (int) (180.0 / resolution);
        return new GridDef(width, height, -180.0, 90.0, resolution, resolution);
    }

    public static GridDef createGlobalGrid(int width, int height) {
        return new GridDef(width, height, -180.0, 90.0, 360.0 / width, 180.0 / height);
    }

    public GridDef(int width, int height, double easting, double northing, double resolutionX, double resolutionY) {
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

    public double getEasting() {
        return easting;
    }

    public double getNorthing() {
        return northing;
    }

    public double getResolutionX() {
        return resolutionX;
    }

    public double getResolutionY() {
        return resolutionY;
    }

    public final double getCenterLon(int gridX) {
        return getLon(gridX + 0.5);
    }

    public final double getCenterLat(int gridY) {
        return getLat(gridY + 0.5);
    }

    public final double getLon(double gridX) {
        double lon = easting + resolutionX * gridX;
        if (lon < -180.0) {
            lon = 180.0 - (-lon % 180.0);
        }
        if (lon > 180.0) {
            lon = -180.0 + (lon % 180.0);
        }
        return lon;
    }

    public final double getLat(double gridY) {
        double lat = northing - resolutionY * gridY;
        if (lat < -90.0) {
            return -90.0;
        }
        if (lat > 90.0) {
            return 90.0;
        }
        return lat;
    }

    public final int getGridX(double lon, boolean crop) {
        if (lon < -180.0) {
            throw new IllegalArgumentException("lon < -180.0");
        }
        if (lon > 180.0) {
            throw new IllegalArgumentException("lon > 180.0");
        }
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
        if (lat < -90.0) {
            throw new IllegalArgumentException("lat < -90.0");
        }
        if (lat > 90.0) {
            throw new IllegalArgumentException("lat > 90.0");
        }
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

    public Rectangle2D getLonLatRectangle(int gridX, int gridY) {
        double lon1 = getLon(gridX);
        double lat1 = getLat(gridY + 1);
        return new Rectangle2D.Double(lon1, lat1, resolutionX, resolutionY);
    }

    public Rectangle getGridRectangle(Rectangle2D lonLatRectangle) {
        double lon1 = lonLatRectangle.getX();
        double lat1 = lonLatRectangle.getY();
        double lon2 = lon1 + lonLatRectangle.getWidth();
        double lat2 = lat1 + lonLatRectangle.getHeight();
        return getGridRectangle(lon1, lat1, lon2, lat2);
    }

    public Rectangle getGridRectangle(double lon1, double lat1, double lon2, double lat2) {
        double eps = 1.0e-10;
        int gridX1 = getGridX(lon1, true);
        int gridX2 = getGridX(lon2 - eps, true);
        int gridY1 = getGridY(lat2, true);
        int gridY2 = getGridY(lat1 + eps, true);
        return new Rectangle(gridX1, gridY1, gridX2 - gridX1 + 1, gridY2 - gridY1 + 1);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GridDef gridDef = (GridDef) o;

        return width != gridDef.width ? false
                : height != gridDef.height ? false
                : Double.compare(gridDef.easting, easting) != 0 ? false
                : Double.compare(gridDef.northing, northing) != 0 ? false
                : Double.compare(gridDef.resolutionX, resolutionX) != 0 ? false
                : Double.compare(gridDef.resolutionY, resolutionY) != 0 ? false
                : true;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = width;
        result = 31 * result + height;
        temp = northing != 0.0 ? Double.doubleToLongBits(northing) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = easting != 0.0 ? Double.doubleToLongBits(easting) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = resolutionX != 0.0 ? Double.doubleToLongBits(resolutionX) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = resolutionY != 0.0 ? Double.doubleToLongBits(resolutionY) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
