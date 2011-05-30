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
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.tools.Constants;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * IOHandler for reading from and writing to an mmd file.
 *
 * @author Thomas Storm
 */
public class MmdIOHandler implements IOHandler {

    private NetcdfFile ncFile;
    private final String sensorName;
    private Variable matchupIds;
    private ObservationReader reader;
    private MmdWriter writer;
    private DataFile dataFile;
    private final Properties configuration;

    @SuppressWarnings({"AssignmentToCollectionOrArrayFieldFromParameter"})
    public MmdIOHandler(Properties configuration) {
        this.sensorName = configuration.getProperty("mms.reingestion.sensor");
        this.configuration = configuration;
    }

    @Override
    public void init(final DataFile dataFile) throws IOException {
        this.dataFile = dataFile;
        final String fileLocation = dataFile.getPath();
        validateFileLocation(fileLocation);
        ncFile = NetcdfFile.open(fileLocation);
        matchupIds = ncFile.findVariable(NetcdfFile.escapeName(Constants.COLUMN_NAME_MATCHUP_ID));
        // allow for matchup_id instead of matchup.id to support ARC2 output
        if (matchupIds == null) {
            matchupIds = ncFile.findVariable(NetcdfFile.escapeName(Constants.VARIABLE_NAME_MATCHUP_ID_ALTERNATIVE));
        }
        final String property = getProperty(Constants.PROPERTY_MMS_REINGESTION_LOCATED, "no");
        if ("yes".equals(property)) {
            reader = new MmdReader(dataFile, ncFile, sensorName);
        } else {
            reader = new Arc3Reader(dataFile, ncFile, sensorName);
        }
        writer = new MmdWriter(this);
    }

    @Override
    public int getNumRecords() {
        validateDelegate(reader);
        return reader.getNumRecords();
    }

    @Override
    public Observation readObservation(final int recordNo) throws IOException {
        validateDelegate(reader);
        return reader.readObservation(recordNo);
    }

    @Override
    public Item getColumn(String role) {
        validateDelegate(reader);
        return reader.getColumn(role);
    }

    @Override
    public Item[] getColumns() {
        validateDelegate(reader);
        return reader.getColumns();
    }

    @Override
    public DataFile getDatafile() {
        return dataFile;
    }

    @Override
    public final Array read(String role, ExtractDefinition extractDefinition) throws IOException {
        final Variable variable = ncFile.findVariable(NetcdfFile.escapeName(role));
        final int[] sourceShape = variable.getShape();
        final int[] targetShape = extractDefinition.getShape();
        final int[] targetStart = new int[variable.getRank()];
        targetStart[0] = extractDefinition.getRecordNo();
        for (int i = 1; i < targetShape.length; i++) {
            targetStart[i] = sourceShape[i] / 2 - targetShape[i] / 2;
        }
        return readData(variable, targetStart, targetShape);
    }

    @Override
    public void close() {
        if (ncFile != null) {
            try {
                ncFile.close();
            } catch (IOException ignore) {
            }
        } else {
            throw new IllegalStateException("No file opened - has init() not been called?");
        }
    }

    public int getMatchupId(final int recordNo) throws IOException {
        final Array matchupId = readData(matchupIds, new int[]{recordNo}, new int[]{1});
        return matchupId.getInt(0);
    }

    Array getData(final String sourceVariableName, final int recordNo) throws IOException {
        final Variable variable = ncFile.findVariable(NetcdfFile.escapeName(sourceVariableName));
        if (recordNo >= variable.getShape()[0]) {
            throw new IllegalArgumentException("recordNo >= variable.getShape()[0]");
        }
        final int[] origin = new int[variable.getRank()];
        origin[0] = recordNo;
        final int[] shape = variable.getShape();
        shape[0] = 1;
        return readData(variable, origin, shape);
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
        return configuration.getProperty(key, defaultValue);
    }

    private void validateFileLocation(final String fileLocation) throws IOException {
        if (!NetcdfFile.canOpen(fileLocation)) {
            throw new IOException(MessageFormat.format("File ''{0}'' cannot be opened.", fileLocation));
        }
    }

    private void validateDelegate(final Object delegate) {
        if (delegate == null) {
            throw new IllegalStateException("Trying to read or write without calling init() beforehand.");
        }
    }
}
