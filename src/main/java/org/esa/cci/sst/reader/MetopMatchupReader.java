package org.esa.cci.sst.reader;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.util.PgUtil;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Reads records from an METOP MD NetCDF input file and creates Observations.
 * Defines the variables to access in the NetCDF files and implements the conversion
 * to a "reference observation" with a single point as coordinate and to a common
 * observation with a sub-scene polygon as coordinate.
 *
 * @author Martin Boettcher
 */
public class MetopMatchupReader extends NetcdfMatchupReader {

    private static final int LAT_LON_FILL_VALUE = -32768;
    static final long MILLISECONDS_1981;

    static {
        try {
            MILLISECONDS_1981 = TimeUtil.parseCcsdsUtcFormat("1981-01-01T00:00:00Z");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected int rowCount;
    protected int colCount;

    public MetopMatchupReader() {
        super(Constants.SENSOR_NAME_METOP);
    }

    @Override
    public String getDimensionName() {
        return "n";
    }

    @Override
    public String getSstVariableName() {
        return "sst";
    }

    @Override
    public String[] getVariableNames() {
        return new String[]{
                "msr_id",
                "lat",
                "lon",
                "box_center_y_coord",
                "box_center_x_coord",
                "msr_time",
                "dtime",
                "sst"
        };
    }

    @Override
    public void init(File observationFile, DataFile dataFileEntry) throws IOException {
        super.init(observationFile, dataFileEntry);
        rowCount = netcdf.findDimension("ny").getLength();
        colCount = netcdf.findDimension("nx").getLength();
    }

    @Override
    public long getTime(int recordNo) throws IOException, InvalidRangeException {
        return MILLISECONDS_1981 + 1000L * Math.round(
                getDouble("msr_time", recordNo) + getDouble("dtime", recordNo, rowCount / 2));
    }

    /**
     * Reads record and creates Observation for METOP sub-scene contained in MD. This observation
     * may serve as common observation in some matchup. METOP sub-scenes contain scan lines scanned
     * from  left to right looking in flight direction.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     *
     * @return Observation for METOP sub-scene
     *
     * @throws IOException           if file io fails
     * @throws InvalidRangeException if record number is out of range 0 .. numRecords-1
     */
    @Override
    public Observation readObservation(int recordNo) throws IOException, InvalidRangeException {
        final int x = colCount / 2;
        final int y = rowCount / 2;

        final Observation observation = new Observation();
        observation.setName(getString("msr_id", recordNo));
        observation.setSensor(Constants.SENSOR_NAME_METOP);
        observation.setLocation(new PGgeometry(new Polygon(new LinearRing[]{new LinearRing(getPoints(recordNo))})));
        observation.setTime(toDate(getDouble("msr_time", recordNo) + getDouble("dtime", recordNo, y)));
        observation.setDatafile(dataFileEntry);
        observation.setRecordNo(recordNo);
        observation.setClearSky(getShort(getSstVariableName(), recordNo, y, x) != sstFillValue);

        return observation;
    }

    /**
     * Reads record and creates Observation for METOP pixel contained in MD. This observation
     * may serve as reference observation in some matchup.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     *
     * @return Observation for METOP pixel
     *
     * @throws IOException           if file io fails
     * @throws InvalidRangeException if record number is out of range 0 .. numRecords-1
     */
    @Override
    public Observation readRefObs(int recordNo) throws IOException, InvalidRangeException {
        final int x = colCount / 2;
        final int y = rowCount / 2;

        final Observation observation = new Observation();
        observation.setName(getString("msr_id", recordNo));
        observation.setSensor(Constants.SENSOR_NAME_METOP_REFERENCE);
        observation.setLocation(new PGgeometry(newPoint(getLon(recordNo, y, x), getLat(recordNo, y, x))));
        observation.setTime(toDate(getDouble("msr_time", recordNo) + getDouble("dtime", recordNo, y)));
        observation.setDatafile(dataFileEntry);
        observation.setRecordNo(recordNo);
        observation.setClearSky(getShort(getSstVariableName(), recordNo, y, x) != sstFillValue);

        return observation;
    }

    private Point[] getPoints(int recordNo) throws IOException, InvalidRangeException {
        final List<Point> pointList = new ArrayList<Point>(9);

        for (int i = 0; i < rowCount - 1; i++) {
            final int lon = getLon(recordNo, i, 0);
            final int lat = getLat(recordNo, i, 0);
            if (isValid(lon, lat)) {
                if (pointList.size() < 1) {
                    pointList.add(newPoint(lon, lat));
                } else {
                    pointList.add(1, newPoint(lon, lat));
                }
            }
        }
        for (int k = 0; k < colCount - 1; k++) {
            final int lon = getLon(recordNo, rowCount - 1, k);
            final int lat = getLat(recordNo, rowCount - 1, k);
            if (isValid(lon, lat)) {
                if (pointList.size() < 3) {
                    pointList.add(newPoint(lon, lat));
                } else {
                    pointList.add(3, newPoint(lon, lat));
                }
            }
        }
        for (int i = rowCount - 1; i > 0; i--) {
            final int lon = getLon(recordNo, i, colCount - 1);
            final int lat = getLat(recordNo, i, colCount - 1);
            if (isValid(lon, lat)) {
                if (pointList.size() < 5) {
                    pointList.add(newPoint(lon, lat));
                } else {
                    pointList.add(5, newPoint(lon, lat));
                }
            }
        }
        for (int k = colCount - 1; k > 0; k--) {
            final int lon = getLon(recordNo, 0, k);
            final int lat = getLat(recordNo, 0, k);
            if (isValid(lon, lat)) {
                if (pointList.size() < 7) {
                    pointList.add(newPoint(lon, lat));
                } else {
                    pointList.add(7, newPoint(lon, lat));
                }
            }
        }
        pointList.add(pointList.get(0));
        if (PgUtil.isClockwise(pointList)) {
            Collections.reverse(pointList);
        }

        return pointList.toArray(new Point[pointList.size()]);
    }

    private int getLat(int recordNo, int y, int x) throws IOException, InvalidRangeException {
        return getShort("lat", recordNo, y, x);
    }

    private int getLon(int recordNo, int y, int x) throws IOException, InvalidRangeException {
        return getShort("lon", recordNo, y, x);
    }

    private static Date toDate(double secondsSince1981) {
        return new Date(MILLISECONDS_1981 + (long) secondsSince1981 * 1000);
    }

    private static boolean isValid(int lon, int lat) {
        return lon != LAT_LON_FILL_VALUE && lat != LAT_LON_FILL_VALUE;
    }

    private static Point newPoint(int lon, int lat) {
        return new Point(0.01 * lon, 0.01 * lat);
    }
}
