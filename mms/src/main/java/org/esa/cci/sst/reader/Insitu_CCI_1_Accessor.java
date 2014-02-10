package org.esa.cci.sst.reader;

import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Date;
import java.util.List;

class Insitu_CCI_1_Accessor implements InsituAccessor {

    public static final double HALF_JULIAN_DAY = 0.5;
    private final NetcdfReader netcdfReader;
    private Array historyTimes;

    Insitu_CCI_1_Accessor(NetcdfReader netcdfReader) {
        this.netcdfReader = netcdfReader;
    }

    @Override
    public void readHistoryTimes() throws IOException {
        final Variable timeVariable = netcdfReader.getVariable("insitu.time");
        if (timeVariable != null) {
            historyTimes = timeVariable.read();
        } else {
            throw new IllegalStateException("File format not supported: missing variable 'insitu.time'.");
        }
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
    public String getObservationName() {
        final Attribute wmo_idAttribute = netcdfReader.getNetcdfFile().findGlobalAttribute("wmo_id");
        if (wmo_idAttribute != null) {
            return wmo_idAttribute.getStringValue();
        }
        throw new IllegalStateException("File format not supported: missing attribute 'wmo_id'.");
    }

    @Override
    public double getStartLon() {
        return netcdfReader.getGlobalAttributeDouble("start_lon");
    }

    @Override
    public double getEndLon() throws IOException {
        return netcdfReader.getGlobalAttributeDouble("end_lon");
    }

    @Override
    public double getStartLat() {
        return netcdfReader.getGlobalAttributeDouble("start_lat");
    }

    @Override
    public double getEndLat() throws IOException {
        return netcdfReader.getGlobalAttributeDouble("end_lat");
    }

    @Override
    public Range find12HoursRange(Date refTime) {
        final double refJulianTime = TimeUtil.toJulianDate(refTime);
        return InsituReaderHelper.findRange(historyTimes, refJulianTime, HALF_JULIAN_DAY);
    }

    @Override
    public List<Range> createSubsampling(Range range, int maxLength) {
        return InsituReaderHelper.createSubsampling(historyTimes, range, maxLength);
    }
}
