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

import org.esa.cci.sst.data.Observation;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * @author Thomas Storm
 */
class MmdWriter {

    private final MmdIOHandler mmdIOHandler;

    MmdWriter(final MmdIOHandler mmdIOHandler) {
        this.mmdIOHandler = mmdIOHandler;
    }

    void write(final NetcdfFileWriteable targetFile, final Observation sourceObservation,
               final String sourceVariableName, final String targetVariableName,
               final int targetRecordNumber) throws IOException {

        final Variable targetVariable = targetFile.findVariable(NetcdfFile.escapeName(targetVariableName));
        final int[] origin = new int[targetVariable.getRank()];
        origin[0] = targetRecordNumber;

        try {
            final Array variableData = mmdIOHandler.getData(sourceVariableName, sourceObservation.getRecordNo());
            targetFile.write(NetcdfFile.escapeName(targetVariableName), origin, variableData);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }

}
