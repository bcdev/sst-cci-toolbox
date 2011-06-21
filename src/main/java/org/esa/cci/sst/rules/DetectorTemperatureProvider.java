/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.rules;

import org.esa.beam.util.io.CsvReader;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TimeUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Provides the detector temperature for a given date.
 *
 * @author Thomas Storm
 */
class DetectorTemperatureProvider {

    public static final float FILL_VALUE = Float.MIN_VALUE;

    float[] temperatures;
    int startTime;
    int step;

    DetectorTemperatureProvider() {
        final File detectorTemperatureFile = new File(getClass().getResource("detector_temperature.dat").getFile());
        init(detectorTemperatureFile);
    }

    DetectorTemperatureProvider(File detectorTemperatureFile) {
        init(detectorTemperatureFile);
    }

    /**
     * Returns the detector temperature for the given date.
     * @param date The date to get the detector temperature for.
     * @return The detector temperature.
     */
    float getDetectorTemperature(Date date) {
        final double millisSince1970 = date.getTime();
        final double millisSince1978 = millisSince1970 - TimeUtil.MILLIS_1978;
        final double secondsSinceCciEpoch = millisSince1978 / 1000;
        int index = (int) Math.round((secondsSinceCciEpoch - startTime) / step);
        if(index >= temperatures.length) {
            return FILL_VALUE;
        }
        return temperatures[index];
    }

    private void init(File detectorTemperatureFile) {
        try {
            readMetaInfo(detectorTemperatureFile);
            readTemperatures(detectorTemperatureFile);
        } catch (IOException e) {
            throw new ToolException("Unable to read from detector temperature file.", e, ToolException.TOOL_IO_ERROR);
        }
    }

    private void readTemperatures(File detectorTemperatureFile) throws IOException {
        final char[] separators = {' ', '\n'};
        final CsvReader csvReader = new CsvReader(new FileReader(detectorTemperatureFile), separators, true, "#");
        try {
            final List<double[]> lines = csvReader.readDoubleRecords();
            int index = 0;
            for (double[] values : lines) {
                for (double value : values) {
                    temperatures[index] = (float) value;
                    index++;
                }
            }
        } finally {
            csvReader.close();
        }
    }

    private void readMetaInfo(File detectorTemperatureFile) throws IOException {
        int startTime = getMetaInfo(detectorTemperatureFile, "start");
        this.startTime = TimeUtil.secondsSince1981ToSecondsSince1978(startTime);
        step = getMetaInfo(detectorTemperatureFile, "step");
        final int numberOfRecords = getMetaInfo(detectorTemperatureFile, "number of records");
        temperatures = new float[numberOfRecords];
    }

    private int getMetaInfo(File file, String type) throws IOException {
        String line = getLine(file, type);
        final String value = line.replace(String.format("# %s:", type), "").trim();
        return Integer.parseInt(value);
    }

    @SuppressWarnings({"NestedAssignment", "UnnecessaryContinue"})
    private String getLine(File file, String type) throws IOException {
        final BufferedReader metaInfoReader = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = metaInfoReader.readLine()) != null) {
                if (line.matches(String.format("# %s:.*", type))) {
                    return line;
                }
            }
        } finally {
            metaInfoReader.close();
        }
        throw new ToolException("Unable to read from detector temperature file.", ToolException.TOOL_IO_ERROR);
    }
}
