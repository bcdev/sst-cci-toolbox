package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

class Insitu_CCI_2_Accessor implements InsituAccessor {

    private static final int HALF_DAY_SECS_1978 = 43200;
    public static final String WMOID_TAG = "WMOID_";

    private final NetcdfReader netcdfReader;
    private Array historyTimes;
    private Array lon;
    private Array lat;

    Insitu_CCI_2_Accessor(NetcdfReader netcdfReader) {
        this.netcdfReader = netcdfReader;
        historyTimes = null;
        lon = null;
        lat = null;
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
    public String getObservationName() {
        final DataFile datafile = netcdfReader.getDatafile();

        return extractWMOID(datafile);
    }

    @Override
    public double getStartLon() throws IOException {
        ensureLon();
        return lon.getDouble(0);
    }

    @Override
    public double getEndLon() throws IOException {
        ensureLon();
        return lon.getDouble((int) lon.getSize() - 1);
    }

    @Override
    public double getStartLat() throws IOException {
        ensureLat();
        return lat.getDouble(0);
    }

    @Override
    public double getEndLat() throws IOException {
        ensureLat();
        return lat.getDouble((int) lat.getSize() - 1);
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

    @Override
    public List<SamplingPoint> readSamplingPoints() throws IOException {
        ensureLat();
        ensureLon();

        final LinkedList<SamplingPoint> samplingPoints = new LinkedList<>();
        final int numRecords = historyTimes.getIndexPrivate().getShape(0);
        for (int i = 0; i < numRecords; i++) {
            final double secsSince1978 = historyTimes.getDouble(i);
            final Date date = TimeUtil.secondsSince1978ToDate(secsSince1978);
            final SamplingPoint samplingPoint = new SamplingPoint(lon.getDouble(i), lat.getDouble(i), date.getTime(), Double.NaN);
            final String observationName = getObservationName();
            samplingPoint.setReference(Integer.parseInt(observationName));
            samplingPoints.add(samplingPoint);
        }
        return samplingPoints;
    }

    // package access for testing only tb 2014-02-06
    static String extractWMOID(DataFile dataFile) {
        final String path = dataFile.getPath();
        int wmoidStartIndex = path.indexOf(WMOID_TAG);
        if (wmoidStartIndex < 0) {
            return null;
        }

        wmoidStartIndex += WMOID_TAG.length();
        final int wmoidEndIndex = path.indexOf("_", wmoidStartIndex + 1);
        return path.substring(wmoidStartIndex, wmoidEndIndex);
    }

    private void ensureLon() throws IOException {
        if (lon == null) {
            final Variable lonVariable = netcdfReader.getVariable("lon");
            lon = lonVariable.read();
        }
    }

    private void ensureLat() throws IOException {
        if (lat == null) {
            final Variable latVariable = netcdfReader.getVariable("lat");
            lat = latVariable.read();
        }
    }
}
