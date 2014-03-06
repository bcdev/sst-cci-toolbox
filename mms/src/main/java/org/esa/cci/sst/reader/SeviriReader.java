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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.util.VariableSampleSource;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads records from an SEVIRI MD NetCDF input file and creates Observations.
 * Defines the variables to access in the NetCDF files and implements the conversion
 * to a common observation with a sub-scene polygon as coordinate.
 *
 * @author Martin Boettcher
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree"})
class SeviriReader extends MdReader implements InsituSource {

    protected int noOfLines;
    protected int noOfColumns;

    private int cachedRecordNo = Integer.MAX_VALUE;
    private GeoCoding cachedGeoCoding = null;

    SeviriReader(String sensorName) {
        super(sensorName);
    }

    @Override
    public void init(DataFile datafile, File archiveRoot) throws IOException {
        super.init(datafile, archiveRoot);
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
        observation.setName(getString("msr_id", recordNo));
        final byte dataset = getByte("msr_type", recordNo);
        switch (dataset) {
            case 0:
                observation.setDataset((byte) 1);
                break;
            case 1:
                observation.setDataset((byte) 0);
                break;
            default:
                observation.setDataset(dataset);
        }
        observation.setReferenceFlag(Constants.MATCHUP_REFERENCE_FLAG_UNDEFINED);
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
        observation.setTimeRadius(Math.abs(getDouble("dtime", recordNo, line, column)));
        observation.setDatafile(getDatafile());
        observation.setRecordNo(recordNo);
        return observation;
    }

    @Override
    public List<SamplingPoint> readSamplingPoints() {
        return new ArrayList<>();
    }

    @Override
    public long getTime(int recordNo, int scanLine) throws IOException {
        final double time = getDouble("time", recordNo);
        final double dtime = getDTime(recordNo, scanLine);
        return TimeUtil.secondsSince1981ToDate(time + dtime).getTime();
    }

    @Override
    public InsituSource getInsituSource() {
        return this;
    }

    @Override
    public int getScanLineCount() {
        return noOfLines;
    }

    @Override
    public int getElementCount() {
        return noOfColumns;
    }

    @Override
    public double getDTime(int recordNo, int scanLine) throws IOException {
        return getDouble("dtime", recordNo, scanLine, 0);
    }

    @Override
    public GeoCoding getGeoCoding(int recordNo) throws IOException {
        if (recordNo == cachedRecordNo) {
            return cachedGeoCoding;
        }
        final Variable lon = getVariable("lon");
        final Variable lat = getVariable("lat");
        final Array lonArray = getData(lon, recordNo);
        final Array latArray = getData(lat, recordNo);
        final VariableSampleSource lonSource = new VariableSampleSource(lon, lonArray);
        final VariableSampleSource latSource = new VariableSampleSource(lat, latArray);

        cachedRecordNo = recordNo;
        return cachedGeoCoding = new PixelLocatorGeoCoding(lonSource, latSource);
    }

    @Override
    public final double readInsituLon(int recordNo) throws IOException {
        return getNumberScaled("msr_lon", recordNo).floatValue();
    }

    @Override
    public final double readInsituLat(int recordNo) throws IOException {
        return getNumberScaled("msr_lat", recordNo).floatValue();
    }

    @Override
    public final double readInsituTime(int recordNo) throws IOException {
        return TimeUtil.secondsSince1981ToSecondsSinceEpoch(getDouble("msr_time", recordNo));
    }

    @Override
    public final double readInsituSst(int recordNo) throws IOException {
        return getNumberScaled("msr_sst", recordNo).doubleValue();
    }

    private static float coordinateOf(int intCoordinate) {
        return intCoordinate * 0.0001f;
    }
}
