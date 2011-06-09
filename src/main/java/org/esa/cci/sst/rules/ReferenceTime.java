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
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;

/**
 * Sets the time of the reference observation.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
class ReferenceTime extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.INT;

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws
                                                                                                     RuleException {
        targetColumnBuilder.type(DATA_TYPE).unit(Constants.UNIT_TIME);
    }

    @Override
    public final Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array array = Array.factory(DATA_TYPE, new int[]{1});
        final long time = getTime();
        final int julianDate = (int) TimeUtil.millisToSecondsSinceEpoch(time);
        array.setInt(0, julianDate);
        return array;
    }

    private long getTime() throws RuleException {
        final Context context = getContext();
        final Reader reader = context.getReferenceObservationReader();
        final ReferenceObservation refObs = context.getMatchup().getRefObs();
        final int recordNo = refObs.getRecordNo();
        final Point point = refObs.getPoint().getGeometry().getFirstPoint();
        final double lon = point.getX();
        final double lat = point.getY();
        final GeoPos geoPos = new GeoPos((float) lat, (float) lon);
        final long time;
        try {
            final PixelPos pixelPos = reader.getGeoCoding(recordNo).getPixelPos(geoPos, null);
            final int scanLine = pixelPos != null ? (int) pixelPos.y : -1;
            time = reader.getTime(recordNo, scanLine);
        } catch (IOException e) {
            throw new RuleException("Unable to read time.", e);
        }
        return time;
    }

}
