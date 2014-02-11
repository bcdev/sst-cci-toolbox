package org.esa.cci.sst.reader;

import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

class Insitu_CCI_1_Accessor implements InsituAccessor {

    private static final double HALF_JULIAN_DAY = 0.5;

    private final NetcdfReader netcdfReader;
    private final HashMap<String, String> variableNamesMap;
    private Array historyTimes;
    private Array lon;
    private Array lat;

    Insitu_CCI_1_Accessor(NetcdfReader netcdfReader) {
        this.netcdfReader = netcdfReader;
        variableNamesMap = new HashMap<>();
        variableNamesMap.put("sst", "insitu.sea_surface_temperature");
        variableNamesMap.put("lon", "insitu.longitude");
        variableNamesMap.put("lat", "insitu.latitude");
        variableNamesMap.put("time", "insitu.time");
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

    @Override
    public List<SamplingPoint> readSamplingPoints() throws IOException {
        ensureLon();
        ensureLat();
        final LinkedList<SamplingPoint> samplingPoints = new LinkedList<>();

        final int numRecords = historyTimes.getIndexPrivate().getShape(0);
        for (int i = 0; i < numRecords; i++) {
            final double mjd = historyTimes.getDouble(i);
            final Date date = TimeUtil.julianDateToDate(mjd);
            final SamplingPoint samplingPoint = new SamplingPoint(lon.getDouble(i), lat.getDouble(i), date.getTime(), Double.NaN);
            final String wmoId = getObservationName();
            samplingPoint.setReference(Integer.parseInt(wmoId));
            samplingPoints.add(samplingPoint);
        }
        return samplingPoints;
    }

    @Override
    public Variable getVariable(String role) {
        final String variableName = variableNamesMap.get(role);
        return netcdfReader.getVariable(variableName);
    }

    @Override
    public void scaleTime(Array timeArray) {
        final long size = timeArray.getSize();
        for (int i = 0; i < size; i++) {
            final double mjd = timeArray.getDouble(i);
            timeArray.setDouble(i, TimeUtil.julianDateToSecondsSinceEpoch(mjd));
        }
    }

    private void ensureLon() throws IOException {
        if (lon == null) {
            final Variable lonVariable = netcdfReader.getVariable("insitu.longitude");
            lon = lonVariable.read();
        }
    }

    private void ensureLat() throws IOException {
        if (lat == null) {
            final Variable latVariable = netcdfReader.getVariable("insitu.latitude");
            lat = latVariable.read();
        }
    }
}
