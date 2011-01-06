package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.Date;

/**
 * TODO add API doc
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

    @Override
    public Observation readObservation(int recordNo) throws IOException, InvalidRangeException {
        //throw new UnsupportedOperationException("aatsr only available as reference");
        return null;
    }

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

    public Date dateOf(double julianDate) throws IOException, InvalidRangeException {
        return TimeUtil.dateOfJulianDate(julianDate);
    }
}
