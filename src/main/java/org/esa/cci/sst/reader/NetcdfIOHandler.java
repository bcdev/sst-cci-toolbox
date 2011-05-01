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
import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.util.IoUtil;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Abstract base class for all netcdf-observation readers. Provides methods to to open the file and access its
 * variables.
 *
 * @author Thomas Storm
 */
abstract class NetcdfIOHandler implements IOHandler {

    private final String sensorName;

    private NetcdfFile ncFile;
    private DataFile dataFile;

    NetcdfIOHandler(String sensorName) {
        this.sensorName = sensorName;
    }

    /**
     * Opens NetCDF file. May be overridden to initialise additional
     * variables.
     *
     * @param dataFile data file entry to be referenced in each observation created by reader
     *
     * @throws java.io.IOException if file access fails
     */
    @Override
    public void init(DataFile dataFile) throws IOException {
        if (ncFile != null) {
            close();
        }
        final String path = dataFile.getPath();
        ncFile = NetcdfFile.open(path);
        this.dataFile = dataFile;
    }

    @Override
    public Descriptor[] getVariableDescriptors() throws IOException {
        final ArrayList<Descriptor> variableDescriptorList = new ArrayList<Descriptor>();
        for (final Variable variable : ncFile.getVariables()) {
            final Descriptor variableDescriptor = createVariableDescriptor(variable, sensorName, dataFile);
            variableDescriptorList.add(variableDescriptor);
        }
        return variableDescriptorList.toArray(new Descriptor[variableDescriptorList.size()]);
    }

    @Override
    public InsituRecord readInsituRecord(int recordNo) throws IOException, OperationNotSupportedException {
        throw new OperationNotSupportedException();
    }

    /**
     * Closes NetCDF file.
     */
    @Override
    public void close() {
        if (ncFile != null) {
            try {
                ncFile.close();
            } catch (IOException ignore) {
                // ok
            }
        }
        dataFile = null;
    }

    @Override
    public DataFile getDataFile() {
        return dataFile;
    }

    protected String getSensorName() {
        return sensorName;
    }

    protected NetcdfFile getNcFile() {
        return ncFile;
    }

    private static Descriptor createVariableDescriptor(final Variable variable, final String sensorName,
                                                       final DataFile dataFile) {
        final VariableDescriptor descriptor = IoUtil.createVariableDescriptor(variable, sensorName);
        descriptor.setSensor(dataFile.getSensor());
        return descriptor;
    }

}
