package org.esa.cci.sst;

import org.esa.cci.sst.util.TimeUtil;
import ucar.nc2.NetcdfFile;

import java.util.Date;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
public class AatsrMatchupReader extends NetcdfMatchupReader {

    public Date getDate(String role, int recordNo) {
        return TimeUtil.dateOfJulianDate(getDouble(role, recordNo));
    }

    public float getCoordinate(String role, int recordNo) {
        return getFloat(role, recordNo);
    }
}
