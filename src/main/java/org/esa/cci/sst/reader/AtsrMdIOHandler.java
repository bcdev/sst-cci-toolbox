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

import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import org.postgis.Point;

import java.io.IOException;
import java.util.Date;

/**
 * Reads records from an (A)ATSR MD NetCDF input file and creates Observations.
 * Defines the variables to access in the NetCDF files and implements the conversion
 * to a "reference observation" with a single point as coordinate. (A)ATSR MDs only
 * serve as reference observation. They never provide a coverage to serve as "common
 * observation" that matches a reference observation.
 *
 * @author Martin Boettcher
 */
class AtsrMdIOHandler extends MdIOHandler {

    AtsrMdIOHandler(String sensorName) {
        super(sensorName);
    }

    /**
     * Reads record and creates ReferenceObservation for (A)ATSR pixel contained in MD. This observation
     * may serve as reference observation in some matchup.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     *
     * @return Observation for (A)ATSR pixel
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    @Override
    public ReferenceObservation readObservation(int recordNo) throws IOException {
        final PGgeometry location = new PGgeometry(new Point(getFloat("atsr.longitude", recordNo),
                                                             getFloat("atsr.latitude", recordNo)));
        final ReferenceObservation observation = new ReferenceObservation();
        observation.setCallsign(getString("insitu.callsign", recordNo));
        observation.setDataset(getByte("insitu.dataset", recordNo));
        observation.setReferenceFlag(getByte("insitu.reference_flag", recordNo));
        observation.setSensor(getDatafile().getSensor().getName());
        observation.setPoint(location);
        observation.setLocation(location);
        observation.setTime(dateOf(getDouble("atsr.time.julian", recordNo)));
        observation.setDatafile(getDatafile());
        observation.setRecordNo(recordNo);
        return observation;
    }

    @Override
    public InsituRecord readInsituRecord(int recordNo) throws IOException {
        final InsituRecord insituRecord = new InsituRecord();
        final double julianDate = getDouble("insitu.time.julian", recordNo);
        final double time = TimeUtil.julianDateToSecondsSinceEpoch(julianDate);
        insituRecord.setValue(InsituVariable.TIME, time);
        insituRecord.setValue(InsituVariable.LAT, getFloat("insitu.latitude", recordNo));
        insituRecord.setValue(InsituVariable.LAT, getFloat("insitu.longitude", recordNo));
        final Number sst = getNumberScaled("insitu.sea_surface_temperature", recordNo);
        insituRecord.setValue(InsituVariable.SST, sst.floatValue() + 273.15f);

        return insituRecord;
    }

    private static Date dateOf(double julianDate) {
        return TimeUtil.julianDateToDate(julianDate);
    }
}
