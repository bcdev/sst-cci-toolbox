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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader for AVHRR-based matchup datasets.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree"})
class AvhrrMdReader extends MdReader implements InsituSource {

    protected AvhrrMdReader(String sensorName) {
        super(sensorName);
    }

    @Override
    public Observation readObservation(int recordNo) throws IOException {
        ReferenceObservation observation = new ReferenceObservation();
        // decision to use insitu lat/lon as reference since GAC coordinates will go through ARC1
        // correction [Boe, 2011-08-04 after email discussion with GC]
        final PGgeometry location = new PGgeometry(new Point(getFloat("insitu.longitude", recordNo),
                                                             getFloat("insitu.latitude", recordNo)));
        observation.setName(getString("insitu.callsign", recordNo));
        observation.setDataset(getByte("insitu.dataset", recordNo));
        observation.setReferenceFlag((byte) 4);
        observation.setSensor(getDatafile().getSensor().getName());
        observation.setPoint(location);
        observation.setLocation(location);
        observation.setTime(TimeUtil.secondsSince1981ToDate(getDouble("avhrr.time", recordNo)));
        observation.setTimeRadius(Math.abs(getDouble("avhrr.time", recordNo) - getInt("insitu.time", recordNo)));
        observation.setDatafile(getDatafile());
        observation.setRecordNo(recordNo);

        return observation;
    }


    @Override
    public double getDTime(int recordNo, int scanLine) throws IOException {
        return 0;
    }

    @Override
    public long getTime(int recordNo, int scanLine) throws IOException {
        final double time = getDouble("avhrr.time", recordNo);
        return TimeUtil.secondsSince1981ToDate(time).getTime();
    }

    @Override
    public InsituSource getInsituSource() {
        return this;
    }

    @Override
    public List<SamplingPoint> readSamplingPoints() {
        return new ArrayList<>();
    }

    @Override
    public GeoCoding getGeoCoding(int recordNo) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public int getScanLineCount() {
        return 0;
    }

    @Override
    public int getElementCount() {
        return 0;
    }

    @Override
    public final double readInsituLon(int recordNo) throws IOException {
        return getFloat("insitu.longitude", recordNo);
    }

    @Override
    public final double readInsituLat(int recordNo) throws IOException {
        return getFloat("insitu.latitude", recordNo);
    }

    @Override
    public final double readInsituTime(int recordNo) throws IOException {
        return TimeUtil.secondsSince1981ToSecondsSinceEpoch(getInt("insitu.time", recordNo));
    }

    @Override
    public final double readInsituSst(int recordNo) throws IOException {
        final Variable sstVariable = getVariable("insitu.sea_surface_temperature");
        final String unit = sstVariable.findAttribute("units").getStringValue();
        if("celcius".equals(unit)) {
            return getNumberScaled("insitu.sea_surface_temperature", recordNo).doubleValue() + 273.15;
        } else {
            return getNumberScaled("insitu.sea_surface_temperature", recordNo).doubleValue();
        }
    }
}
