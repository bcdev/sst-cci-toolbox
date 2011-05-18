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

import org.esa.beam.watermask.operator.GeoRectangle;
import org.esa.beam.watermask.operator.WatermaskClassifier;
import org.esa.cci.sst.tools.BasicTool;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Writes a land water mask band to a given netcdf file.
 * Prerequisites:
 * <ul>
 * <li>the target file has to comprise the correct structure already, that is
 * <li>the given latitude and longitude variables have to exist
 * <li>the x and y dimensions have to exist
 * <li>the lat/lon variables have to be written to the file
 * </ul>
 *
 * @author Thomas Storm
 */
class LandWaterMaskWriter {

    private static final int SUBSAMPLING_FACTOR = 10;

    private final NetcdfFileWriteable file;
    private final BasicTool tool;
    private final WatermaskClassifier classifier;
    private String[] targetVariables;
    private final List<Variable> sourceLongitudes = new ArrayList<Variable>(1);
    private final List<Variable> sourceLatitudes = new ArrayList<Variable>(1);
    private final List<Dimension> sourceXDimensions = new ArrayList<Dimension>(1);
    private final List<Dimension> sourceYDimensions = new ArrayList<Dimension>(1);

    private int matchupIndex;

    LandWaterMaskWriter(final NetcdfFileWriteable file, final BasicTool tool) throws IOException {
        this.file = file;
        this.tool = tool;
        readSourceGeoVariables("mmd.watermask.source.latitude", sourceLatitudes, file);
        readSourceGeoVariables("mmd.watermask.source.longitude", sourceLongitudes, file);
        readTargetDimensions("mmd.watermask.target.xdimension", sourceXDimensions, file);
        readTargetDimensions("mmd.watermask.target.ydimension", sourceYDimensions, file);
        setTargetVariables();
        classifier = new WatermaskClassifier(WatermaskClassifier.RESOLUTION_50);
    }

    void writeLandWaterMask(int matchupIndex) throws IOException {
        this.matchupIndex = matchupIndex;
        for (int i = 0; i < sourceXDimensions.size(); i++) {
            final Dimension sourceXDimension = sourceXDimensions.get(i);
            final Dimension sourceYDimension = sourceYDimensions.get(i);
            final Variable sourceLatitude = sourceLatitudes.get(i);
            final Variable sourceLongitude = sourceLongitudes.get(i);
            final String targetVariable = targetVariables[i];
            for (int x = 0; x < sourceXDimension.getLength(); x++) {
                for (int y = 0; y < sourceYDimension.getLength(); y++) {
                    GeoRectangle geoRectangle = computeGeoRectangle(sourceLatitude, sourceLongitude, x, y);
                    float value = tryAndGetValue(geoRectangle);
                    if(value == -1.0f) {
                        return;
                    }
                    writeValue(targetVariable, Array.factory(value), new int[]{matchupIndex, x, y});
                }
            }
        }
    }

    private float tryAndGetValue(GeoRectangle geoRectangle) {
        final float value;
        try {
            value = classifier.getWaterMaskFraction(geoRectangle, SUBSAMPLING_FACTOR);
        } catch (IOException e) {
            handle(e);
            return -1.0f;
        }
        return value;
    }

    private void handle(Exception e) {
        final String message = MessageFormat.format(
                "Unable to write land/water mask for matchup with index ''{0}''. Reason: ''{1}''.",
                this.matchupIndex, e.getMessage());
        tool.getLogger().warning(message);
    }

    private GeoRectangle computeGeoRectangle(Variable sourceLatitude, Variable sourceLongitude, int x, int y) throws IOException {
        float startLat = readSingleFloat(sourceLatitude, x, y);
        float startLon = readSingleFloat(sourceLongitude, x, y);
        float endLat;
        float endLon;
        try {
            endLat = readSingleFloat(sourceLatitude, x, y + 1);
            endLon = readSingleFloat(sourceLongitude, x + 1, y);
        } catch (IOException e) {
            endLat = startLat;
            endLon = startLon;
        }
        return new GeoRectangle(startLat, endLat, startLon, endLon);
    }

    private void setTargetVariables() {
        final String variableNames = getProperty("mmd.watermask.target.variablename");
        targetVariables = variableNames.split(" ");
    }

    private void readTargetDimensions(String key, final List<Dimension> list, final NetcdfFileWriteable file) {
        final String propertyValue = getProperty(key);
        final String[] sourceDimensions = propertyValue.split(" ");
        for (final String sourceDimension : sourceDimensions) {
            list.add(file.findDimension(sourceDimension));
        }
    }

    private void readSourceGeoVariables(String key, final List<Variable> list, final NetcdfFileWriteable file) {
        final String propertyValue = NetcdfFile.escapeName(getProperty(key));
        final String[] sourceGeoVariables = propertyValue.split(" ");
        for (final String sourceGeoVariableName : sourceGeoVariables) {
            list.add(file.findVariable(sourceGeoVariableName));
        }
    }

    private void writeValue(final String targetVariable, final Array value, final int[] origin) throws IOException {
        try {
            file.write(NetcdfFile.escapeName(targetVariable), origin, value);
        } catch (InvalidRangeException e) {
            throw new IOException(
                    MessageFormat.format("Unable to write into variable ''{0}''.", targetVariable));
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
