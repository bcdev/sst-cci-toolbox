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
import org.esa.cci.sst.data.VariableDescriptor;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Abstract base class for all netcdf-observation readers. Provides methods to to open the file and access its
 * variables.
 *
 * @author Thomas Storm
 */
public abstract class NetcdfIOHandler implements IOHandler {

    private final HashMap<String, String> dimensionRoleMap = new HashMap<String, String>(7);
    private final String sensorName;

    private NetcdfFile ncFile;
    private DataFile dataFile;

    public NetcdfIOHandler(String sensorName) {
        this.sensorName = sensorName;
        dimensionRoleMap.put("n", "match_up");
        dimensionRoleMap.put("nx", "nj");
        dimensionRoleMap.put("ny", "ni");
        dimensionRoleMap.put("len_id", "length");
        dimensionRoleMap.put("len_filename", "length");
        dimensionRoleMap.put("cs_length", "length");
        dimensionRoleMap.put("ui_length", "length");
        dimensionRoleMap.put("length", "length");
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
        this.dataFile = dataFile;
        final String path = dataFile.getPath();
        if (!NetcdfFile.canOpen(path)) {
            throw new IOException("Cannot open file '" + path + "'.");
        }
        ncFile = NetcdfFile.open(path);
    }

    @Override
    public VariableDescriptor[] getVariableDescriptors() throws IOException {
        final ArrayList<VariableDescriptor> variableDescriptorList = new ArrayList<VariableDescriptor>();
        for (final Variable variable : ncFile.getVariables()) {
            final VariableDescriptor variableDescriptor = new VariableDescriptor();
            variableDescriptor.setName(String.format("%s.%s", sensorName, variable.getName()));
            variableDescriptor.setType(variable.getDataType().name());
            final String dimensions = variable.getDimensionsString();
            variableDescriptor.setDimensions(dimensions);
            final String dimensionRoles = getDimensionRoles(dimensions);
            variableDescriptor.setDimensionRoles(dimensionRoles);
            for (final Attribute attr : variable.getAttributes()) {
                if ("add_offset".equals(attr.getName())) {
                    variableDescriptor.setAddOffset(attr.getNumericValue());
                }
                if ("scale_factor".equals(attr.getName())) {
                    variableDescriptor.setScaleFactor(attr.getNumericValue());
                }
                if ("_FillValue".equals(attr.getName())) {
                    variableDescriptor.setFillValue(attr.getNumericValue());
                }
                if ("valid_min".equals(attr.getName())) {
                    variableDescriptor.setValidMin(attr.getNumericValue());
                }
                if ("valid_max".equals(attr.getName())) {
                    variableDescriptor.setValidMax(attr.getNumericValue());
                }
                if ("long_name".equals(attr.getName())) {
                    variableDescriptor.setLongName(attr.getStringValue());
                }
                if ("standard_name".equals(attr.getName())) {
                    variableDescriptor.setStandardName(attr.getStringValue());
                }
            }
            variableDescriptor.setDataSchema(dataFile.getDataSchema());
            final String units = variable.getUnitsString();
            if (units != null && !units.isEmpty()) {
                variableDescriptor.setUnits(units);
            }
            variableDescriptorList.add(variableDescriptor);
        }
        return variableDescriptorList.toArray(new VariableDescriptor[variableDescriptorList.size()]);
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
    }

    protected String getSensorName() {
        return sensorName;
    }

    protected NetcdfFile getNcFile() {
        return ncFile;
    }

    protected DataFile getDataFile() {
        return dataFile;
    }

    private String getDimensionRoles(String dimensionsString) {
        final StringBuilder sb = new StringBuilder();
        for (final String dimensionString : dimensionsString.split(" ")) {
            if (sb.length() != 0) {
                sb.append(" ");
            }
            final String dimensionRole = dimensionRoleMap.get(dimensionString);
            if (dimensionRole == null) {
                sb.append(dimensionString);
            } else {
                sb.append(dimensionRole);
            }
        }
        return sb.toString();
    }
}
