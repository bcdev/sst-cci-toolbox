package org.esa.cci.sst.reader;

import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.Date;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
public class AatsrMatchupReader extends NetcdfMatchupReader {

    public Date getDate(String role, int recordNo) throws IOException, InvalidRangeException {
        return TimeUtil.dateOfJulianDate(getDouble(role, recordNo));
    }

    public float getCoordinate(String role, int recordNo) throws IOException, InvalidRangeException {
        return getFloat(role, recordNo);
    }
}
