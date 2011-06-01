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

package org.esa.cci.sst.rules;

import org.esa.cci.sst.reader.ExtractDefinition;

import java.util.Date;

/**
 * A default implementation of ${@link ExtractDefinition}, which provides only a record number and
 * a shape for reading exactly one value from a one-dimensional variable.
 *
 * @author Thomas Storm
 */
class OneDimOneValue implements ExtractDefinition {

    private final int recordNo;

    OneDimOneValue(int recordNo) {
        this.recordNo = recordNo;
    }

    @Override
    public double getLat() {
        return Double.NaN;
    }

    @Override
    public double getLon() {
        return Double.NaN;
    }

    @Override
    public int getRecordNo() {
        return recordNo;
    }

    @Override
    public int[] getShape() {
        return new int[]{1};
    }

    @Override
    public Date getDate() {
        return null;
    }
}
