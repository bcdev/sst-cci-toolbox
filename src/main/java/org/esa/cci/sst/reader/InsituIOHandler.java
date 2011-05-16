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

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Observation;
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
class InsituIOHandler extends NetcdfIOHandler {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
    private Array historyTimes;

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    InsituIOHandler(String sensorName) {
        super(sensorName);
    }

    @Override
    public void init(DataFile dataFile) throws IOException {
        super.init(dataFile);
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
        final DataFile dataFile = getDataFile();
        observation.setDatafile(dataFile);
        observation.setCallsign(getNcFile().findGlobalAttribute("wmo_id").getStringValue());
        observation.setRecordNo(0);
        observation.setSensor(getSensorName());

        final Date startTime = TimeUtil.julianDateToDate(historyTimes.getDouble(0));
        final Date endTime = TimeUtil.julianDateToDate(
                historyTimes.getDouble(historyTimes.getIndexPrivate().getShape(0) - 1));
        observation.setTime(TimeUtil.centerTime(startTime, endTime));
        observation.setTimeRadius(TimeUtil.timeRadius(startTime, endTime));

        try {
            final double startLon = parseDouble("start_lon");
            final double startLat = parseDouble("start_lat");
            final double endLon = parseDouble("end_lon");
            final double endLat = parseDouble("end_lat");
            observation.setLocation(createLineGeometry(startLon, startLat, endLon, endLat));
        } catch (ParseException e) {
            throw new IOException("Unable to set location.", e);
        }
        return observation;
    }

    /**
     * Writes a subset of an in-situ history observation to an MMD target file. The method must be called per
     * variable and per match-up. The subset covers plus/minus 12 hours centered at the reference observation
     * time. The subset is limited by the size of the target array, which is {@link org.esa.cci.sst.tools.Constants#INSITU_HISTORY_LENGTH}.
     *
     * @param targetFile   The target MMD file.
     * @param observation  The observation to write.
     * @param matchupIndex The target matchup index.
     * @param refPoint     Not used.
     * @param refTime      The reference time.
     *
     * @throws IOException when an IO error has occurred.
     */
    @Override
    public void write(NetcdfFileWriteable targetFile, Observation observation, String sourceVariableName,
                      String targetVariableName, int matchupIndex, PGgeometry refPoint, Date refTime) throws
                                                                                                      IOException {
        final NetcdfFile sourceFile = getNcFile();
        final Variable sourceVariable = sourceFile.findVariable(NetcdfFile.escapeName(sourceVariableName));
        final Variable targetVariable = targetFile.findVariable(NetcdfFile.escapeName(targetVariableName));

        try {
            final Range range = findRange(historyTimes, TimeUtil.toJulianDate(refTime));
            if (range != Range.EMPTY) {
                final List<Range> subsampling = createSubsampling(historyTimes, range, targetVariable.getShape(1));
                final Array source = sourceVariable.read();
                // todo - convert values into seconds since 1978 here (rq-20110420)
                final Array subset = createSubset(source, subsampling);
                writeSubset(targetFile, targetVariable, matchupIndex, subset);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private double parseDouble(String attributeName) throws ParseException {
        return Double.parseDouble(getNcFile().findGlobalAttribute(attributeName).getStringValue());
    }

    private Array readHistoryTimes() throws IOException {
        return getNcFile().findVariable(NetcdfFile.escapeName("insitu.time")).read();
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
            final ArrayList<Range> subsampling = new ArrayList<Range>();
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

    static Array createSubset(Array source, List<Range> subsetRanges) throws InvalidRangeException {
        // compute subset shape for first dimension
        int length = 0;
        for (final Range r : subsetRanges) {
            length += r.length();
        }
        // create empty subset array
        final int[] subsetShape = source.getShape();
        subsetShape[0] = length;
        final Array subset = Array.factory(source.getElementType(), subsetShape);
        // setup ranges for copying
        final ArrayList<Range> sourceRanges = new ArrayList<Range>(source.getRank());
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
        return subset;
    }

    private static void writeSubset(NetcdfFileWriteable targetFile, Variable targetVariable, int matchupIndex,
                                    Array subset) throws Exception {
        final int[] origin = new int[targetVariable.getRank()];
        origin[0] = matchupIndex;
        final int[] shape = targetVariable.getShape();
        shape[0] = 1;
        shape[1] = subset.getIndexPrivate().getShape(0);
        targetFile.write(NetcdfFile.escapeName(targetVariable.getName()), origin, subset.reshape(shape));
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
