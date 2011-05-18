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

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
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

    private NetcdfFile netcdfFile;
    private DataFile datafile;

    NetcdfIOHandler(String sensorName) {
        this.sensorName = sensorName;
    }

    /**
     * Opens NetCDF file. May be overridden to initialise additional
     * variables.
     *
     * @param datafile data file entry to be referenced in each observation created by reader
     *
     * @throws java.io.IOException if file access fails
     */
    @Override
    public void init(DataFile datafile) throws IOException {
        if (netcdfFile != null) {
            close();
        }
        final String path = datafile.getPath();
        this.netcdfFile = NetcdfFile.open(path);
        this.datafile = datafile;
    }

    @Override
    public Item getColumn(String role) {
        final Variable variable = netcdfFile.findVariable(NetcdfFile.escapeName(role));
        if (variable != null) {
            createColumn(variable);
        }
        return null;
    }

    @Override
    public Item[] getColumns() {
        final ArrayList<Item> columnList = new ArrayList<Item>();
        for (final Variable variable : netcdfFile.getVariables()) {
            final Item column = createColumn(variable);
            columnList.add(column);
        }
        return columnList.toArray(new Item[columnList.size()]);
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
        if (netcdfFile != null) {
            try {
                netcdfFile.close();
            } catch (IOException ignore) {
                // ok
            }
        }
        datafile = null;
    }

    @Override
    public DataFile getDatafile() {
        return datafile;
    }

    protected String getSensorName() {
        return sensorName;
    }

    protected NetcdfFile getNetcdfFile() {
        return netcdfFile;
    }

    private Item createColumn(final Variable variable) {
        return IoUtil.createColumnBuilder(variable, sensorName).sensor(datafile.getSensor()).build();
    }
}
