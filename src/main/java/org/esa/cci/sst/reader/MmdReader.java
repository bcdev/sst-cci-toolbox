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

package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.IoUtil;
import org.postgis.PGgeometry;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import javax.persistence.Query;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Reader for reading from an mmd file. Does not implement the <code>write</code>-method.
 *
 * @author Thomas Storm
 */
public class MmdReader implements IOHandler {

    private static final String DATAFILE_QUERY = "SELECT df " +
                                                 "FROM DataFile df, DataSchema ds " +
                                                 "WHERE ds.sensorType = '%s\' " +
                                                 "AND df.dataSchema = ds";

    private static final String MAXIMUM_RECORD_NUMBER = "SELECT MAX(recordno) " +
                                                        "FROM mm_observation";

    private static final String GET_LOCATION = "SELECT o.location " +
                                               "FROM mm_observation o, mm_matchup m " +
                                               "WHERE o.id = m.refobs_id " +
                                               "AND m.id = %d";

    private static final String VARIABLE_NAME_MATCHUP = "matchup_id";
    private static final String VARIABLE_NAME_SEA_SURFACE_TEMPERATURE = "atsr.3.sea_surface_temperature.ARC.N2";

    private NetcdfFile mmd;
    private int maxRecordNumber = -1;

    private final PersistenceManager persistenceManager;
    private final String sensor;
    private final String schemaName;

    public MmdReader(final PersistenceManager persistenceManager, final String sensor, final String schemaName) {
        this.persistenceManager = persistenceManager;
        this.sensor = sensor;
        this.schemaName = schemaName;
    }

    @Override
    public void init(final DataFile dataFile) throws IOException {
        final String fileLocation = dataFile.getPath();
        validateFileLocation(fileLocation);
        mmd = NetcdfFile.open(fileLocation);
    }

    @Override
    public void close() {
        if (mmd != null) {
            try {
                mmd.close();
            } catch (IOException ignore) {
            }
        } else {
            throw new IllegalStateException("No file opened - has init() not been called?");
        }
    }

    @Override
    public int getNumRecords() {
        final Dimension recordDimension = getRecordDimension();
        return recordDimension.getLength();
    }

    @Override
    public Observation readObservation(final int recordNo) throws IOException {
        if (getNumRecords() < recordNo) {
            throw new IllegalArgumentException(MessageFormat.format("Invalid record number: ''{0}''.", recordNo));
        }
        final RelatedObservation observation = new RelatedObservation();
        setObservationLocation(observation, recordNo);
        observation.setDatafile(getDatafile());
        observation.setName(String.format("mmd_observation_%d", recordNo));
        setObservationRecordNo(observation);
        observation.setSensor(sensor);
        setObservationTime(recordNo, observation);
        return observation;
    }

    private void setObservationTime(final int recordNo, final RelatedObservation observation) throws IOException {
        final Date creationDate = getCreationDate(recordNo);
        observation.setTime(creationDate);
    }

    private void setObservationRecordNo(final RelatedObservation observation) {
        if (maxRecordNumber == -1) {
            final Query maxRecordNumberQuery = persistenceManager.createNativeQuery(MAXIMUM_RECORD_NUMBER,
                                                                                    Integer.class);
            maxRecordNumber = (Integer) maxRecordNumberQuery.getSingleResult();
        }

        maxRecordNumber++;
        observation.setRecordNo(maxRecordNumber);
    }

    @Override
    public VariableDescriptor[] getVariableDescriptors() throws IOException {
        final List<VariableDescriptor> variableDescriptors = new ArrayList<VariableDescriptor>();
        final List<Variable> variables = mmd.getVariables();
        final DataFile datafile = getDatafile();
        for (Variable variable : variables) {
            variableDescriptors.add(createVariableDescriptor(variable, datafile));
        }
        return variableDescriptors.toArray(new VariableDescriptor[variableDescriptors.size()]);
    }

    @Override
    public void write(final NetcdfFileWriteable targetFile, final Observation sourceObservation,
                      final String sourceVariableName, final String targetVariableName, final int targetRecordNumber,
                      final PGgeometry refPoint, final Date refTime) throws IOException {
        throw new IllegalStateException("not needed, therefore not implemented");
    }

    public int getMatchupId(final int recordNo) throws IOException {
        final Variable matchupIds = mmd.findVariable(NetcdfFile.escapeName(VARIABLE_NAME_MATCHUP));
        final Array matchupId = readMatchupId(recordNo, matchupIds);
        return matchupId.getInt(0);
    }

    Variable getSSTVariable() {
        mmd.getVariables();
        final String escapedVarName = NetcdfFile.escapeName(VARIABLE_NAME_SEA_SURFACE_TEMPERATURE);
        final Variable variable = mmd.findVariable(escapedVarName);
        if (variable == null) {
            throw new IllegalStateException(
                    MessageFormat.format("Mmd file does not contain a variable called ''{0}''.",
                                         VARIABLE_NAME_SEA_SURFACE_TEMPERATURE));
        }
        return variable;
    }

    Date getCreationDate(final int matchupId) throws IOException {
        final Variable variable = mmd.findVariable(Constants.VARIABLE_NAME_TIME);
        final Array timeArray;
        try {
            timeArray = variable.read(new int[]{matchupId}, new int[]{1});
        } catch (Exception e) {
            throw new IOException("Unable to read time.", e);
        }
        final long time = (long) timeArray.getDouble(0);
        return new Date(time);
    }

    void setObservationLocation(final RelatedObservation observation, int recordNo) throws IOException {
        final int matchupId = getMatchupId(recordNo);
        final String getLocation = String.format(GET_LOCATION, matchupId);
        final Query query = persistenceManager.createNativeQuery(getLocation);
        final Object result = query.getSingleResult();
        final PGgeometry geometry;
        try {
            geometry = new PGgeometry(result.toString());
        } catch (SQLException e) {
            throw new IOException("Unable to set location", e);
        }
        observation.setLocation(geometry);
    }

    private Array readMatchupId(final int recordNo, final Variable matchupIds) throws IOException {
        final Array matchupId;
        try {
            matchupId = matchupIds.read(new int[]{recordNo}, new int[]{1});
        } catch (InvalidRangeException e) {
            throw new IOException(
                    MessageFormat.format("Unable to read matchup_id from file ''{0}''.", mmd.getLocation()), e);
        }
        return matchupId;
    }

    private VariableDescriptor createVariableDescriptor(final Variable variable, final DataFile dataFile) {
        final VariableDescriptor variableDescriptor = IoUtil.createVariableDescriptor(variable, sensor);
        variableDescriptor.setDataSchema(dataFile.getDataSchema());
        return variableDescriptor;
    }

    private Dimension getRecordDimension() {
        String recordDimensionName = Constants.DIMENSION_NAME_MATCHUP;
        Dimension recordDimension = mmd.findDimension(recordDimensionName);
        validateRecordDimension(recordDimensionName, recordDimension);
        return recordDimension;
    }

    private DataFile getDatafile() {
        final String queryString = String.format(DATAFILE_QUERY, schemaName);
        final Query query = persistenceManager.createQuery(queryString);
        return (DataFile) query.getSingleResult();
    }

    private void validateRecordDimension(final String recordDimensionName, final Dimension recordDimension) {
        if (recordDimension == null) {
            throw new IllegalStateException(
                    MessageFormat.format("Mmd file does not contain a record dimension called ''{0}''.",
                                         recordDimensionName));
        }
    }

    private void validateFileLocation(final String fileLocation) throws IOException {
        if (!NetcdfFile.canOpen(fileLocation)) {
            throw new IOException(MessageFormat.format("File ''{0}'' cannot be opened.", fileLocation));
        }
    }
}
