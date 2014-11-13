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

package org.esa.cci.sst.common;

import java.awt.*;
import java.text.ParseException;
import java.util.StringTokenizer;

/**
 * A global mask comprising 72 x 36 5º cells.
 *
 * @author Norman
 * @author Ralf Quast
 */
public class RegionMask implements Grid {

    private final String name;
    private final boolean[][] samples;
    private final Coverage coverage;

    private static int width = 72;
    private static int height = 36;
    private static GridDef gridDef = GridDef.createGlobal(width, height);

    public static void setSpatialResolution(SpatialResolution spatialResolution) {
        final GridDef gridDef = spatialResolution.getGridDef();
        width = gridDef.getWidth();
        height = gridDef.getHeight();
        RegionMask.gridDef = gridDef;
    }

    public static RegionMask create(String name, String data) throws ParseException {
        final boolean[][] samples = new boolean[height][width];
        final StringTokenizer stringTokenizer = new StringTokenizer(data, "\n");

        int lineNo = 0;
        int y = 0;
        while (stringTokenizer.hasMoreTokens()) {
            lineNo++;
            String line = stringTokenizer.nextToken().trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                if (line.length() != width) {
                    throw new ParseException(String.format("Region %s: Illegal mask format in line %d: Line must contain exactly %d characters, but found %d.", name, lineNo, width, line.length()), 0);
                }
                for (int x = 0; x < width; x++) {
                    char c = line.charAt(x);
                    if (c != '0' && c != '1') {
                        throw new ParseException(String.format("Region %s: Illegal mask format in line %d: Only use characters '0' and '1'.", name, lineNo), x);
                    }
                    if (c == '1') {
                        samples[y][x] = true;
                    }
                }
                y++;
            }
        }
        if (y != height) {
            throw new ParseException(String.format("Region %s: Illegal mask format in line %d: Exactly %d lines are required, but found %d.", name, lineNo, height, y), 0);
        }

        return new RegionMask(name, samples);
    }

    public static RegionMask create(String name, double west, double north, double east, double south) {
        if (north < south) {
            throw new IllegalArgumentException("north < south");
        }
        final Rectangle gridRectangle = gridDef.getGridRectangle(west, south, east, north);
        final int minX = gridRectangle.x;
        final int minY = gridRectangle.y;
        final int maxX = gridRectangle.x + gridRectangle.width - 1;
        final int maxY = gridRectangle.y + gridRectangle.height - 1;
        boolean[][] samples = new boolean[height][width];

        for (int y = minY; y <= maxY; y++) {
            if (minX <= maxX) {
                // westing-->easting is within -180...180
                for (int x = minX; x <= maxX; x++) {
                    samples[y][x] = true;
                }
            } else {
                // westing-->easting intersects with anti-meridian
                for (int x = minX; x <= width - 1; x++) {
                    samples[y][x] = true;
                }
                for (int x = 0; x <= maxX; x++) {
                    samples[y][x] = true;
                }
            }
        }

        return new RegionMask(name, samples);
    }

    private RegionMask(String name, boolean[][] samples) {
        this.name = name;
        this.samples = samples;

        int nG = 0;
        int nN = 0;
        int nS = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                boolean sample = samples[j][i];
                if (sample) {
                    nG++;
                    if (j < height / 2) {
                        nN++;
                    } else {
                        nS++;
                    }
                }
            }
        }

        int nTotal = width * height;
        if (nG == 0) {
            coverage = Coverage.EMPTY;
        } else if (nG == nTotal) {
            coverage = Coverage.GLOBE;
        } else if (nN == nG && nN == nTotal / 2) {
            coverage = Coverage.N_HEMISPHERE;
        } else if (nS == nG && nS == nTotal / 2) {
            coverage = Coverage.S_HEMISPHERE;
        } else {
            coverage = Coverage.OTHER;
        }
    }

    public String getName() {
        return name;
    }

    public Coverage getCoverage() {
        return coverage;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public GridDef getGridDef() {
        return gridDef;
    }

    @Override
    public boolean getSampleBoolean(int gridX, int gridY) {
        return samples[gridY][gridX];
    }

    @Override
    public int getSampleInt(int gridX, int gridY) {
        return samples[gridY][gridX] ? 1 : 0;
    }

    @Override
    public double getSampleDouble(int gridX, int gridY) {
        return samples[gridY][gridX] ? 1.0 : 0.0;
    }

    public static RegionMask combine(RegionMaskList regionMaskList) {
        if (regionMaskList == null || regionMaskList.size() == 0) {
            return null;
        }
        if (regionMaskList.size() == 1) {
            return regionMaskList.get(0);
        }
        boolean[][] samples = new boolean[height][width];
        for (RegionMask regionMask : regionMaskList) {
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    if (regionMask.samples[j][i]) {
                        samples[j][i] = true;
                    }
                }
            }
        }
        return new RegionMask("Combined", samples);
    }
}
