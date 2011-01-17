package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.Date;

/**
 * Reads records from an (A)ATSR MD NetCDF input file and creates Observations.
 * Defines the variables to access in the NetCDF files and implements the conversion
 * to a "reference observation" with a single point as coordinate. (A)ATSR MDs only
 * serve as reference observation. They never provide a coverage to serve as "common
 * observation" that matches a reference observation. Therefore, the readObservation()
 * method is not implemented.
 *
 * @author Martin Boettcher
 */
public class AatsrMatchupReader extends NetcdfMatchupReader {

    @Override
    public String getDimensionName() {
        return "match_up";
    }

    @Override
    public String getSstVariableName() {
        return "atsr.sea_surface_temperature.dual";
    }

    @Override
    public String[] getVariableNames() {
        return new String[]{
                "insitu.unique_identifier",
                "atsr.latitude",
                "atsr.longitude",
                "atsr.time.julian",
                "atsr.sea_surface_temperature.dual"
        };
    }

    @Override
    public long getTime(int recordNo) throws IOException, InvalidRangeException {
        return dateOf(getDouble("atsr.time.julian", recordNo)).getTime();
    }

    /**
     * Constantly returns null as (A)ATSR MD never serves as common observation
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     * @return  null
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. numRecords-1
     */
    @Override
    public Observation readObservation(int recordNo) throws IOException, InvalidRangeException {
        //throw new UnsupportedOperationException("aatsr only available as reference");
        return null;
    }

    /**
     * Reads record and creates Observation for (A)ATSR pixel contained in MD. This observation
     * may serve as reference observation in some matchup.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     * @return Observation for (A)ATSR pixel
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. numRecords-1
     */
    @Override
    public Observation readRefObs(int recordNo) throws IOException, InvalidRangeException {

        final Observation observation = new Observation();
        observation.setName(getString("insitu.unique_identifier", recordNo));
        observation.setSensor("aatsr.ref");
        observation.setLocation(new PGgeometry(new Point(getFloat("atsr.longitude", recordNo),
                                                         getFloat("atsr.latitude", recordNo))));
        observation.setTime(dateOf(getDouble("atsr.time.julian", recordNo)));
        observation.setDatafile(dataFileEntry);
        observation.setRecordNo(recordNo);
        observation.setClearSky(getShort("atsr.sea_surface_temperature.dual", recordNo) != sstFillValue);
        return observation;
    }

    private Date dateOf(double julianDate) throws IOException, InvalidRangeException {
        return TimeUtil.dateOfJulianDate(julianDate);
    }
}
