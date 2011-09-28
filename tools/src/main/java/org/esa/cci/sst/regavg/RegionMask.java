package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.Grid;

import java.text.ParseException;
import java.util.StringTokenizer;

/**
 * A global mask comprising 72 x 35 5-degree cells.
 *
 * @author Norman
 */
public class RegionMask {
    private static final int WIDTH = 72;
    private static final int HEIGHT = 36;
    private static final Grid GRID = Grid.createGlobalGrid(WIDTH, HEIGHT);

    private final String name;
    private final boolean[] samples;

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
                for (int x = 0; x < 72; x++) {
                    char c = line.charAt(x);
                    if (c != '0' && c != '1') {
                        throw new ParseException(String.format("Region %s: Illegal mask format in line %d: Only use characters '0' and '1'.", name, lineNo), x);
                    }
                    samples[y * 72 + x] = c != '0';
                }
                y++;
            }
        }
        if (y != HEIGHT) {
            throw new ParseException(String.format("Region %s: Illegal mask format in line %d: Exactly %d lines are required, but found %d.", name, lineNo, HEIGHT, y), 0);
        }
        return new RegionMask(name, samples);
    }

    public static RegionMask create(String name, double westing, double northing, double easting, double southing) {
        if (northing < southing) {
            throw new IllegalArgumentException("northing must not be less than southing");
        }
        final double eps = 1e-10;
        int gridX1 = GRID.getGridX(westing, true);
        int gridX2 = GRID.getGridX(easting - eps, true);
        int gridY1 = GRID.getGridX(northing, true);
        int gridY2 = GRID.getGridX(southing + eps, true);
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
    }

    public String getName() {
        return name;
    }

    public boolean getSample(double lon, double lat) {
        int gridX = GRID.getGridX(lon, true);
        int gridY = GRID.getGridY(lat, true);
        return samples[gridY * WIDTH + gridX];
    }

}
