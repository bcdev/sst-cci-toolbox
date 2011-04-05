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
import org.esa.cci.sst.util.DataUtil;
import org.postgis.PGgeometry;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Reader for reading from an mmd file. Does not implement the <code>write</code>-method.
 *
 * @author Thomas Storm
 */
public class MmdReader implements IOHandler {

    private static final String RECORD_DIMENSION_NAME = "record";
    private static final String VARIABLE_NAME_SEA_SURFACE_TEMPERATURE = "atsr.3.sea_surface_temperature.ARC.N2";
    private NetcdfFile mmd;

    @Override
    public void init(final DataFile dataFile) throws IOException {
        final String fileLocation = dataFile.getPath();
        validateFileLocation(fileLocation);
        mmd = NetcdfFile.open(fileLocation);
    }

    private void validateFileLocation(final String fileLocation) throws IOException {
        if (!NetcdfFile.canOpen(fileLocation)) {
            throw new IOException("File '" + fileLocation + "' cannot be opened.");
        }
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
        if(getNumRecords() < recordNo) {
            throw new IllegalArgumentException(MessageFormat.format("Invalid record number: ''{0}''.", recordNo));
        }
        final RelatedObservation observation = new RelatedObservation();
//        observation.setLocation();
        observation.setDatafile(createDatafile());
//        observation.setId();
//        observation.setName();
        observation.setRecordNo(recordNo);
        observation.setSensor("ARC");   // todo - ts 4Apr2011 - ok?
        observation.setTime(getCreationDate());
        return observation;
    }

    @Override
    public VariableDescriptor[] getVariableDescriptors() throws IOException {
        throw new IllegalStateException("not needed, therefore not implemented");
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

    Date getCreationDate() throws IOException {
        final Attribute creationDateAttribute = mmd.findGlobalAttributeIgnoreCase("date_created");
        final String creationDateAttributeStringValue = creationDateAttribute.getStringValue();
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        try {
            return simpleDateFormat.parse(creationDateAttributeStringValue);
        } catch (ParseException e) {
            throw new IOException("Unable to read creation date of file '" + mmd.getLocation() + "'.", e);
        }
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
}
