package org.esa.cci.sst.data;

/**
 * Describes a variable.
 *
 * @author Ralf Quast.
 */
public interface Descriptor {

    String getName();

    String getType();

    String getDimensions();

    String getUnit();

    Number getAddOffset();

    Number getScaleFactor();

    Number getFillValue();

    Number getValidMin();

    Number getValidMax();

    String getStandardName();

    String getLongName();

    String getRole();

    Sensor getSensor();
}
