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

import ucar.ma2.DataType;
import ucar.nc2.Attribute;

/**
 * Enumeration of in-situ variables used in in-situ history.
 *
 * @author Ralf Quast.
 */
public enum InsituVariable {
    TIME("insitu.time", DataType.DOUBLE,
         new Attribute("long_name", "time of in situ measurement"),
         new Attribute("units", "seconds since 1978-01-01 00:00:00")),
    LAT("insitu.latitude", DataType.FLOAT,
        new Attribute("long_name", "in situ latitude"),
        new Attribute("units", "degrees_north")),
    LON("insitu.longitude", DataType.FLOAT,
        new Attribute("long_name", "in situ longitude"),
        new Attribute("units", "degrees_east")),
    SST("insitu.sea_surface_temperature", DataType.FLOAT,
        new Attribute("long_name", "in situ sea surface temperature"),
        new Attribute("units", "kelvin"),
        new Attribute("_FillValue", -32768.0f));

    private final String variableName;
    private final DataType dataType;
    private final Attribute[] attributes;

    private InsituVariable(String variableName, DataType dataType, Attribute... attributes) {
        this.variableName = variableName;
        this.dataType = dataType;
        this.attributes = attributes;
    }

    /**
     * Returns the name of the variable.
     *
     * @return the name of the variable.
     */
    public final String getName() {
        return variableName;
    }

    /**
     * Returns the netCDF data type of the variable.
     *
     * @return the netCDF data type of the variable.
     */
    public final DataType getDataType() {
        return dataType;
    }

    /**
     * Returns the netCDF attributes of the variable.
     *
     * @return the netCDF attributes of the variable.
     */
    public final Attribute[] getAttributes() {
        return attributes.clone();
    }
}
