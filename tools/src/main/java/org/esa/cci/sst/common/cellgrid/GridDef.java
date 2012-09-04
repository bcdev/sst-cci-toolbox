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

package org.esa.cci.sst.common.cellgrid;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * The layout and geo-coding of a {@link CellGrid}.
 *
 * @author Norman Fomferra
 */
public class GridDef {
    //time dimension is currently 1, refer to sst-cci product spec
    private int time = 1;
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

    public static GridDef createGlobal(double resolution, int time) {
        GridDef gridDef = createGlobal(resolution);
        return gridDef.setTime(time);
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

    public int getTime() {
        return time;
    }

    public GridDef setTime(int time) {
        this.time = time;
        return this;
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

    public double getResolution() {
        if (resolutionX != resolutionY) {
            throw new IllegalStateException("To get a valid result, resolutionX and resolutionY must be equal. " +
                    "Get resolutionX with getResolutionX() and resolutionY with getResolutionY().");
        }
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

    /**
     * @param lonLatRectangle Spanned from upper left corner, unit is degree
     * @return The result-rectangle is spanned from upper left corner (as demanded by Java Doc of Rectangle).
     */
    public Rectangle getGridRectangle(Rectangle2D lonLatRectangle) {
        double lon1 = lonLatRectangle.getX();
        double lat1 = lonLatRectangle.getY();
        double lon2 = lon1 + lonLatRectangle.getWidth();
        double lat2 = lat1 + lonLatRectangle.getHeight();
        return getGridRectangle(lon1, lat1, lon2, lat2); //spanned from upper left corner (as demanded by Java Doc of Rectangle)
    }

    /**
     * @param lon1 Minimum lon coordinate (lower left corner)
     * @param lat1 Minimum lat coordinate (lower left corner)
     * @param lon2 Maximum lon coordinate (upper right corner)
     * @param lat2 Maximum lat coordinate (upper right corner)
     * @return The result-rectangle is spanned from the lower left corner (Different from Java Doc of @refer Rectangle ).
     */
    public Rectangle getGridRectangle(double lon1, double lat1, double lon2, double lat2) {
        double eps = 1.0e-10;
        int gridX1 = getGridX(lon1, true);
        int gridX2 = getGridX(lon2 - eps, true);
        int gridY1 = getGridY(lat2, true);
        int gridY2 = getGridY(lat1 + eps, true);
        return new Rectangle(gridX1, gridY1, gridX2 - gridX1 + 1, gridY2 - gridY1 + 1);  //spanned from the lower left corner
    }


    public int getAbsoluteNumberOfCells() {
        return width * height * time;
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
                : time != gridDef.time ? false
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
        result = 31 * result + time;
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
