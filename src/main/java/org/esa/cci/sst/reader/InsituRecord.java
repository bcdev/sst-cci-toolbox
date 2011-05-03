package org.esa.cci.sst.reader;

/**
 * A common set of in-situ data that are included in MD files.
 *
 * @author Ralf Quast
 */
public class InsituRecord {

    final Number[] values = new Number[InsituVariable.values().length];

    InsituRecord() {
    }

    /**
     * Returns the value of a given in-situ variable.
     *
     * @param v The in-situ variable.
     *
     * @return the value of the given in-situ variable.
     */
    public final Number getValue(InsituVariable v) {
        return values[v.ordinal()];
    }

    /**
     * Set the value of a given in-situ variable.
     *
     * @param v     The in-situ variable.
     * @param value The value of the in-situ variable.
     */
    public void setValue(InsituVariable v, Number value) {
        values[v.ordinal()] = value;
    }
}
