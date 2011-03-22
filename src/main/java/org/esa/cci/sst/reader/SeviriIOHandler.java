package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import static org.esa.cci.sst.SensorType.*;

/**
 * Reads records from an SEVIRI MD NetCDF input file and creates Observations.
 * Defines the variables to access in the NetCDF files and implements the conversion
 * to a common observation with a sub-scene polygon as coordinate.
 *
 * @author Martin Boettcher
 */
public class SeviriIOHandler extends NetcdfIOHandler {

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

    public SeviriIOHandler() {
        super(SEVIRI.getSensor(), "n");
    }

    @Override
    public String getSstVariableName() {
        return "sst";
    }

    @Override
    public void init(DataFile dataFile) throws IOException {
        super.init(dataFile);
        final NetcdfFile ncFile = getNcFile();
        noOfLines = ncFile.findDimension("ny").getLength();
        noOfColumns = ncFile.findDimension("nx").getLength();
    }

    /**
     * Reads record and creates ReferenceObservation for SEVIRI sub-scene contained in MD. This observation
     * may serve as common observation in some matchup. SEVIRI sub-scenes contain scan lines scanned
     * from right to left looking from first to last scan line.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     * @return Observation for SEVIRI sub-scene
     * @throws IOException  if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    @Override
    public Observation readObservation(int recordNo) throws IOException {
        //int    line   = getShort("box_center_y_coord", recordNo);
        //int    column = getShort("box_center_x_coord", recordNo);
        int line = noOfLines / 2;
        int column = noOfColumns / 2;

        final ReferenceObservation observation = new ReferenceObservation();
        observation.setName(getString("msr_id", recordNo));
        observation.setSensor(getSensorName());
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
        observation.setDatafile(getDataFileEntry());
        observation.setRecordNo(recordNo);
        observation.setClearSky(getShort(getSstVariableName(), recordNo, line, column) != getSstFillValue());
        return observation;
    }

    public Date dateOf(double secondsSince1981) {
        return new Date(MILLISECONDS_1981 + (long) secondsSince1981 * 1000);
    }

    public float coordinateOf(int intCoordinate) {
        return intCoordinate * 0.0001f;
    }
}
