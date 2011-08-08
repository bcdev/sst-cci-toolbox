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

package org.esa.cci.sst.reader;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.VariableSampleSource;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.tools.Constants;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Reader for reading from an mmd file.
 *
 * @author Thomas Storm
 */
public class MmdReader implements Reader {

    private NetcdfFile ncFile;
    private Variable matchupIds;
    private ObservationReader delegateReader;
    private DataFile dataFile;
    private Properties configuration;
    private final String sensorName;

    public MmdReader(String sensorName) {
        this.sensorName = sensorName;
    }

    @Override
    public void init(final DataFile dataFile, File archiveRoot) throws IOException {
        this.dataFile = dataFile;
        final String fileLocation;
        if (archiveRoot == null || dataFile.getPath().startsWith(File.separator)) {
            fileLocation = dataFile.getPath();
        } else {
            fileLocation = archiveRoot.getPath() + File.separator + dataFile.getPath();
        }
        validateFileLocation(fileLocation);
        ncFile = NetcdfFile.open(fileLocation);
        matchupIds = ncFile.findVariable(NetcdfFile.escapeName(Constants.VARIABLE_NAME_MATCHUP_ID));
        // allow for matchup_id instead of matchup.id to support ARC2 output
        if (matchupIds == null) {
            matchupIds = ncFile.findVariable(NetcdfFile.escapeName(Constants.VARIABLE_NAME_ARC2_MATCHUP_ID));
        }
        final String property = getProperty(Constants.PROPERTY_MMS_REINGESTION_LOCATED, "no");
        if ("yes".equals(property)) {
            delegateReader = new MmdObservationReader(dataFile, ncFile, sensorName);
        } else {
            delegateReader = new MmdArcReader(dataFile, ncFile, sensorName);
        }
    }

    @Override
    public int getNumRecords() {
        validateDelegate(delegateReader);
        return delegateReader.getNumRecords();
    }

    @Override
    public Observation readObservation(final int recordNo) throws IOException {
        validateDelegate(delegateReader);
        return delegateReader.readObservation(recordNo);
    }

    @Override
    public Item getColumn(String role) {
        validateDelegate(delegateReader);
        return delegateReader.getColumn(role);
    }

    @Override
    public Item[] getColumns() {
        validateDelegate(delegateReader);
        return delegateReader.getColumns();
    }

    @Override
    public DataFile getDatafile() {
        return dataFile;
    }

    @Override
    public GeoCoding getGeoCoding(int recordNo) throws IOException {
        if (ncFile.findVariable("longitude") == null || ncFile.findVariable("latitude") == null) {
            return null;
        }
        final VariableSampleSource lonSampleSource = getVariableSampleSource(recordNo, "longitude");
        final VariableSampleSource latSampleSource = getVariableSampleSource(recordNo, "latitude");
        return new PixelLocatorGeoCoding(lonSampleSource, latSampleSource);
    }

    private VariableSampleSource getVariableSampleSource(int recordNo, String variableName) throws IOException {
        final Variable variable = ncFile.findVariable(variableName);
        final int[] origin = {recordNo, 0, 0};
        final int[] shape = variable.getShape();
        shape[0] = 1;
        final Array slice;
        try {
            slice = variable.read(origin, shape);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
        return new VariableSampleSource(variable, slice);
    }

    @Override
    public double getDTime(int recordNo, int scanLine) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public long getTime(int recordNo, int scanLine) throws IOException {
        final int[] origin = {recordNo, scanLine};
        final int[] shape = {1, 1};
        final int year = getSingleInt(origin, shape, "time_year");
        final int day = getSingleInt(origin, shape, "time_day_num");
        final int milliseconds = getSingleInt(origin, shape, "time_utc_msecs");
        if(year == Short.MIN_VALUE || day == Short.MIN_VALUE || milliseconds == Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        final int second = milliseconds / 1000;
        final int minute = second / 60;
        final int hour = minute / 60;
        final int millisecond = milliseconds % 1000;

        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        calendar.clear();
        calendar.set(GregorianCalendar.YEAR, year);
        calendar.set(GregorianCalendar.DAY_OF_YEAR, day);
        calendar.set(GregorianCalendar.HOUR_OF_DAY, hour);
        calendar.set(GregorianCalendar.MINUTE, minute);
        calendar.set(GregorianCalendar.SECOND, second);
        calendar.set(GregorianCalendar.MILLISECOND, millisecond);
        return calendar.getTimeInMillis();
    }

    private int getSingleInt(int[] origin, int[] shape, String varName) throws IOException {
        final Variable variable = ncFile.findVariable(NetcdfFile.escapeName(varName));
        final Array array;
        try {
            array = variable.read(origin, shape);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
        return array.getInt(0);
    }

    @Override
    public final Array read(String role, ExtractDefinition extractDefinition) throws IOException {
        final Variable variable = ncFile.findVariable(NetcdfFile.escapeName(role));
        final int recordNo = extractDefinition.getRecordNo();

        if (variable.getDataType().isString()) {
            int[] origin = new int[variable.getRank()];
            origin[0] = recordNo;
            final int[] shape = variable.getShape();
            shape[0] = 1;
            return readData(variable, origin, shape);
        }

        final int[] shape = extractDefinition.getShape();
        int[] origin = new int[variable.getRank()];
        origin[0] = recordNo;
        for (int i = 1; i < variable.getRank(); i++) {
            origin[i] = (variable.getShape(i) - shape[i]) / 2;
        }

        return readData(variable, origin, shape);
    }

    @Override
    public void close() {
        delegateReader = null;
        if (ncFile != null) {
            try {
                ncFile.close();
            } catch (IOException ignore) {
            }
        } else {
            throw new IllegalStateException("No file opened - has init() not been called?");
        }
    }

    @Override
    public int getLineSkip() {
        return 0;
    }

    @Override
    public InsituSource getInsituSource() {
        return null;
    }

    @Override
    public Product getProduct() {
        return null;
    }

    @Override
    public int getScanLineCount() {
        final Variable latitude = ncFile.findVariable("latitude");
        if(latitude == null) {
            throw new IllegalStateException("Only implemented for ARC2 inputs.");
        }
        return latitude.getShape()[latitude.getRank() - 2];
    }

    @Override
    public int getElementCount() {
        final Variable longitude = ncFile.findVariable("longitude");
        if(longitude == null) {
            throw new IllegalStateException("Only implemented for ARC2 inputs.");
        }
        return longitude.getShape()[longitude.getRank() - 2];
    }

    public int getMatchupId(final int recordNo) throws IOException {
        final Array matchupId = readData(matchupIds, new int[]{recordNo}, new int[]{1});
        return matchupId.getInt(0);
    }

    Array readData(final Variable variable, final int[] origin, final int[] shape) throws IOException {
        Assert.notNull(variable, "Trying to read from non-existing variable.");
        try {
            return variable.read(origin, shape);
        } catch (InvalidRangeException e) {
            throw new IOException(
                    MessageFormat.format("Unable to read from file ''{0}''.", ncFile.getLocation()), e);
        }
    }

    String getProperty(final String key, final String defaultValue) {
        if (configuration == null) {
            return defaultValue;
        }
        return configuration.getProperty(key, defaultValue);
    }

    private void validateFileLocation(final String fileLocation) throws IOException {
        if (!NetcdfFile.canOpen(fileLocation)) {
            throw new IOException(MessageFormat.format("File ''{0}'' cannot be opened.", fileLocation));
        }
    }

    private void validateDelegate(final Object delegate) {
        if (delegate == null) {
            throw new IllegalStateException("Trying to read without calling init() beforehand.");
        }
    }

    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }
}
