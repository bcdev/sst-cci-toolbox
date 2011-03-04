package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import static org.esa.cci.sst.SensorName.*;

/**
 * Reads records from an SEVIRI MD NetCDF input file and creates Observations.
 * Defines the variables to access in the NetCDF files and implements the conversion
 * to a common observation with a sub-scene polygon as coordinate.
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

    public SeviriMatchupReader() {
        super(SENSOR_NAME_SEVIRI.getSensor());
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
    public void init(DataFile dataFileEntry) throws IOException {
        super.init(dataFileEntry);
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

    /**
     * Reads record and creates ReferenceObservation for SEVIRI sub-scene contained in MD. This observation
     * may serve as common observation in some matchup. SEVIRI sub-scenes contain scan lines scanned
     * from right to left looking from first to last scan line.
     *
     *
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     * @return Observation for SEVIRI sub-scene
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. numRecords-1
     */
    @Override
    public GlobalObservation readObservation(int recordNo) throws IOException, InvalidRangeException {

        //int    line   = getShort("box_center_y_coord", recordNo);
        //int    column = getShort("box_center_x_coord", recordNo);
        int line = noOfLines / 2;
        int column = noOfColumns / 2;

        final ReferenceObservation observation = new ReferenceObservation();
        observation.setName(getString("msr_id", recordNo));
        observation.setSensor(SENSOR_NAME_SEVIRI.getSensor());
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
        observation.setPoint(new PGgeometry(new Point(coordinateOf(getInt("lon", recordNo, line, column)),
                                                      coordinateOf(getInt("lat", recordNo, line, column)))));
        observation.setTime(dateOf(getDouble("time", recordNo) + getDouble("dtime", recordNo, line, column)));
        observation.setDatafile(dataFileEntry);
        observation.setRecordNo(recordNo);
        observation.setClearSky(getShort(getSstVariableName(), recordNo, line, column) != sstFillValue);
        return observation;
    }

    public Date dateOf(double secondsSince1981) {
        return new Date(MILLISECONDS_1981 + (long) secondsSince1981 * 1000);
    }

    public float coordinateOf(int intCoordinate) {
        return intCoordinate * 0.0001f;
    }
}
