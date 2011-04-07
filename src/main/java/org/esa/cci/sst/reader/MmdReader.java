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

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DataSchema;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.util.DataUtil;
import org.esa.cci.sst.util.ReaderUtil;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
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

    // todo - ts 6Apr2011 - review
    private static final String CORRESPONDING_OBSERVATION_TIME_QUERY = "SELECT o.time " +
                                                                       "FROM mm_coincidence c, mm_observation o " +
                                                                       "WHERE c.matchup_id = %d " +
                                                                       "AND c.observation_id = o.id " +
                                                                       "AND ST_Intersects(o.location, '%s') " +
                                                                       "AND ST_Intersects(o.location, '%s') " +
                                                                       "ORDER BY o.time";

    private static final String CORRESPONDING_REFERENCE_OBSERVATION_TIME_QUERY = "SELECT o.time " +
                                                                                 "FROM mm_observation o " +
                                                                                 "WHERE ST_Intersects(o.location, '%s') " +
                                                                                 "AND ST_Intersects(o.location, '%s') " +
                                                                                 "AND o.dtype = 'ReferenceObservation'" +
                                                                                 "ORDER BY o.time";

    private static final String MAXIMUM_RECORD_NUMBER = "SELECT MAX(recordno) " +
                                                        "FROM mm_observation";

    private static final String RECORD_DIMENSION_NAME = "record";
    private static final String VARIABLE_NAME_SEA_SURFACE_TEMPERATURE = "atsr.3.sea_surface_temperature.ARC.N2";
    private static final String VARIABLE_NAME_MATCHUP = "matchup_id";

    private NetcdfFile mmd;
    private final PersistenceManager persistenceManager;

    public MmdReader(final PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
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
        observation.setDatafile(createDatafile());
        observation.setName("mmd_observation_" + recordNo);
        setObservationRecordNo(observation);
        observation.setSensor("ARC");   // todo - ts 04Apr2011 - ok?
        setObservationTime(recordNo, observation);
        return observation;
    }

    private void setObservationTime(final int recordNo, final RelatedObservation observation) throws IOException {
        final int matchupId = getMatchupId(recordNo);
        System.out.println("matchupId = " + matchupId);
        final Date creationDate = getCreationDate(matchupId, observation);
        observation.setTime(creationDate);
    }

    private int getMatchupId(final int recordNo) throws IOException {
        final Variable matchupIds = mmd.findVariable(NetcdfFile.escapeName(VARIABLE_NAME_MATCHUP));
        final Array matchupId = readMatchupId(recordNo, matchupIds);
        return matchupId.getInt(0);
    }

    private Array readMatchupId(final int recordNo, final Variable matchupIds) throws IOException {
        final Array matchupId;
        try {
            matchupId = matchupIds.read(new int[]{recordNo}, new int[]{1});
        } catch (InvalidRangeException e) {
            throw new IOException("Unable to read matchup_id from file '" + mmd.getLocation() + "'.", e);
        }
        return matchupId;
    }

    private void setObservationRecordNo(final RelatedObservation observation) {
//        observation.setRecordNo(0); // todo - ts 06Apr2011 - set to last record number + 1
    }

    @Override
    public VariableDescriptor[] getVariableDescriptors() throws IOException {
        final List<VariableDescriptor> variableDescriptors = new ArrayList<VariableDescriptor>();
        final List<Variable> variables = mmd.getVariables();
        for (Variable variable : variables) {
            variableDescriptors.add(createVariableDescriptor(variable, "ARC3", createDatafile()));
        }
        return variableDescriptors.toArray(new VariableDescriptor[variableDescriptors.size()]);
    }

    @Override
    public void write(final NetcdfFileWriteable targetFile, final Observation sourceObservation,
                      final String sourceVariableName,
                      final String targetVariableName, final int targetRecordNumber, final PGgeometry refPoint,
                      final Date refTime) throws IOException {
        throw new IllegalStateException("not needed, therefore not implemented");
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

    Date getCreationDate(final int matchupId, final RelatedObservation observation) {
        final Geometry geometry = observation.getLocation().getGeometry();
        final String firstPoint = geometry.getFirstPoint().toString();
        final String lastPoint = geometry.getLastPoint().toString();

        final String coincidenceQueryString = String.format(CORRESPONDING_OBSERVATION_TIME_QUERY, matchupId, firstPoint,
                                                            lastPoint);
        final Query coincidenceQuery = persistenceManager.createNativeQuery(coincidenceQueryString, Date.class);
        List resultList = coincidenceQuery.getResultList();

        if (resultList.isEmpty()) {
            final String refObsQueryString = String.format(CORRESPONDING_REFERENCE_OBSERVATION_TIME_QUERY, firstPoint,
                                                           lastPoint);
            final Query refObsQuery = persistenceManager.createNativeQuery(refObsQueryString, Date.class);
            resultList = refObsQuery.getResultList();

            if (resultList.isEmpty()) {
                throw new IllegalStateException("No corresponding observation found.");
            }

        }

        return (Date) resultList.get(resultList.size() / 2);
    }

    void setObservationLocation(final RelatedObservation observation, int recordNo) throws IOException {
        final Variable lon = mmd.findVariable(NetcdfFile.escapeName("lon"));
        final Variable lat = mmd.findVariable(NetcdfFile.escapeName("lat"));
        final int[] startOrigin = {recordNo, 0, 0};
        final int[] endOrigin = getEndOrigin(recordNo);
        final int[] shape = {1, 1, 1};
        final float startLon;
        final float endLon;
        final float startLat;
        final float endLat;
        try {
            startLon = lon.read(startOrigin, shape).getFloat(0);
            endLon = lon.read(endOrigin, shape).getFloat(0);
            startLat = lat.read(startOrigin, shape).getFloat(0);
            endLat = lat.read(endOrigin, shape).getFloat(0);
        } catch (Exception e) {
            throw new IOException("Unable to read location.", e);
        }
        observation.setLocation(ReaderUtil.createGeometry(startLon, startLat, endLon, endLat));
    }

    int[] getEndOrigin(final int recordNo) {
        final int lastIndexOfNiDim = mmd.findDimension("ni").getLength() - 1;
        final int lastIndexOfNjDim = mmd.findDimension("nj").getLength() - 1;
        return new int[]{recordNo, lastIndexOfNiDim, lastIndexOfNjDim};
    }

    private VariableDescriptor createVariableDescriptor(final Variable variable, final String sensorName,
                                                        final DataFile dataFile) {
        final VariableDescriptor variableDescriptor = ReaderUtil.createBasicVariableDescriptor(variable, sensorName);
        ReaderUtil.setVariableDescriptorDimensions(variable, variableDescriptor);
        ReaderUtil.setVariableDescriptorAttributes(variable, variableDescriptor);
        ReaderUtil.setVariableUnits(variable, variableDescriptor);
        ReaderUtil.setVariableDesciptorDataSchema(dataFile, variableDescriptor);
        return variableDescriptor;
    }

    private Dimension getRecordDimension() {
        final Dimension recordDimension = mmd.findDimension(RECORD_DIMENSION_NAME);
        if (recordDimension == null) {
            throw new IllegalStateException(
                    MessageFormat.format("Mmd file does not contain a record dimension called ''{0}''.",
                                         RECORD_DIMENSION_NAME));
        }
        return recordDimension;
    }

    private DataFile createDatafile() {
        final DataSchema dataSchema = createDataSchema();
        return DataUtil.createDataFile(new File(mmd.getLocation()), dataSchema);
    }

    private DataSchema createDataSchema() {
        final String sensorType = "ARC";   // todo - ts 4Apr2011 - ok?
        return DataUtil.createDataSchema(Constants.DATA_SCHEMA_NAME_MMD, sensorType);
    }

    private void validateFileLocation(final String fileLocation) throws IOException {
        if (!NetcdfFile.canOpen(fileLocation)) {
            throw new IOException("File '" + fileLocation + "' cannot be opened.");
        }
    }

}
