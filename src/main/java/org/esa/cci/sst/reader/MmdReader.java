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

import com.bc.ceres.core.Assert;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.IoUtil;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Thomas Storm
 */
class MmdReader implements ObservationReader {

    private final DataFile dataFile;
    private final NetcdfFile mmd;
    private final String sensor;

    MmdReader(DataFile dataFile, final NetcdfFile mmd, final String sensor) {
        this.dataFile = dataFile;
        this.mmd = mmd;
        this.sensor = sensor;
    }

    @Override
    public Observation readObservation(final int recordNo) throws IOException {
        validateRecordNumber(recordNo);
        final RelatedObservation observation = new RelatedObservation();
        setupObservation(recordNo, observation);
        setObservationLocation(observation, recordNo);
        setObservationTime(recordNo, observation);
        observation.setRecordNo(recordNo);
        return observation;
    }

    @Override
    public int getNumRecords() {
        Variable variable = mmd.findVariable(NetcdfFile.escapeName(Constants.VARIABLE_NAME_MATCHUP_ID));
        // allow for matchup_id instead of matchup.id to support ARC2 output
        if (variable == null) {
            variable = mmd.findVariable(NetcdfFile.escapeName(Constants.VARIABLE_NAME_MATCHUP_ID_ALTERNATIVE));
        }
        return variable.getDimensions().get(0).getLength();
    }

    @Override
    public VariableDescriptor[] getVariableDescriptors() throws IOException {
        final List<VariableDescriptor> variableDescriptors = new ArrayList<VariableDescriptor>();
        final List<Variable> variables = mmd.getVariables();
        final DataFile datafile = dataFile;
        for (Variable variable : variables) {
            final VariableDescriptor variableDescriptor = createVariableDescriptor(variable, datafile);
            variableDescriptors.add(variableDescriptor);
        }
        return variableDescriptors.toArray(new VariableDescriptor[variableDescriptors.size()]);
    }

    Date getCreationDate(final int recordNo, Variable variable) throws IOException {
        // todo - mb,ts 28Apr2011 - maybe other data types
        final Double julianDate = (Double) readCenterValue(recordNo, variable);
        return TimeUtil.julianDateToDate(julianDate);
    }

    void setupObservation(final int recordNo, final Observation observation) throws IOException {
        observation.setDatafile(dataFile);
        observation.setName(String.format("observation_%d", recordNo));
        observation.setSensor(sensor);
    }

    void validateRecordNumber(final int recordNo) {
        if (getNumRecords() < recordNo) {
            throw new IllegalArgumentException(MessageFormat.format("Invalid record number: ''{0}''.", recordNo));
        }
    }

    private void setObservationLocation(final RelatedObservation observation, int recordNo) throws IOException {
        final Variable latitudeVariable = findVariable("latitude", "lat");
        final Variable longitudeVariable = findVariable("longitude", "lon");
        Assert.state(latitudeVariable != null, "No latitude variable found.");
        Assert.state(longitudeVariable != null, "No longitude variable found.");

        final float centerLatitude = (Float) readCenterValue(recordNo, latitudeVariable);
        final float centerLongitude = (Float) readCenterValue(recordNo, longitudeVariable);

        final Point centerPoint = new Point(centerLongitude, centerLatitude);
        final PGgeometry geometry = new PGgeometry(centerPoint);
        observation.setLocation(geometry);
    }

    private Object readCenterValue(int recordNo, Variable variable) throws IOException {
        final int dimCount = variable.getDimensions().size();
        final int[] origin = new int[dimCount];
        final int[] shape = new int[dimCount];

        origin[0] = recordNo;
        shape[0] = 1;
        for (int i = 1; i < dimCount; i++) {
            origin[i] = variable.getDimension(i).getLength() / 2;
            shape[i] = 1;
        }

        final Object centerValue;
        try {
            centerValue = variable.read(origin, shape).getObject(0);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
        return centerValue;
    }

    private Variable findVariable(String... variableNames) {
        for (Variable variable : mmd.getVariables()) {
            for (String name : variableNames) {
                if (variable.getName().endsWith(name)) {
                    return variable;
                }
            }
        }
        return null;
    }

    private void setObservationTime(final int recordNo, final RelatedObservation observation) throws IOException {
        // todo - mb,ts 28Apr2011 - maybe other variable names
        final Variable variable = findVariable(Constants.VARIABLE_OBSERVATION_TIME);
        if (variable != null) {
            final Date creationDate = getCreationDate(recordNo, variable);
            observation.setTime(creationDate);
        }
    }

    private VariableDescriptor createVariableDescriptor(final Variable variable, final DataFile dataFile) {
        final VariableDescriptor variableDescriptor = IoUtil.createVariableDescriptor(variable, sensor);
        variableDescriptor.setSensor(dataFile.getSensor());
        return variableDescriptor;
    }
}
