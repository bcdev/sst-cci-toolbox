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

package org.esa.cci.sst.rules;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tools.mmdgeneration.DetectorTemperatureProvider;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Sets the detector temperature.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"UnusedDeclaration"})
final class DetectorTemperature extends Rule {

    private static final double SCALE_FACTOR = 0.01;
    private static final short FILL_VALUE = Short.MIN_VALUE;
    private static final DataType DATA_TYPE = DataType.SHORT;

    @Override
    public Item apply(Item sourceColumn) throws RuleException {
        return new ColumnBuilder(sourceColumn)
                .type(DATA_TYPE)
                .scaleFactor(SCALE_FACTOR)
                .addOffset(0.0)
                .fillValue(FILL_VALUE)
                .unit("K")
                .build();
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Context context = getContext();
        final Array result = Array.factory(DataType.SHORT, new int[]{1});
        final Observation observation = context.getObservation();
        final Reader observationReader = context.getObservationReader();
        if (observation == null || observationReader == null) {
            result.setShort(0, FILL_VALUE);
            return result;
        }

        final long time = getTime();
        final Date date = new Date(time);
        float detectorTemperature = DetectorTemperatureProvider.create().getDetectorTemperature(date);
        if (detectorTemperature == DetectorTemperatureProvider.FILL_VALUE) {
            result.setShort(0, FILL_VALUE);
        } else {
            detectorTemperature /= SCALE_FACTOR;
            result.setShort(0, (short) detectorTemperature);
        }
        return result;
    }

    private int getScanline() {
        int numScanlines = getNumScanlines();
        return Math.round(numScanlines / 2);
    }

    private int getNumScanlines() {
        final Map<String, Integer> dimensionConfiguration = getContext().getDimensionConfiguration();
        return dimensionConfiguration.get(AtsrImageDimensions.DIMENSIONS_NAME_ATSR_NY);
    }

    private long getTime(int recordNo, int scanline) throws RuleException {
        try {
            return getContext().getObservationReader().getTime(recordNo, scanline);
        } catch (IOException e) {
            throw new RuleException(e);
        }
    }

    private long getTime() throws RuleException {
        final Context context = getContext();
        final Reader reader = context.getObservationReader();
        if (reader == null) {
            throw new RuleException("Cannot read time. observationReader is null");
        }
        final ReferenceObservation refObs = context.getMatchup().getRefObs();
        final int recordNo = context.getObservation().getRecordNo();
        final Point point = refObs.getPoint().getGeometry().getFirstPoint();
        final double lon = point.getX();
        final double lat = point.getY();
        final GeoPos geoPos = new GeoPos((float) lat, (float) lon);
        final long time;
        try {
            final int scanLine = (int) reader.getGeoCoding(recordNo).getPixelPos(geoPos, null).y;
            time = reader.getTime(recordNo, scanLine);
        } catch (IOException e) {
            throw new RuleException("Cannot read time.", e);
        }
        return time;
    }
}
