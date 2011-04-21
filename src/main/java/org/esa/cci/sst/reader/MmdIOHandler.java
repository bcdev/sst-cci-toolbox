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
import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.tools.MmsTool;
import org.postgis.PGgeometry;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;

/**
 * IOHandler for reading from and writing to an mmd file.
 *
 * @author Thomas Storm
 */
public class MmdIOHandler implements IOHandler {

    static final String VARIABLE_NAME_MATCHUP = "matchup_id";

    private NetcdfFile mmd;
    private final MmsTool tool;
    private final String sensor;
    private final String schemaName;
    private Variable matchupIds;
    private MmdReader reader;
    private MmdWriter writer;

    public MmdIOHandler(final MmsTool tool, final String sensor, final String schemaName) {
        this.tool = tool;
        this.sensor = sensor;
        this.schemaName = schemaName;
    }

    @Override
    public void init(final DataFile dataFile) throws IOException {
        final String fileLocation = dataFile.getPath();
        validateFileLocation(fileLocation);
        mmd = NetcdfFile.open(fileLocation);
        matchupIds = mmd.findVariable(NetcdfFile.escapeName(VARIABLE_NAME_MATCHUP));
        reader = new MmdReader(this, tool.getPersistenceManager(), mmd, sensor, schemaName);
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
    public VariableDescriptor[] getVariableDescriptors() throws IOException {
        validateDelegate(reader);
        return reader.getVariableDescriptors();
    }

    @Override
    public InsituRecord readInsituRecord(int recordNo) {
        return null;
    }

    @Override
    public void write(final NetcdfFileWriteable targetFile, final Observation sourceObservation,
                      final String sourceVariableName, final String targetVariableName, final int targetRecordNumber,
                      final PGgeometry refPoint, final Date refTime) throws IOException {
        validateDelegate(writer);
        writer.write(targetFile, sourceObservation, sourceVariableName, targetVariableName, targetRecordNumber);
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

    public int getMatchupId(final int recordNo) throws IOException {
        final Array matchupId = readData(matchupIds, new int[]{recordNo}, new int[]{1});
        return matchupId.getInt(0);
    }

    Array getData(final String sourceVariableName, final int recordNo) throws IOException {
        final Variable variable = mmd.findVariable(NetcdfFile.escapeName(sourceVariableName));
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
        try {
            return variable.read(origin, shape);
        } catch (InvalidRangeException e) {
            throw new IOException(
                    MessageFormat.format("Unable to read from file ''{0}''.", mmd.getLocation()), e);
        }
    }

    String getProperty(final String key) {
        return tool.getConfiguration().getProperty(key);
    }

    void handleError(final Throwable t, final String message, final int exitCode) {
        tool.getErrorHandler().handleError(t, message, exitCode);
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
