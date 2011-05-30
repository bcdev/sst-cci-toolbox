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

import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import org.postgis.Point;

import java.io.IOException;

/**
 * @author Thomas Storm
 */
class AvhrrMdReader extends MdReader {

    protected AvhrrMdReader(String sensorName) {
        super(sensorName);
    }

    @Override
    public Observation readObservation(int recordNo) throws IOException {
        ReferenceObservation observation = new ReferenceObservation();
        final PGgeometry location = new PGgeometry(new Point(getFloat("avhrr.longitude", recordNo),
                                                             getFloat("avhrr.latitude", recordNo)));
        observation.setCallsign(getString("insitu.callsign", recordNo));
        observation.setDataset(getByte("insitu.dataset", recordNo));
        observation.setReferenceFlag((byte) 4);
        observation.setSensor(getDatafile().getSensor().getName());
        observation.setPoint(location);
        observation.setLocation(location);
        observation.setTime(TimeUtil.secondsSince1981ToDate(getDouble("avhrr.time", recordNo)));
        observation.setDatafile(getDatafile());
        observation.setRecordNo(recordNo);

        return observation;
    }
}
