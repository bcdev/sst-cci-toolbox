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
 * Reads records from an METOP MD NetCDF input file and creates Observations.
 * Defines the variables to access in the NetCDF files and implements the conversion
 * to a "reference observation" with a single point as coordinate and to a common
 * observation with a sub-scene polygon as coordinate.
 *
 * @author Martin Boettcher
 */
public class MetopMatchupReader extends NetcdfMatchupReader {

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
                "msr_time",
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
        //int line = getShort("box_center_y_coord", recordNo);
        int line = noOfLines / 2;
        return MILLISECONDS_1981 + (long) ((getDouble("msr_time", recordNo) + getDouble("dtime", recordNo, line)) * 1000);
    }

    /**
     * Reads record and creates Observation for METOP sub-scene contained in MD. This observation
     * may serve as common observation in some matchup. METOP sub-scenes contain scan lines scanned
     * from  left to right looking in flight direction.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     * @return Observation for METOP sub-scene
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. numRecords-1
     */
    @Override
    public Observation readObservation(int recordNo) throws IOException, InvalidRangeException {

        //int    line   = getShort("box_center_y_coord", recordNo);
        //int    column = getShort("box_center_x_coord", recordNo);
        int line = noOfLines / 2;
        int column = noOfColumns / 2;

        final Observation observation = new Observation();
        observation.setName(getString("msr_id", recordNo));
        observation.setSensor("metop");
        observation.setLocation(new PGgeometry(new Polygon(new LinearRing[] { new LinearRing(new Point[] {
                new Point(coordinateOf(getShort("lon", recordNo, 0, 0)),
                          coordinateOf(getShort("lat", recordNo, 0, 0))),
                new Point(coordinateOf(getShort("lon", recordNo, 0, noOfColumns - 1)),
                          coordinateOf(getShort("lat", recordNo, 0, noOfColumns - 1))),
                new Point(coordinateOf(getShort("lon", recordNo, noOfLines - 1, noOfColumns - 1)),
                          coordinateOf(getShort("lat", recordNo, noOfLines - 1, noOfColumns - 1))),
                new Point(coordinateOf(getShort("lon", recordNo, noOfLines - 1, 0)),
                          coordinateOf(getShort("lat", recordNo, noOfLines - 1, 0))),
                new Point(coordinateOf(getShort("lon", recordNo, 0, 0)),
                          coordinateOf(getShort("lat", recordNo, 0, 0)))
        })})));
        observation.setTime(dateOf(getDouble("msr_time", recordNo) + getDouble("dtime", recordNo, line)));
        observation.setDatafile(dataFileEntry);
        observation.setRecordNo(recordNo);
        observation.setClearSky(getShort(getSstVariableName(), recordNo, line, column) != sstFillValue);
        return observation;
    }

    /**
     * Reads record and creates Observation for METOP pixel contained in MD. This observation
     * may serve as reference observation in some matchup.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     * @return Observation for METOP pixel
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. numRecords-1
     */
    @Override
    public Observation readRefObs(int recordNo) throws IOException, InvalidRangeException {

        //int    line   = getShort("box_center_y_coord", recordNo);
        //int    column = getShort("box_center_x_coord", recordNo);
        int line = noOfLines / 2;
        int column = noOfColumns / 2;

        final Observation observation = new Observation();
        observation.setName(getString("msr_id", recordNo));
        observation.setSensor("metop.ref");
        observation.setLocation(new PGgeometry(new Point(coordinateOf(getShort("lon", recordNo, line, column)),
                                                         coordinateOf(getShort("lat", recordNo, line, column)))));
        observation.setTime(dateOf(getDouble("msr_time", recordNo) + getDouble("dtime", recordNo, line)));
        observation.setDatafile(dataFileEntry);
        observation.setRecordNo(recordNo);
        observation.setClearSky(getShort(getSstVariableName(), recordNo, line, column) != sstFillValue);
        return observation;
    }

    private Date dateOf(double secondsSince1981) {
        return new Date(MILLISECONDS_1981 + (long) secondsSince1981 * 1000);
    }

    // TODO handle fill value
    private float coordinateOf(int shortCoordinate) {
        if (shortCoordinate == -32768) {
            throw new IllegalArgumentException("TODO handle fill value");
        }
        return shortCoordinate * 0.01f;
    }
}
