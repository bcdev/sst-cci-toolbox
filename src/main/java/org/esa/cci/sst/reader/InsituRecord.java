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
