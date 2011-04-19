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
         new Attribute("long_name", "time of in-situ sst"),
         new Attribute("units", "Julian data format UTC")),
    LAT("insitu.latitude", DataType.FLOAT,
        new Attribute("long_name", "latitude"),
        new Attribute("units", "degrees_north")),
    LON("insitu.longitude", DataType.FLOAT,
        new Attribute("long_name", "longitude"),
        new Attribute("units", "degrees_east")),
    SST("insitu.sea_surface_temperature", DataType.FLOAT,
        new Attribute("long_name", "sea surface temperature"),
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
