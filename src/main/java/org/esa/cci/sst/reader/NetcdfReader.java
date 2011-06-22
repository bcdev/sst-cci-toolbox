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
import org.esa.cci.sst.util.IoUtil;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Abstract base class for all netcdf-observation readers. Provides methods to to open the file and access its
 * variables.
 *
 * @author Thomas Storm
 */
abstract class NetcdfReader implements Reader {

    private final String sensorName;
    private final HashMap<String, Variable> variableMap = new HashMap<String, Variable>();

    private DataFile datafile;
    private NetcdfFile netcdfFile;

    protected NetcdfReader(String sensorName) {
        this.sensorName = sensorName;
    }

    /**
     * Opens a NetCDF file. May be overridden to initialise additional
     * variables.
     *
     * @param datafile data file entry to be referenced in each observation created by reader
     *
     * @throws IOException if file access fails.
     */
    @Override
    public void init(DataFile datafile) throws IOException {
        Assert.state(netcdfFile == null, "netcdfFile != null");

        this.datafile = datafile;
        this.netcdfFile = NetcdfFile.open(datafile.getPath());

        final List<Variable> variables = netcdfFile.getVariables();
        for (final Variable variable : variables) {
            if (variable.getRank() > 0) {
                variableMap.put(variable.getName(), variable);
            }
        }
    }

    @Override
    public final Item getColumn(String role) {
        final Variable variable = variableMap.get(role);
        if (variable != null) {
            return createColumn(variable);
        }
        return null;
    }

    @Override
    public final Item[] getColumns() {
        final ArrayList<Item> columnList = new ArrayList<Item>();
        for (final Variable variable : variableMap.values()) {
            final Item column = createColumn(variable);
            columnList.add(column);
        }
        return columnList.toArray(new Item[columnList.size()]);
    }

    /**
     * Closes the NetCDF file.
     */
    @Override
    public void close() {
        variableMap.clear();
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
    public final DataFile getDatafile() {
        return datafile;
    }

    @Override
    public int getLineSkip() {
        return 0;
    }

    public final String getSensorName() {
        return sensorName;
    }

    public final NetcdfFile getNetcdfFile() {
        return netcdfFile;
    }

    public final Collection<Variable> getVariables() {
        return Collections.unmodifiableCollection(variableMap.values());
    }

    public final Variable getVariable(String name) {
        return variableMap.get(name);
    }

    protected static Number getAttribute(Variable variable, String attributeName, Number defaultValue) {
        final Attribute attribute = variable.findAttribute(attributeName);
        if (attribute == null) {
            return defaultValue;
        }
        return attribute.getNumericValue();
    }

    private Item createColumn(final Variable variable) {
        return IoUtil.createColumnBuilder(variable, sensorName).sensor(datafile.getSensor()).build();
    }
}
