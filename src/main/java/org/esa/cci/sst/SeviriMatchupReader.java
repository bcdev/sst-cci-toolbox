package org.esa.cci.sst;

import org.esa.cci.sst.util.TimeUtil;
import ucar.nc2.NetcdfFile;

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

    public Date getDate(String role, int recordNo) {
        final double seviriTime = getDouble(role, recordNo);
        return new Date(MILLISECONDS_1981 + (long) seviriTime * 1000);
    }

    public float getCoordinate(String role, int recordNo) {
        int seviriCoordinate = getShort(role, recordNo);
        return seviriCoordinate * 0.01f;
    }
}
