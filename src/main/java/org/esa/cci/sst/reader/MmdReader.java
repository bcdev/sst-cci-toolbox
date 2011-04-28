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
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.persistence.Query;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Thomas Storm
 */
class MmdReader implements ObservationReader {

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

    private static final String CORRESPONDING_VARIABLE_QUERY = "SELECT v " +
                                                               "FROM VariableDescriptor v, DataSchema d " +
                                                               "WHERE v.name = '%s' " +
                                                               "AND v.dataSchema.id = d.id " +
                                                               "AND NOT d.name = '%s'";

    private final MmdIOHandler mmdIOHandler;
    private final PersistenceManager persistenceManager;
    private final NetcdfFile mmd;
    private final String sensor;
    private final String schemaName;

    private int maxRecordNumber = -1;

    MmdReader(final MmdIOHandler mmdIOHandler, final PersistenceManager persistenceManager, final NetcdfFile mmd,
              final String sensor, final String schemaName) {
        this.mmdIOHandler = mmdIOHandler;
        this.persistenceManager = persistenceManager;
        this.mmd = mmd;
        this.sensor = sensor;
        this.schemaName = schemaName;
    }

    @Override
    public Observation readObservation(final int recordNo) throws IOException {
        validateRecordNumber(recordNo);
        final RelatedObservation observation = new RelatedObservation();
        setObservationLocation(observation, recordNo);
        setupObservation(recordNo, observation);
        setObservationTime(recordNo, observation);
        return observation;
    }

    @Override
    public int getNumRecords() {
        final Variable variable = mmd.findVariable(NetcdfFile.escapeName(Constants.VARIABLE_NAME_MATCHUP_ID));
        return variable.getDimensions().get(0).getLength();
    }

    @Override
    public VariableDescriptor[] getVariableDescriptors() throws IOException {
        final List<VariableDescriptor> variableDescriptors = new ArrayList<VariableDescriptor>();
        final List<Variable> variables = mmd.getVariables();
        final DataFile datafile = getDatafile();
        for (Variable variable : variables) {
                final VariableDescriptor variableDescriptor = createVariableDescriptor(variable, datafile);
                copyVariableProperties(variable, variableDescriptor);
                if (variableDescriptor.getDimensionRoles() == null) {
                    variableDescriptor.setDimensionRoles(variableDescriptor.getDimensions());
                }
                variableDescriptors.add(variableDescriptor);
            }
        return variableDescriptors.toArray(new VariableDescriptor[variableDescriptors.size()]);
    }

    Date getCreationDate(final int matchupId) throws IOException {
        final Variable variable = mmd.findVariable(Constants.VARIABLE_NAME_TIME);
        final Array timeArray = mmdIOHandler.readData(variable, new int[]{matchupId}, new int[]{1});
        final long time = (long) timeArray.getDouble(0);
        return new Date(time);
    }


    void setupObservation(final int recordNo, final Observation observation) throws IOException {
        observation.setDatafile(getDatafile());
        observation.setName(String.format("observation_%d", recordNo));
        setObservationRecordNo(observation);
        observation.setSensor(sensor);
    }

    void validateRecordNumber(final int recordNo) {
        if (getNumRecords() < recordNo) {
            throw new IllegalArgumentException(MessageFormat.format("Invalid record number: ''{0}''.", recordNo));
        }
    }

    private void setObservationLocation(final RelatedObservation observation, int recordNo) throws IOException {
        final int matchupId = mmdIOHandler.getMatchupId(recordNo);
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

    private void setObservationTime(final int recordNo, final RelatedObservation observation) throws IOException {
        final Date creationDate = getCreationDate(recordNo);
        observation.setTime(creationDate);
    }

    private void setObservationRecordNo(final Observation observation) {
        if (maxRecordNumber == -1) {
            final Query maxRecordNumberQuery = persistenceManager.createNativeQuery(MAXIMUM_RECORD_NUMBER,
                                                                                    Integer.class);
            maxRecordNumber = (Integer) maxRecordNumberQuery.getSingleResult();
        }

        maxRecordNumber++;
        observation.setRecordNo(maxRecordNumber);
    }

    private void copyVariableProperties(final Variable variable, final VariableDescriptor variableDescriptor) {
        final VariableDescriptor sourceDescriptor = getSourceDescriptor(variable);
        if (sourceDescriptor != null) {
            variableDescriptor.setDimensionRoles(sourceDescriptor.getDimensionRoles());
            variableDescriptor.setAddOffset(sourceDescriptor.getAddOffset());
            variableDescriptor.setFillValue(sourceDescriptor.getFillValue());
            variableDescriptor.setScaleFactor(sourceDescriptor.getScaleFactor());
            variableDescriptor.setStandardName(sourceDescriptor.getStandardName());
            variableDescriptor.setUnits(sourceDescriptor.getUnit());
            variableDescriptor.setValidMin(sourceDescriptor.getValidMin());
            variableDescriptor.setValidMax(sourceDescriptor.getValidMax());
        }
    }

    @SuppressWarnings({"unchecked"})
    private VariableDescriptor getSourceDescriptor(final Variable variable) {
        final String schemaName = mmdIOHandler.getProperty("mms.reingestion.schemaname");
        final String queryString = String.format(CORRESPONDING_VARIABLE_QUERY, variable.getName(), schemaName);
        final Query query = persistenceManager.createQuery(queryString);
        final List<VariableDescriptor> resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return null;
        }
        return resultList.get(0);
    }

    private VariableDescriptor createVariableDescriptor(final Variable variable, final DataFile dataFile) {
        final VariableDescriptor variableDescriptor = IoUtil.createVariableDescriptor(variable, sensor);
        variableDescriptor.setDataSchema(dataFile.getDataSchema());
        return variableDescriptor;
    }

    private DataFile getDatafile() {
        final String queryString = String.format(DATAFILE_QUERY, schemaName);
        final Query query = persistenceManager.createQuery(queryString);
        return (DataFile) query.getSingleResult();
    }
}
