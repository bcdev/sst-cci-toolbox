/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.reader;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Allows reading of observations from the in-history situ data.
 *
 * @author Thomas Storm
 */
class InsituReader extends NetcdfReader {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
    private Array historyTimes;

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    InsituReader(String sensorName) {
        super(sensorName);
    }

    @Override
    public void init(DataFile datafile, File archiveRoot) throws IOException {
        super.init(datafile, archiveRoot);
        historyTimes = readHistoryTimes();
    }

    @Override
    public void close() {
        super.close();
        historyTimes = null;
    }

    @Override
    public int getNumRecords() {
        return 1;
    }

    @Override
    public InsituObservation readObservation(int recordNo) throws IOException {
        final InsituObservation observation = new InsituObservation();
        final DataFile dataFile = getDatafile();
        observation.setDatafile(dataFile);
        observation.setName(getNetcdfFile().findGlobalAttribute("wmo_id").getStringValue());
        observation.setRecordNo(0);
        observation.setSensor(getSensorName());

        final Date startTime = TimeUtil.julianDateToDate(historyTimes.getDouble(0));
        final Date endTime = TimeUtil.julianDateToDate(
                historyTimes.getDouble(historyTimes.getIndexPrivate().getShape(0) - 1));
        observation.setTime(TimeUtil.centerTime(startTime, endTime));
        observation.setTimeRadius(TimeUtil.timeRadius(startTime, endTime));

        try {
            final double startLon = (parseDouble("start_lon") + 180.0) % 360.0 - 180.0;
            final double startLat = parseDouble("start_lat");
            final double endLon = (parseDouble("end_lon") + 180.0) % 360.0 - 180.0;
            final double endLat = parseDouble("end_lat");
            if (startLat < -90.0 || startLat > 90.0 || endLat < -90.0 || endLat > 90.0) {
                throw new IOException(String.format("latitude attributes (%g .. %g) out of range (-90.0 .. 90.0)", startLat, endLat));
            }
            observation.setLocation(createLineGeometry(startLon, startLat, endLon, endLat));
        } catch (ParseException e) {
            throw new IOException("Unable to set location.", e);
        }
        return observation;
    }

    @Override
    public final Array read(String role, ExtractDefinition extractDefinition) throws IOException {
        final Variable sourceVariable = getVariable(role);
        final Date refTime = extractDefinition.getDate();
        final Range range = findRange(historyTimes, TimeUtil.toJulianDate(refTime));
        final Array source = sourceVariable.read();
        final Array target = Array.factory(source.getElementType(), extractDefinition.getShape());
        final Number fillValue = getAttribute(sourceVariable, "_FillValue", Short.MIN_VALUE);
        for (int i = 0; i < target.getSize(); i++) {
            target.setObject(i, fillValue);
        }
        if (range != Range.EMPTY) {
            final List<Range> subsampling = createSubsampling(historyTimes, range, extractDefinition.getShape()[1]);
            try {
                extractSubset(source, target, subsampling);
            } catch (InvalidRangeException e) {
                throw new IOException("Unable to create target.", e);
            }
        }
        return target;
    }

    @Override
    public GeoCoding getGeoCoding(int recordNo) throws IOException {
        return null;
    }

    @Override
    public double getDTime(int recordNo, int scanLine) throws IOException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public long getTime(int recordNo, int scanLine) throws IOException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public InsituSource getInsituSource() {
        return null;
    }

    @Override
    public int getScanLineCount() {
        throw new IllegalStateException("Not implemented.");
    }

    @Override
    public int getElementCount() {
        throw new IllegalStateException("Not implemented");
    }

    private double parseDouble(String attributeName) throws ParseException {
        return Double.parseDouble(getNetcdfFile().findGlobalAttribute(attributeName).getStringValue());
    }

    private Array readHistoryTimes() throws IOException {
        return getVariable("insitu.time").read();
    }

    /**
     * Returns the range of the in-situ history time data that falls within ±12 hours of a
     * given reference time.
     *
     * @param historyTimes  The in-situ history time data (JD). The array must be of rank 1 and
     *                      its elements must be sorted in ascending order.
     * @param referenceTime The reference time (JD).
     *
     * @return the range of the in-situ history time data that falls within ±12 hours of the
     *         given reference time.
     *
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

    static List<Range> createSubsampling(Array historyTimes, Range range, int maxLength) {
        try {
            final List<Range> subsampling = new ArrayList<Range>();
            if (range.length() > maxLength) {
                subsampling.add(new Range(range.first(), range.first()));
                // get maxLength-2 entries from the history
                final double startTime = historyTimes.getDouble(range.first());
                final double endTime = historyTimes.getDouble(range.last());
                final double timeStep = (endTime - startTime) / (maxLength - 1);
                for (int i = range.first() + 1; i < range.last(); i++) {
                    if (historyTimes.getDouble(i) >= startTime + subsampling.size() * timeStep) {
                        if (subsampling.size() < maxLength - 1) {
                            subsampling.add(new Range(i, i));
                        }
                    }
                }
                subsampling.add(new Range(range.last(), range.last()));
            } else { // no subset needed
                subsampling.add(range);
            }
            return subsampling;
        } catch (InvalidRangeException e) {
            return Collections.emptyList();
        }
    }

    static void extractSubset(Array source, Array subset, List<Range> subsetRanges) throws InvalidRangeException {
        // setup ranges for copying
        final List<Range> sourceRanges = new ArrayList<Range>(source.getRank());
        for (int i = 0; i < source.getRank(); i++) {
            sourceRanges.add(null);
        }
        // copy values from source to subset
        final IndexIterator subsetIterator = subset.getIndexIterator();
        for (final Range s : subsetRanges) {
            sourceRanges.set(0, s);
            final Array sourceSection = source.section(sourceRanges);
            final IndexIterator sourceIterator = sourceSection.getIndexIterator();
            while (sourceIterator.hasNext()) {
                subsetIterator.setObjectNext(sourceIterator.getObjectNext());
            }
        }
    }

    private static void writeSubset(NetcdfFileWriteable targetFile, Variable targetVariable, int matchupIndex,
                                    Array subset) throws Exception {
        final int[] origin = new int[targetVariable.getRank()];
        origin[0] = matchupIndex;
        final int[] shape = targetVariable.getShape();
        shape[0] = 1;
        shape[1] = subset.getIndexPrivate().getShape(0);
        targetFile.write(NetcdfFile.makeValidPathName(targetVariable.getName()), origin, subset.reshape(shape));
    }

    private static PGgeometry createLineGeometry(double startLon, double startLat, double endLon, double endLat) {
        double startLon1 = normalizeLon(startLon);
        double endLon1 = normalizeLon(endLon);

        return new PGgeometry(new LineString(new Point[]{new Point(startLon1, startLat), new Point(endLon1, endLat)}));
    }

    // this is a copy of {@code GeoPos.normalizeLon()} using {@code double} instead of {@code float} as argument
    private static double normalizeLon(double lon) {
        double lon1 = lon;
        if (lon1 < -360.0 || lon1 > 360.0) {
            lon1 %= 360.0;
        }
        if (lon1 < -180.0) {
            lon1 += 360.0;
        } else if (lon1 > 180.0) {
            lon1 -= 360.0;
        }
        return lon1;
    }
}
