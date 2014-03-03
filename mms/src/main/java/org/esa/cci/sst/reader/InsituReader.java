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
import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.util.GeometryUtil;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Allows reading of observations from the in-history situ data.
 *
 * @author Thomas Storm
 */
class InsituReader extends NetcdfReader {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);

    private InsituAccessor insituAccessor;

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    InsituReader(String sensorName) {
        super(sensorName);
    }

    @Override
    public void init(DataFile datafile, File archiveRoot) throws IOException {
        super.init(datafile, archiveRoot);

        insituAccessor = createInsituAccessor();
        insituAccessor.readHistoryTimes();
    }

    @Override
    public void close() {
        super.close();
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
        observation.setName(insituAccessor.getObservationName());
        observation.setRecordNo(0);
        observation.setSensor(getSensorName());

        final Date startTime = insituAccessor.getHistoryStart();
        final Date endTime = insituAccessor.getHistoryEnd();
        observation.setTime(TimeUtil.getCenterTime(startTime, endTime));
        observation.setTimeRadius(TimeUtil.getTimeRadius(startTime, endTime));

        final double startLon = GeometryUtil.normalizeLongitude(insituAccessor.getStartLon());
        final double startLat = insituAccessor.getStartLat();
        final double endLon = GeometryUtil.normalizeLongitude(insituAccessor.getEndLon());
        final double endLat = insituAccessor.getEndLat();
        if (isNotOnPlanet(startLat, endLat)) {
            throw new IOException(String.format("latitude attributes (%g .. %g) out of range (-90.0 .. 90.0)", startLat, endLat));
        }
        observation.setLocation(createLineGeometry(startLon, startLat, endLon, endLat));
        return observation;
    }


    @Override
    public List<SamplingPoint> readSamplingPoints() throws IOException {
        return insituAccessor.readSamplingPoints();
    }

    @Override
    public final Array read(String role, ExtractDefinition extractDefinition) throws IOException {
        final Variable sourceVariable = insituAccessor.getVariable(role);
        final Date refTime = extractDefinition.getDate();
        final Range range = insituAccessor.find12HoursRange(refTime);
        final Array source = sourceVariable.read();

        final Array target = Array.factory(source.getElementType(), extractDefinition.getShape());
        final Number fillValue = getAttribute(sourceVariable, "_FillValue", Short.MIN_VALUE);
        for (int i = 0; i < target.getSize(); i++) {
            target.setObject(i, fillValue);
        }

        if (range != Range.EMPTY) {
            final List<Range> subsampling = insituAccessor.createSubsampling(range, extractDefinition.getShape()[1]);
            try {
                extractSubset(source, target, subsampling);
            } catch (InvalidRangeException e) {
                throw new IOException("Unable to create target.", e);
            }
        }
        if (role.equals("time")) {
            insituAccessor.scaleTime(target);
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

    private InsituAccessor createInsituAccessor() throws IOException {
        final String title = getGlobalAttribute("title");
        if (title == null) {
            throw new IOException("Illegal file format");
        }
        if (title.contains("SST CCI Phase II")) {
            return new Insitu_CCI_2_Accessor(this);
        } else {
            return new Insitu_CCI_1_Accessor(this);
        }
    }

    // package access for testing only tb 2014-02-06
    static boolean isNotOnPlanet(double startLat, double endLat) {
        return startLat < -90.0 || startLat > 90.0 || endLat < -90.0 || endLat > 90.0;
    }

    static void extractSubset(Array source, Array subset, List<Range> subsetRanges) throws InvalidRangeException {
        // setup ranges for copying
        final List<Range> sourceRanges = new ArrayList<>(source.getRank());
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

    private static PGgeometry createLineGeometry(double startLon, double startLat, double endLon, double endLat) {
        double startLon1 = normalizeLon(startLon);
        double endLon1 = normalizeLon(endLon);

        return new PGgeometry(new LineString(new Point[]{new Point(startLon1, startLat), new Point(endLon1, endLat)}));
    }

    // this is a copy of {@code GeoPos.normalizeLon()} using {@code double} instead of {@code float} as argument
    // package access for testing only tb 2014-01-29
    static double normalizeLon(double lon) {
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
