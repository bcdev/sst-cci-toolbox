package org.esa.cci.sst.reader;

import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
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
        return findRange(historyTimes, refJulianTime);
    }

    /**
     * Returns the range of the in-situ history time data that falls within ±12 hours of a
     * given reference time.
     *
     * @param historyTimes  The in-situ history time data (JD). The array must be of rank 1 and
     *                      its elements must be sorted in ascending order.
     * @param referenceTime The reference time (JD).
     * @return the range of the in-situ history time data that falls within ±12 hours of the
     * given reference time.
     * @throws IllegalArgumentException when {@code historyTimes.getRank() != 1}.
     */
    static Range findRange(Array historyTimes, double referenceTime) {
        if (historyTimes.getRank() != 1) {
            throw new IllegalArgumentException("history.getRank() != 1");
        }
        if (referenceTime + 0.5 < historyTimes.getDouble(0)) {
            return Range.EMPTY;
        }
        final int historyLength = historyTimes.getIndexPrivate().getShape(0);
        if (referenceTime - 0.5 > historyTimes.getDouble(historyLength - 1)) {
            return Range.EMPTY;
        }
        int startIndex = -1;
        int endIndex = -1;
        for (int i = 0; i < historyLength; i++) {
            final double time = historyTimes.getDouble(i);
            if (startIndex == -1) {
                if (time >= referenceTime - 0.5) {
                    startIndex = i;
                    endIndex = startIndex;
                }
            } else {
                if (time <= referenceTime + 0.5) {
                    endIndex = i;
                } else {
                    break;
                }
            }
        }
        try {
            return new Range(startIndex, endIndex);
        } catch (InvalidRangeException e) {
            return Range.EMPTY;
        }
    }
}
