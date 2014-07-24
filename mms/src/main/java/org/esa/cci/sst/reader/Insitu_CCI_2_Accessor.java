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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Insitu_CCI_2_Accessor implements InsituAccessor {

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
        final Variable timeVariable = netcdfReader.getVariable("insitu.time");
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
    public Range findExtractionRange(Date refTime, int halfExtractDurationInSeconds) {
        final double refTimeSecsSince1978 = TimeUtil.toSecondsSince1978(refTime);
        return InsituReaderHelper.findRange(historyTimes, refTimeSecsSince1978, halfExtractDurationInSeconds);
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
            samplingPoint.setIndex(i);
            samplingPoints.add(samplingPoint);
        }
        return samplingPoints;
    }

    @Override
    public Variable getVariable(String role) {
        return netcdfReader.getVariable(role);
    }

    @Override
    public void scaleTime(Array timeArray) {
        // nothing to scale here
    }

    // package access for testing only tb 2014-02-06
    static String extractWMOID(DataFile dataFile) {
        final String path = dataFile.getPath();
        final int wmoidTagStart = path.indexOf(WMOID_TAG);
        if (wmoidTagStart < 0) {
            return null;
        }

        final String wmoidPart = path.substring(wmoidTagStart + WMOID_TAG.length());
        final Matcher matcher = Pattern.compile("_[0-9]{8}_[0-9]{8}\\..*").matcher(wmoidPart);
        final boolean found = matcher.find();
        if (!found) {
            return null;
        }
        final int wmoidEnd = matcher.start();
        return wmoidPart.substring(0, wmoidEnd);
    }

    private void ensureLon() throws IOException {
        if (lon == null) {
            final Variable lonVariable = netcdfReader.getVariable("insitu.lon");
            lon = lonVariable.read();
        }
    }

    private void ensureLat() throws IOException {
        if (lat == null) {
            final Variable latVariable = netcdfReader.getVariable("insitu.lat");
            lat = latVariable.read();
        }
    }
}
