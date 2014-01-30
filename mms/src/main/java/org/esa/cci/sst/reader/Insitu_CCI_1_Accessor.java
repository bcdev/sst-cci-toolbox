package org.esa.cci.sst.reader;

import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Date;

class Insitu_CCI_1_Accessor implements InsituAccessor {

    private final NetcdfReader netcdfReader;
    private Array historyTimes;

    Insitu_CCI_1_Accessor(NetcdfReader netcdfReader) {
        this.netcdfReader = netcdfReader;
    }

    @Override
    public void readHistoryTimes() throws IOException {
        final Variable timeVariable = netcdfReader.getVariable("insitu.time");
        historyTimes = timeVariable.read();
    }

    @Override
    public Date getHistoryStart() {
        final double startMjd = historyTimes.getDouble(0);
        return TimeUtil.julianDateToDate(startMjd);
    }

    @Override
    public Date getHistoryEnd() {
        final double endMjd = historyTimes.getDouble(historyTimes.getIndexPrivate().getShape(0) - 1);
        return TimeUtil.julianDateToDate(endMjd);
    }

    @Override
    public Range find12HoursRange(Date refTime) {
        final double refJulianTime = TimeUtil.toJulianDate(refTime);
        return InsituReader.findRange(historyTimes, refJulianTime, 0.5);
    }
}
