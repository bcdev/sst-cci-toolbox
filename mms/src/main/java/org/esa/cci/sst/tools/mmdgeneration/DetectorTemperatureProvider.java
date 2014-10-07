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

package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.beam.util.io.CsvReader;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.util.TimeUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

/**
 * Provides the detector temperature for a given date.
 *
 * @author Thomas Storm
 */
public class DetectorTemperatureProvider {

    public static final float FILL_VALUE = Float.MIN_VALUE;

    static DetectorTemperatureProvider singleton = null;
    float[] temperatures;
    int startTime;
    int step;

    DetectorTemperatureProvider() {
        init("detector_temperature.dat");
    }

    DetectorTemperatureProvider(String resourceName) {
        init(resourceName);
    }

    /**
     * Factory method to return and maybe create the singleton
     * DetectorTemperatureProvider for the default data file.
     * @return  a DetectorTemperatureProvider
     */
    public static DetectorTemperatureProvider create() {
        if (singleton == null) {
            singleton = new DetectorTemperatureProvider();
        }
        return singleton;
    }

    /**
     * Returns the detector temperature for the given date.
     * @param date The date to get the detector temperature for.
     * @return The detector temperature.
     */
    public float getDetectorTemperature(Date date) {
        final long millisSince1970 = date.getTime();
        final long millisSince1978 = millisSince1970 - TimeUtil.MILLIS_1978;
        final double secondsSinceCciEpoch = millisSince1978 / 1000;
        int index = (int) Math.round((secondsSinceCciEpoch - startTime) / step);
        if(index >= temperatures.length) {
            return FILL_VALUE;
        }
        return temperatures[index];
    }

    private void init(String resource) {
        try {
            readMetaInfo(resource);
            readTemperatures(resource);
        } catch (IOException e) {
            throw new ToolException("Unable to read from detector temperature file.", e, ToolException.TOOL_IO_ERROR);
        }
    }

    private void readTemperatures(String resource) throws IOException {
        final InputStream inputStream = getClass().getResourceAsStream(resource);
        final InputStreamReader reader = new InputStreamReader(inputStream);
        final char[] separators = {' ', '\n'};
        final CsvReader csvReader = new CsvReader(reader, separators, true, "#");
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

    private void readMetaInfo(String resource) throws IOException {
        int startTime = getMetaInfo(resource, "start");
        this.startTime = (int) TimeUtil.secondsSince1981ToSecondsSinceEpoch(startTime);
        step = getMetaInfo(resource, "step");
        final int numberOfRecords = getMetaInfo(resource, "number of records");
        temperatures = new float[numberOfRecords];
    }

    private int getMetaInfo(String resource, String type) throws IOException {
        String line = getLine(resource, type);
        final String value = line.replace(String.format("# %s:", type), "").trim();
        return Integer.parseInt(value);
    }

    @SuppressWarnings({"NestedAssignment"})
    private String getLine(String resource, String type) throws IOException {
        final InputStream inputStream = getClass().getResourceAsStream(resource);
        final BufferedReader metaInfoReader = new BufferedReader(new InputStreamReader(inputStream));
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
