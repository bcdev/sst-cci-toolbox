package org.esa.cci.sst.regavg;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A list of regions.
 */
public class RegionList extends ArrayList<RegionMask> {

    public static RegionList parse(String value) throws ParseException, IOException {
        StringTokenizer stringTokenizer = new StringTokenizer(value, ";");
        RegionList regionList = new RegionList();

        while (stringTokenizer.hasMoreTokens()) {
            String entry = stringTokenizer.nextToken().trim();
            int entryNo = regionList.size() + 1;
            int pos = entry.indexOf('=');
            if (pos == -1) {
                throw new ParseException(String.format("Illegal region entry %d: is missing the '=' character.", entryNo), 0);
            }
            String name = entry.substring(0, pos).trim();
            String mask = entry.substring(pos + 1).trim();
            if (name.isEmpty()) {
                throw new ParseException(String.format("Illegal region entry %d: Name is empty.", entryNo), 0);
            }
            if (mask.isEmpty()) {
                throw new ParseException(String.format("Illegal region entry %d: Mask is empty.", entryNo), 0);
            }
            String[] splits = mask.split(",");
            if (splits.length == 4) {
                fromWNES(name, splits, regionList);
            } else if (splits.length == 1) {
                fromMaskFile(name, splits[0], regionList);
            } else {
                throw new ParseException(String.format("Illegal region entry %d: Mask is empty.", entryNo), 0);
            }
        }
        return regionList;
    }

    private static void fromWNES(String name, String[] wnes, List<RegionMask> regionMasks) throws ParseException {
        int entryNo = regionMasks.size() + 1;
        double west;
        double north;
        double east;
        double south;
        try {
            west = Double.parseDouble(wnes[0]);
            north = Double.parseDouble(wnes[1]);
            east = Double.parseDouble(wnes[2]);
            south = Double.parseDouble(wnes[3]);
        } catch (NumberFormatException e) {
            throw new ParseException(String.format("Illegal region entry %d: Failed to parse W,N,E,S coordinates.", entryNo), 0);
        }
        if (north < south) {
            throw new ParseException(String.format("Illegal region entry %d: N must not be less than S.", entryNo), 0);
        }
        regionMasks.add(RegionMask.create(name, west, north, east, south));
    }

    private static void fromMaskFile(String name, String split,List<RegionMask> regionMasks) throws IOException, ParseException {
        int entryNo = regionMasks.size() + 1;
        File file = new File(split);
        if (!file.exists()) {
            throw new IOException(String.format("Illegal region entry %d: Mask file not found: %s", entryNo, file));
        }
        char[] data = new char[(int) file.length()];
        FileReader reader = new FileReader(file);
        try {
            int read = reader.read(data);
            if (read != data.length) {
                throw new IOException(String.format("Illegal region entry %d: Failed to read mask file: %s", entryNo, file));
            }
        } finally {
            reader.close();
        }
        regionMasks.add(RegionMask.create(name, new String(data)));
    }
}
