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

package org.esa.cci.sst.regavg;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

/**
 * Represents LUT2.
 * Enables calculation of coverage/sampling uncertainty for an average via the number of values comprising that average.
 * LUT2 contains an uncertainty value for each 90° monthly grid box and calendar month; these represent the sampling
 * uncertainty that would result if the average were created from “averaging” only one 5° monthly value.  These values are later divided by the square root of the number of constituent 5° monthly values  to calculate the sampling uncertainty in a 90° average.
 *
 * @author Norman Fomferra
 */
public class LUT2 {

    final double[][] weights;

    public static LUT2 read(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        try {
            LineNumberReader reader = new LineNumberReader(fileReader);
            reader.readLine();
            reader.readLine();
            reader.readLine();
            reader.readLine();
            double[][] weights = new double[12][8];
            for (int month = 0; month < 12; month++) {
                reader.readLine();
                parseLine(reader.readLine(), month, weights, 0);
                parseLine(reader.readLine(), month, weights, 4);
            }
            return new LUT2(weights);
        } finally {
            fileReader.close();
        }
    }

    private static void parseLine(String line, int month, double[][] weights, int off) {
        String[] split = line.trim().split("[ \\t]+");
        for (int j = 0; j < split.length; j++) {
            weights[month][off + j] = Double.parseDouble(split[j]);
        }
    }

    private LUT2(double[][] weights) {
        this.weights = weights;
    }

    public double getMagnitude90(int month, int x, int y) {
        return weights[month][y * 4 + x];
    }
}
