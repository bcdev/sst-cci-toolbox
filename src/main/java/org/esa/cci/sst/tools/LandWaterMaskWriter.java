/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools;

import org.esa.beam.watermask.operator.WatermaskClassifier;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * Writes a land water mask band to a given netcdf file. That file has to comprise the correct structure already, that
 * is, the given latitude and longitude variables have to exist as well as the x and y dimensions. Additionally, the
 * lat/lon variables have to be written to the file before the land water mask is written.
 *
 * @author Thomas Storm
 */
class LandWaterMaskWriter {

    private final NetcdfFileWriteable file;
    private final MmsTool tool;
    private final Variable latitude;
    private final Variable longitude;
    private final WatermaskClassifier classifier;

    private int matchupIndex;

    LandWaterMaskWriter(final NetcdfFileWriteable file, final MmsTool tool) throws IOException {
        this.file = file;
        this.tool = tool;
        final String sourceLat = NetcdfFile.escapeName(getProperty("mmd.watermask.source.latitude"));
        latitude = file.findVariable(sourceLat);
        final String sourceLon = NetcdfFile.escapeName(getProperty("mmd.watermask.source.longitude"));
        longitude = file.findVariable(sourceLon);
        classifier = new WatermaskClassifier(WatermaskClassifier.RESOLUTION_50);
    }

    void writeLandWaterMask(int matchupIndex) throws IOException {
        this.matchupIndex = matchupIndex;
        final Dimension xDimension = file.findDimension(getProperty("mmd.watermask.target.xdimension"));
        final Dimension yDimension = file.findDimension(getProperty("mmd.watermask.target.ydimension"));
        for (int x = 0; x < xDimension.getLength(); x++) {
            for (int y = 0; y < yDimension.getLength(); y++) {
                final Array value = tryAndGetValue(x, y);
                if (value == null) {
                    return;
                }
                writeValue(value, new int[]{matchupIndex, x, y});
            }
        }
    }

    private Array tryAndGetValue(final int x, final int y) {
        Array value = null;
        try {
            value = getValue(x, y);
        } catch (IOException e) {
            final String message = MessageFormat.format(
                    "Unable to write land/water mask for matchup with index ''{0}''. Reason: ''{1}''.",
                    matchupIndex, e.getMessage());
            tool.getLogger().warning(message);
        }
        return value;
    }

    private Array getValue(final int x, final int y) throws IOException {
        float lat = readSingleFloat(latitude, x, y);
        float lon = readSingleFloat(longitude, x, y);
        final short sample = (short) classifier.getWaterMaskSample(lat, lon);
        return Array.factory(DataType.SHORT, new int[]{1, 1, 1}, new short[]{sample});
    }

    private void writeValue(final Array value, final int[] origin) throws IOException {
        try {
            file.write(Constants.VARIABLE_NAME_WATERMASK, origin, value);
        } catch (InvalidRangeException e) {
            throw new IOException(
                    MessageFormat.format("Unable to write into variable ''{0}''.", Constants.VARIABLE_NAME_WATERMASK));
        }
    }

    private float readSingleFloat(final Variable variable, final int x, final int y) throws IOException {
        final int[] origin = {matchupIndex, x, y};
        final int[] shape = {1, 1, 1};
        final Array latArray;
        try {
            latArray = variable.read(origin, shape);
        } catch (InvalidRangeException e) {
            throw new IOException(MessageFormat.format("Unable to read from variable ''{0}''.", variable.getName()), e);
        }
        return latArray.getFloat(0);
    }

    private String getProperty(final String key) {
        final Properties configuration = tool.getConfiguration();
        return configuration.getProperty(key);
    }
}
