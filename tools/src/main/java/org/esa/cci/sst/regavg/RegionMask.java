package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.Grid;

import java.awt.*;
import java.text.ParseException;
import java.util.StringTokenizer;

/**
 * A global mask comprising 72 x 35 5-degree cells.
 *
 * @author Norman
 */
public class RegionMask {

    public enum Coverage {
        Empty,
        Globe,
        N_Hemisphere,
        S_Hemisphere,
        // may add: Ninty_deg_cells?
        Other,
    }

    private static final int WIDTH = 72;
    private static final int HEIGHT = 36;
    private static final Grid GRID = Grid.createGlobalGrid(WIDTH, HEIGHT);

    private final String name;
    private final boolean[] samples;
    private final Coverage coverage;

    public static RegionMask create(String name, String data) throws ParseException {
        boolean[] samples = new boolean[WIDTH * HEIGHT];
        StringTokenizer stringTokenizer = new StringTokenizer(data, "\n");
        int lineNo = 0;
        int y = 0;
        while (stringTokenizer.hasMoreTokens()) {
            lineNo++;
            String line = stringTokenizer.nextToken().trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                if (line.length() != WIDTH) {
                    throw new ParseException(String.format("Region %s: Illegal mask format in line %d: Line must contain exactly %d characters, but found %d.", name, lineNo, WIDTH, line.length()), 0);
                }
                for (int x = 0; x < WIDTH; x++) {
                    char c = line.charAt(x);
                    if (c != '0' && c != '1') {
                        throw new ParseException(String.format("Region %s: Illegal mask format in line %d: Only use characters '0' and '1'.", name, lineNo), x);
                    }
                    if (c == '1') {
                        samples[y * WIDTH + x] = true;
                    }
                }
                y++;
            }
        }
        if (y != HEIGHT) {
            throw new ParseException(String.format("Region %s: Illegal mask format in line %d: Exactly %d lines are required, but found %d.", name, lineNo, HEIGHT, y), 0);
        }
        return new RegionMask(name, samples);
    }

    public static RegionMask create(String name, double west, double north, double east, double south) {
        if (north < south) {
            throw new IllegalArgumentException("north < south");
        }
        Rectangle gridRectangle = GRID.getGridRectangle(west, south, east, north);
        int gridX1 = gridRectangle.x;
        int gridY1 = gridRectangle.y;
        int gridX2 = gridRectangle.x + gridRectangle.width - 1;
        int gridY2 = gridRectangle.y + gridRectangle.height - 1;
        boolean[] samples = new boolean[WIDTH * HEIGHT];
        for (int y = gridY1; y <= gridY2; y++) {
            if (gridX1 <= gridX2) {
                // westing-->easting is within -180...180
                for (int x = gridX1; x <= gridX2; x++) {
                    samples[y * WIDTH + x] = true;
                }
            } else {
                // westing-->easting intersects with anti-meridian
                for (int x = gridX1; x <= WIDTH - 1; x++) {
                    samples[y * WIDTH + x] = true;
                }
                for (int x = 0; x <= gridX2; x++) {
                    samples[y * WIDTH + x] = true;
                }
            }
        }
        return new RegionMask(name, samples);
    }

    public RegionMask(String name, boolean[] samples) {
        this.name = name;
        this.samples = samples;

        int nG = 0;
        int nN = 0;
        int nS = 0;
        for (int i = 0; i < samples.length; i++) {
            boolean sample = samples[i];
            if (sample) {
                nG++;
                if (i < samples.length / 2) {
                    nN++;
                } else {
                    nS++;
                }
            }
        }

        if (nG == 0) {
            coverage = Coverage.Empty;
        } else if (nG == samples.length) {
            coverage = Coverage.Globe;
        } else if (nN == samples.length / 2) {
            coverage = Coverage.N_Hemisphere;
        } else if (nS == samples.length / 2) {
            coverage = Coverage.S_Hemisphere;
        } else {
            coverage = Coverage.Other;
        }
    }

    public String getName() {
        return name;
    }

    public Coverage getCoverage() {
        return coverage;
    }

    public boolean[] getSamples() {
        return samples.clone();
    }

    public int getWidth() {
        return WIDTH;
    }

    public int getHeight() {
        return HEIGHT;
    }

    public Grid getGrid() {
        return GRID;
    }

    public boolean getSampleForPos(double lon, double lat) {
        int gridX = GRID.getGridX(lon, true);
        int gridY = GRID.getGridY(lat, true);
        return samples[gridY * WIDTH + gridX];
    }

    public boolean getSampleForCell(int  gridX, int gridY) {
        return samples[gridY * WIDTH + gridX];
    }

    public static RegionMask or(RegionMaskList regionMaskList) {
        if (regionMaskList.size() == 0) {
            return null;
        }
        if (regionMaskList.size() == 1) {
            return regionMaskList.get(0);
        }
        boolean[] samples = new boolean[WIDTH * HEIGHT];
        for (RegionMask regionMask : regionMaskList) {
            for (int i = 0; i < samples.length; i++) {
                if (regionMask.samples[i]) {
                    samples[i] = true;
                }
            }
        }
        return new RegionMask("Combined", samples);
    }
}
