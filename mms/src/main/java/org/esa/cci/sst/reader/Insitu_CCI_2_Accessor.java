package org.esa.cci.sst.reader;

import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Date;
import java.util.List;

class Insitu_CCI_2_Accessor implements InsituAccessor {

    private static final int HALF_DAY_SECS_1978 = 43200;

    private final NetcdfReader netcdfReader;
    private Array historyTimes;

    Insitu_CCI_2_Accessor(NetcdfReader netcdfReader) {
        this.netcdfReader = netcdfReader;
    }

    @Override
    public void readHistoryTimes() throws IOException {
        final Variable timeVariable = netcdfReader.getVariable("time");
        historyTimes = timeVariable.read();
    }

    @Override
    public Date getHistoryStart() {
        final double startSeconds = historyTimes.getDouble(0);
        return TimeUtil.secondsSince1978ToDate(startSeconds);
    }

    @Override
    public Date getHistoryEnd() {
        final double endSeconds = historyTimes.getDouble(historyTimes.getIndexPrivate().getShape(0) - 1);
        return TimeUtil.secondsSince1978ToDate(endSeconds);
    }

    @Override
    public Range find12HoursRange(Date refTime) {
        final double refTimeSecsSince1978 = TimeUtil.toSecondsSince1978(refTime);
        return InsituReaderHelper.findRange(historyTimes, refTimeSecsSince1978, HALF_DAY_SECS_1978);
    }

    @Override
    public List<Range> createSubsampling(Range range, int maxLength) {
        return InsituReaderHelper.createSubsampling(historyTimes, range, maxLength);
    }
}
