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
import org.esa.cci.sst.util.PgUtil;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads records from an METOP MD NetCDF input file and creates Observations.
 * Defines the variables to access in the NetCDF files and implements the conversion
 * to a "reference observation" with a single point as coordinate and to a common
 * observation with a sub-scene polygon as coordinate.
 *
 * @author Martin Boettcher
 */
class MetopIOHandler extends MdIOHandler {

    private static final int LAT_LON_FILL_VALUE = -32768;

    protected int rowCount;
    protected int colCount;

    MetopIOHandler(String sensorName) {
        super(sensorName, "n");
    }

    @Override
    public String getSstVariableName() {
        return "sst";
    }

    @Override
    public void init(DataFile dataFile) throws IOException {
        super.init(dataFile);
        final NetcdfFile ncFile = getNcFile();
        rowCount = ncFile.findDimension("ny").getLength();
        colCount = ncFile.findDimension("nx").getLength();
    }

    /**
     * Reads record and creates ReferenceObservation for METOP sub-scene contained in MD. This observation
     * may serve as common observation in some matchup. METOP sub-scenes contain scan lines scanned
     * from  left to right looking in flight direction.
     *
     * @param recordNo index in observation file, must be > 0 and less than numRecords
     *
     * @return Observation for METOP sub-scene
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    @Override
    public ReferenceObservation readObservation(int recordNo) throws IOException {
        final int x = colCount / 2;
        final int y = rowCount / 2;

        final ReferenceObservation observation = new ReferenceObservation();
        observation.setName(getString("msr_id", recordNo));
        observation.setSensor(getSensorName());
        observation.setLocation(new PGgeometry(new Polygon(new LinearRing[]{new LinearRing(getPoints(recordNo))})));
        observation.setPoint(new PGgeometry(newPoint(getLon(recordNo, y, x), getLat(recordNo, y, x))));
        observation.setTime(TimeUtil.secondsSince1981ToDate(
                getDouble("msr_time", recordNo) + getDouble("dtime", recordNo, y)));
        observation.setDatafile(getDataFile());
        observation.setRecordNo(recordNo);
        observation.setClearSky(getShort(getSstVariableName(), recordNo, y, x) != getSstFillValue());

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

    private Point[] getPoints(int recordNo) throws IOException {
        final List<Point> pointList = new ArrayList<Point>(9);

        int lon = getLon(recordNo, 0, 0);
        int lat = getLat(recordNo, 0, 0);
        if (isValid(lon, lat)) {
            pointList.add(newPoint(lon, lat));
        } else {
            for (int k = 0; k < colCount / 2; k++) {
                lon = getLon(recordNo, 0, k);
                lat = getLat(recordNo, 0, k);
                if (isValid(lon, lat)) {
                    pointList.add(newPoint(lon, lat));
                    break;
                }
            }
            for (int i = 0; i < rowCount / 2; i++) {
                lon = getLon(recordNo, i, 0);
                lat = getLat(recordNo, i, 0);
                if (isValid(lon, lat)) {
                    pointList.add(newPoint(lon, lat));
                    break;
                }
            }
        }

        lon = getLon(recordNo, rowCount - 1, 0);
        lat = getLat(recordNo, rowCount - 1, 0);
        if (isValid(lon, lat)) {
            pointList.add(newPoint(lon, lat));
        } else {
            for (int i = rowCount - 1; i >= rowCount / 2; i--) {
                lon = getLon(recordNo, i, 0);
                lat = getLat(recordNo, i, 0);
                if (isValid(lon, lat)) {
                    pointList.add(newPoint(lon, lat));
                    break;
                }
            }
            for (int k = 0; k < colCount / 2; k++) {
                lon = getLon(recordNo, rowCount - 1, k);
                lat = getLat(recordNo, rowCount - 1, k);
                if (isValid(lon, lat)) {
                    pointList.add(newPoint(lon, lat));
                    break;
                }
            }
        }

        lon = getLon(recordNo, rowCount - 1, colCount - 1);
        lat = getLat(recordNo, rowCount - 1, colCount - 1);
        if (isValid(lon, lat)) {
            pointList.add(newPoint(lon, lat));
        } else {
            for (int k = colCount - 1; k >= colCount / 2; k--) {
                lon = getLon(recordNo, rowCount - 1, k);
                lat = getLat(recordNo, rowCount - 1, k);
                if (isValid(lon, lat)) {
                    pointList.add(newPoint(lon, lat));
                    break;
                }
            }
            for (int i = rowCount - 1; i >= rowCount / 2; i--) {
                lon = getLon(recordNo, i, colCount - 1);
                lat = getLat(recordNo, i, colCount - 1);
                if (isValid(lon, lat)) {
                    pointList.add(newPoint(lon, lat));
                    break;
                }
            }
        }

        lon = getLon(recordNo, 0, colCount - 1);
        lat = getLat(recordNo, 0, colCount - 1);
        if (isValid(lon, lat)) {
            pointList.add(newPoint(lon, lat));
        } else {
            for (int i = 0; i < rowCount / 2; i++) {
                lon = getLon(recordNo, i, colCount - 1);
                lat = getLat(recordNo, i, colCount - 1);
                if (isValid(lon, lat)) {
                    pointList.add(newPoint(lon, lat));
                    break;
                }
            }
            for (int k = colCount - 1; k >= colCount / 2; k--) {
                lon = getLon(recordNo, 0, k);
                lat = getLat(recordNo, 0, k);
                if (isValid(lon, lat)) {
                    pointList.add(newPoint(lon, lat));
                    break;
                }
            }
        }
        if (pointList.size() < 3) {
            throw new IllegalArgumentException(
                    String.format("only %d points in polygon of record %d", pointList.size(), recordNo));
        }
        pointList.add(pointList.get(0));
        if (PgUtil.isClockwise(pointList)) {
            Collections.reverse(pointList);
        }

        return pointList.toArray(new Point[pointList.size()]);
    }

    private int getLat(int recordNo, int y, int x) throws IOException {
        return getShort("lat", recordNo, y, x);
    }

    private int getLon(int recordNo, int y, int x) throws IOException {
        return getShort("lon", recordNo, y, x);
    }

    private static boolean isValid(int lon, int lat) {
        return lon != LAT_LON_FILL_VALUE && lat != LAT_LON_FILL_VALUE;
    }

    private static Point newPoint(int lon, int lat) {
        return new Point(0.01 * lon, 0.01 * lat);
    }
}
