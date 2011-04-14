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

/**
 * @author Thomas Storm
 */
class LandWaterMaskWriter {

    private final NetcdfFileWriteable file;

    LandWaterMaskWriter(final NetcdfFileWriteable file) {
        this.file = file;
    }

    void writeLandWaterMask(int matchupIndex) throws IOException {
        final Dimension xDimension = file.findDimension("atsr.ni");
        final Dimension yDimension = file.findDimension("atsr.nj");
        final Variable latitude = file.findVariable(NetcdfFile.escapeName("aatsr.latitude"));
        final Variable longitude = file.findVariable(NetcdfFile.escapeName("aatsr.longitude"));
        final WatermaskClassifier classifier = new WatermaskClassifier(WatermaskClassifier.RESOLUTION_50);
        for (int x = 0; x < xDimension.getLength(); x++) {
            for (int y = 0; y < yDimension.getLength(); y++) {
                float lat = readSingleFloat(matchupIndex, latitude, x, y);
                float lon = readSingleFloat(matchupIndex, longitude, x, y);
                final short sample = (short) classifier.getWaterMaskSample(lat, lon);
                final Array value = Array.factory(DataType.SHORT, new int[]{1, 1, 1}, new short[]{sample});
                final int[] origin = {matchupIndex, x, y};
                writeValue(value, origin);
            }
        }
    }

    private void writeValue(final Array value, final int[] origin) throws IOException {
        try {
            file.write(Constants.VARIABLE_NAME_WATERMASK, origin, value);
        } catch (InvalidRangeException e) {
            throw new IOException(
                    MessageFormat.format("Unable to write into variable ''{0}''.", Constants.VARIABLE_NAME_WATERMASK));
        }
    }

    private float readSingleFloat(final int matchupId, final Variable variable, final int x, final int y) throws
                                                                                                          IOException {
        final int[] origin = {matchupId, x, y};
        final int[] shape = {1, 1, 1};
        final Array latArray;
        try {
            latArray = variable.read(origin, shape);
        } catch (InvalidRangeException e) {
            throw new IOException(MessageFormat.format("Unable to read from variable ''{0}''.", variable.getName()), e);
        }
        return latArray.getFloat(0);
    }

}
