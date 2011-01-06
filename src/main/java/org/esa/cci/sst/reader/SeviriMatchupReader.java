package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
public class SeviriMatchupReader extends NetcdfMatchupReader {

    static final long MILLISECONDS_1981;

    static {
        try {
            MILLISECONDS_1981 = TimeUtil.parseCcsdsUtcFormat("1981-01-01T00:00:00Z");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected int noOfLines;
    protected int noOfColumns;

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
        return new String[] {
                "msr_id",
                "lat",
                "lon",
                "box_center_y_coord",
                "box_center_x_coord",
                "time",
                "dtime",
                "sst"
        };
    }

    @Override
    public void init(File observationFile, DataFile dataFileEntry) throws IOException {
        super.init(observationFile, dataFileEntry);
        noOfLines = netcdf.findDimension("ny").getLength();
        noOfColumns = netcdf.findDimension("nx").getLength();
    }

    @Override
    public long getTime(int recordNo) throws IOException, InvalidRangeException {
        //int    line   = getShort("box_center_y_coord", recordNo);
        //int    column = getShort("box_center_x_coord", recordNo);
        int line = noOfLines / 2;
        int column = noOfColumns / 2;
        return MILLISECONDS_1981 + (long) ((getDouble("time", recordNo) + getDouble("dtime", recordNo, line, column)) * 1000);
    }

    @Override
    public Observation readObservation(int recordNo) throws IOException, InvalidRangeException {

        //int    line   = getShort("box_center_y_coord", recordNo);
        //int    column = getShort("box_center_x_coord", recordNo);
        int line = noOfLines / 2;
        int column = noOfColumns / 2;

        final Observation observation = new Observation();
        observation.setName(getString("msr_id", recordNo));
        observation.setSensor("seviri");
        observation.setLocation(new PGgeometry(new Polygon(new LinearRing[] { new LinearRing(new Point[] {
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
        })})));
        observation.setTime(dateOf(getDouble("time", recordNo) + getDouble("dtime", recordNo, line, column)));
        observation.setDatafile(dataFileEntry);
        observation.setRecordNo(recordNo);
        observation.setClearSky(getShort(getSstVariableName(), recordNo, line, column) != sstFillValue);
        return observation;
    }

    @Override
    public Observation readRefObs(int recordNo) throws IOException, InvalidRangeException {
        //throw new UnsupportedOperationException("seviri only available as common observation");
        return null;
    }

     public Date dateOf(double secondsSince1981) {
        return new Date(MILLISECONDS_1981 + (long) secondsSince1981 * 1000);
    }

    public float coordinateOf(int intCoordinate) {
        return intCoordinate * 0.0001f;
    }
}
