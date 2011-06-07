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

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.util.PixelLocator;
import org.esa.beam.util.QuadTreePixelLocator;
import org.esa.beam.util.VariableSampleSource;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;

import java.awt.geom.Point2D;
import java.io.IOException;

/**
 * Reader for AVHRR-based matchup datasets.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree"})
class AvhrrMdReader extends MdReader {

    private PixelLocator locator;

    protected AvhrrMdReader(String sensorName) {
        super(sensorName);
        final Array lonArray;
        final Array latArray;
        try {
            lonArray = getVariable("avhrr.longitude").read();
            latArray = getVariable("avhrr.latitude").read();
        } catch (IOException e) {
            throw new ToolException("Unable to read geographic information.", e, ToolException.TOOL_IO_ERROR);
        }
        locator = new QuadTreePixelLocator(new VariableSampleSource(lonArray), new VariableSampleSource(latArray));
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


    @Override
    public int getDTime(int recordNo, int scanLine) throws IOException {
        return 0;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos) throws IOException {
        final PixelPos pixelPos = new PixelPos();
        final Point2D.Double foundPoint = new Point2D.Double();
        locator.getPixelLocation(geoPos.lon, geoPos.lat, foundPoint);
        pixelPos.setLocation(foundPoint);
        return pixelPos;
    }

    @Override
    public int getTime(int recordNo, int scanLine) throws IOException {
        final double time = getDouble("avhrr.time", recordNo);
        return (int) TimeUtil.secondsSince1981ToDate(time).getTime();
    }

}
