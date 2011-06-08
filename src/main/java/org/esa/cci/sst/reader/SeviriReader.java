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
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.util.QuadTreePixelLocator;
import org.esa.beam.util.VariableSampleSource;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;

import java.awt.geom.Point2D;
import java.io.IOException;

/**
 * Reads records from an SEVIRI MD NetCDF input file and creates Observations.
 * Defines the variables to access in the NetCDF files and implements the conversion
 * to a common observation with a sub-scene polygon as coordinate.
 *
 * @author Martin Boettcher
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree"})
class SeviriReader extends MdReader {

    protected int noOfLines;
    protected int noOfColumns;
    private QuadTreePixelLocator locator;

    SeviriReader(String sensorName) {
        super(sensorName);
    }

    @Override
    public void init(DataFile datafile) throws IOException {
        super.init(datafile);
        final NetcdfFile ncFile = getNetcdfFile();
        noOfLines = ncFile.findDimension("ny").getLength();
        noOfColumns = ncFile.findDimension("nx").getLength();
        final Array lonArray = getVariable("lon").read();
        final Array latArray = getVariable("lat").read();
        locator = new QuadTreePixelLocator(new VariableSampleSource(lonArray), new VariableSampleSource(latArray));
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
        final byte dataset = getByte("msr_type", recordNo);
        switch (dataset) {
            case 1:
                observation.setDataset((byte) 2);
                break;
            case 2:
                observation.setDataset((byte) 1);
                break;
            default:
                observation.setDataset(dataset);
        }
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
        return observation;
    }

    private static float coordinateOf(int intCoordinate) {
        return intCoordinate * 0.0001f;
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
        final double time = getDouble("time", recordNo);
        final double dtime = getDTime(recordNo, scanLine);
        return (int) TimeUtil.secondsSince1981ToDate(time + dtime).getTime();
    }

    @Override
    public int getDTime(int recordNo, int scanLine) throws IOException {
        final double time = getDouble("dtime", recordNo, scanLine, 0);
        return (int) TimeUtil.secondsSince1981ToDate(time).getTime();
    }

    @Override
    public GeoCoding getGeoCoding(int recordNo) throws IOException {
        String longitudeVariableName = "lon";
        String latitudeVariableName = "lat";
        Array lonArray = getNetcdfFile().findVariable(longitudeVariableName).read();
        Array latArray = getNetcdfFile().findVariable(latitudeVariableName).read();
        lonArray = scale(getColumn(longitudeVariableName).getScaleFactor(), lonArray);
        latArray = scale(getColumn(latitudeVariableName).getScaleFactor(), latArray);
        return new LSGeoCoding(new VariableSampleSource(lonArray), new VariableSampleSource(latArray));
    }

    private Array scale(Number scaleFactor, Array array) {
        if (scaleFactor == null) {
            return array;
        }
        final Array scaledArray = Array.factory(DataType.DOUBLE, array.getShape());
        for (int i = 0; i < array.getSize(); i++) {
            double value = ((Number) array.getObject(i)).doubleValue() * scaleFactor.doubleValue();
            scaledArray.setDouble(i, value);
        }
        return scaledArray;
    }
}
