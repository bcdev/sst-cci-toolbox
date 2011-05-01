package org.esa.cci.sst.data;

/**
 * Describes a variable.
 *
 * @author Ralf Quast.
 */
public interface Descriptor {

    /**
     * Returns the name of the variable.
     *
     * @return the variable name.
     */
    String getName();

    /**
     * Returns the type of the variable.
     *
     * @return the variable type.
     */
    String getType();

    /**
     * Returns the dimensions of the variable.
     *
     * @return the dimensions (space-separated list of dimension names).
     */
    String getDimensions();

    /**
     * Returns the physical unit of the variable.
     *
     * @return the physical unit.
     */
    String getUnit();

    /**
     * Returns the add-offset of the variable.
     *
     * @return the add-offset.
     */
    Number getAddOffset();

    /**
     * Returns the scale factor of the variable.
     *
     * @return the scale factor.
     */
    Number getScaleFactor();

    /**
     * Returns the fill value used by the variable.
     *
     * @return the fill value.
     */
    Number getFillValue();

    /**
     * Returns the minimum valid value of the variable.
     *
     * @return the minimum valid value.
     */
    Number getValidMin();

    /**
     * Returns the maximum valid value of the variable.
     *
     * @return the maximum valid value.
     */
    Number getValidMax();

    /**
     * Returns the standard name of the variable.
     *
     * @return the standard name.
     */
    String getStandardName();

    /**
     * Returns the long name of the variable.
     *
     * @return the long name.
     */
    String getLongName();

    /**
     * Returns the role of the variable.
     *
     * @return the role.
     */
    String getRole();

    /**
     * Returns the sensor associated with the variable.
     *
     * @return the sensor.
     */
    Sensor getSensor();
}
