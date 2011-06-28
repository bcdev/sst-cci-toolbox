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

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

/**
 * Allows to read from files output by ARC.
 *
 * @author Thomas Storm
 */
public class MmdArcReader extends AbstractMmdReader {

    public MmdArcReader(final DataFile dataFile, final NetcdfFile arcfile, final String sensor) {
        super(dataFile, arcfile, sensor);
    }

    @Override
    public Observation readObservation(final int recordNo) throws IOException {
        validateRecordNumber(recordNo);
        final Observation observation = new Observation();
        observation.setRecordNo(recordNo);
        setupObservation(observation);
        return observation;
    }
}
