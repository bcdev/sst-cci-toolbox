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

import org.esa.beam.dataio.cci.sst.NcAaiProductReaderPlugIn;
import org.esa.cci.sst.data.GlobalObservation;

import java.io.IOException;

/**
 * A product IO handler that produces {@link GlobalObservation}s. This handler
 * is used for Aerosol AAI input data only.
 *
 * @author Ralf Quast
 */
class AaiProductHandler extends AbstractProductHandler {

    AaiProductHandler(String sensorName) {
        super(sensorName, NcAaiProductReaderPlugIn.FORMAT_NAME);
    }

    @Override
    public final GlobalObservation readObservation(int recordNo) throws IOException {
        final GlobalObservation globalObservation = new GlobalObservation();
        globalObservation.setTime(getCenterTimeAsDate());
        globalObservation.setDatafile(getDatafile());
        globalObservation.setRecordNo(0);
        globalObservation.setSensor(getSensorName());

        return globalObservation;
    }
}
