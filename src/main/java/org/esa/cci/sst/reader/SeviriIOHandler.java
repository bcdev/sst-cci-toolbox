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
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

/**
 * Reads records from an SEVIRI MD NetCDF input file and creates Observations.
 * Defines the variables to access in the NetCDF files and implements the conversion
 * to a common observation with a sub-scene polygon as coordinate.
 *
 * @author Martin Boettcher
 */
class SeviriIOHandler extends MdIOHandler {

    protected int noOfLines;
    protected int noOfColumns;

    SeviriIOHandler(String sensorName) {
        super(sensorName, "n");
    }

    @Override
    public String getSstVariableName() {
        return "sst";
    }

    @Override
    public void init(DataFile datafile) throws IOException {
        super.init(datafile);
        final NetcdfFile ncFile = getNetcdfFile();
        noOfLines = ncFile.findDimension("ny").getLength();
        noOfColumns = ncFile.findDimension("nx").getLength();
    }

    /**
     * Reads record and creates ReferenceObservation for SEVIRI sub-scene contained in MD. This observation
     * may serve as common observation in some matchup. SEVIRI sub-scenes contain scan lines scanned
     * from right to left looking from first to last scan line.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     *
     * @return Observation for SEVIRI sub-scene
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    @Override
    public ReferenceObservation readObservation(int recordNo) throws IOException {
        final int line = noOfLines / 2;
        final int column = noOfColumns / 2;

        final ReferenceObservation observation = new ReferenceObservation();
        observation.setCallsign(getString("msr_id", recordNo));
        observation.setDataset(getByte("msr_type", recordNo));
        observation.setReferenceFlag((byte) 4);
        observation.setSensor(getSensorName());
        observation.setLocation(new PGgeometry(new Polygon(new LinearRing[]{
                new LinearRing(new Point[]{
                        new Point(coordinateOf(getInt("lon", recordNo, 0, 0)),
                                  coordinateOf(getInt("lat", recordNo, 0, 0))),
                        new Point(coordinateOf(getInt("lon", recordNo, noOfLines - 1, 0)),
                                  coordinateOf(getInt("lat", recordNo, noOfLines - 1, 0))),
                        new Point(coordinateOf(getInt("lon", recordNo, noOfLines - 1, noOfColumns - 1)),
                                  coordinateOf(getInt("lat", recordNo, noOfLines - 1, noOfColumns - 1))),
                        new Point(coordinateOf(getInt("lon", recordNo, 0, noOfColumns - 1)),
                                  coordinateOf(getInt("lat", recordNo, 0, noOfColumns - 1))),
                        new Point(coordinateOf(getInt("lon", recordNo, 0, 0)),
                                  coordinateOf(getInt("lat", recordNo, 0, 0)))
                })
        })));
        observation.setPoint(new PGgeometry(new Point(coordinateOf(getInt("lon", recordNo, line, column)),
                                                      coordinateOf(getInt("lat", recordNo, line, column)))));
        observation.setTime(TimeUtil.secondsSince1981ToDate(
                getDouble("time", recordNo) + getDouble("dtime", recordNo, line, column)));
        observation.setDatafile(getDatafile());
        observation.setRecordNo(recordNo);
        observation.setClearSky(getShort(getSstVariableName(), recordNo, line, column) != getSstFillValue());
        return observation;
    }

    @Override
    public InsituRecord readInsituRecord(int recordNo) throws IOException {
        final InsituRecord insituRecord = new InsituRecord();
        final double secondsSince1981 = getDouble("msr_time", recordNo);
        final double time = TimeUtil.secondsSince1981ToSecondsSinceEpoch(secondsSince1981);
        insituRecord.setValue(InsituVariable.TIME, time);
        final Number lat = getNumberScaled("msr_lat", recordNo);
        insituRecord.setValue(InsituVariable.LAT, lat.floatValue());
        final Number lon = getNumberScaled("msr_lon", recordNo);
        insituRecord.setValue(InsituVariable.LON, lon.floatValue());
        final Number sst = getNumberScaled("msr_sst", recordNo);
        insituRecord.setValue(InsituVariable.SST, sst.floatValue());

        return insituRecord;
    }

    private static float coordinateOf(int intCoordinate) {
        return intCoordinate * 0.0001f;
    }
}
