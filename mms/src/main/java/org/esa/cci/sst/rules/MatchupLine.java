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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.Reader;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;

/**
 * Sets the matchup line.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"UnusedDeclaration"})
final class MatchupLine extends Rule {

    private static final int FILL_VALUE = -1;

    @Override
    public Item apply(Item sourceColumn) throws RuleException {
        return new ColumnBuilder(sourceColumn)
                .type(DataType.INT)
                .fillValue(FILL_VALUE)
                .build();
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array array = Array.factory(DataType.INT, new int[]{1});
        final int value = getValue();
        array.setInt(0, value);
        return array;
    }

    private int getValue() throws RuleException {
        final Context context = getContext();
        final ReferenceObservation refObs = context.getMatchup().getRefObs();
        final Point point = refObs.getPoint().getGeometry().getFirstPoint();
        final double lon = point.getX();
        final double lat = point.getY();
        final Reader observationReader = context.getObservationReader();
        if (observationReader == null) {
            return FILL_VALUE;
        }
        final GeoCoding geoCoding;
        try {
            geoCoding = observationReader.getGeoCoding(refObs.getRecordNo());
        } catch (IOException e) {
            throw new RuleException("Unable to obtain geo-coding.", e);
        }
        final PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos((float) lat, (float) lon), null);

        return (int) (pixelPos.getY() + observationReader.getLineSkip());
    }
}
