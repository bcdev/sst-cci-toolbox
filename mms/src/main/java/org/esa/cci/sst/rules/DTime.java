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
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tools.Constants;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Arrays;

/**
 * Sets dtime.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
final class DTime extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.SHORT;
    private static final short FILL_VALUE = Short.MIN_VALUE;

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws
                                                                                                     RuleException {
        targetColumnBuilder.type(DATA_TYPE).unit(Constants.UNIT_DTIME);
        targetColumnBuilder.fillValue(FILL_VALUE);
    }

    @Override
    public final Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Variable targetVariable = getContext().getTargetVariable();
        final int scanLineCount = targetVariable.getShape(1);
        final Array targetArray = Array.factory(DATA_TYPE, new int[]{1, scanLineCount});
        Arrays.fill((short[]) targetArray.getStorage(), FILL_VALUE);
        getDTime(targetArray);
        return targetArray;
    }

    private void getDTime(Array array) throws RuleException {
        final Context context = getContext();
        final Reader reader = context.getObservationReader();
        if (reader == null) {
            return;
        }
        final ReferenceObservation refObs = context.getMatchup().getRefObs();
        final int recordNo = context.getObservation().getRecordNo();
        final Point point = refObs.getPoint().getGeometry().getFirstPoint();
        final double lon = point.getX();
        final double lat = point.getY();
        final GeoPos geoPos = new GeoPos((float) lat, (float) lon);
        try {
            final int refScanLine = (int) reader.getGeoCoding(recordNo).getPixelPos(geoPos, null).y;
            final long time = reader.getTime(recordNo, refScanLine);
            for (int i = 0; i < array.getSize(); i++) {
                final int scanLine = (int) (refScanLine - array.getSize() / 2) + i;
                if (scanLine >= 0 && scanLine < reader.getScanLineCount()) {
                    array.setShort(i, (short) (reader.getTime(recordNo, scanLine) - time));
                }
            }
        } catch (IOException e) {
            throw new RuleException("Cannot read dtime.", e);
        }
    }
}
