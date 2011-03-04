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
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Variable;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

/**
 * Allows reading of observations from the in-history situ data.
 *
 * @author Thomas Storm
 */
public class InsituHistoryReader implements ObservationReader {

    @Override
    public void init(DataFile dataFileEntry) throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public int getNumRecords() {
        return 0;
    }

    @Override
    public long getTime(int recordNo) throws IOException, InvalidRangeException {
        return 0;
    }

    @Override
    public Observation readObservation(int recordNo) throws IOException, InvalidRangeException {
        return null;
    }

    @Override
    public Variable[] getVariables() throws IOException {
        return new Variable[0];
    }

    @Override
    public void write(Observation observation, Variable variable, NetcdfFileWriteable file, int matchupIndex,
                      int[] dimensionSizes) throws IOException {
    }
}
