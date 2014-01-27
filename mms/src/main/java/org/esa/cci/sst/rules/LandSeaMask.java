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
import org.esa.beam.watermask.operator.WatermaskClassifier;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tools.ToolException;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * Sets the land/sea mask.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
class LandSeaMask extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.BYTE;
    private final WatermaskClassifier classifier;

    LandSeaMask() {
        classifier = createWatermaskClassifier();
    }

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        targetColumnBuilder.type(DATA_TYPE);
        targetColumnBuilder.fillValue((byte) WatermaskClassifier.INVALID_VALUE);
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Variable targetVariable = getContext().getTargetVariable();
        if (getContext().getObservationReader() == null) {
            final int[] shape = targetVariable.getShape();
            shape[0] = 1;
            return createFilledArray(shape);
        }
        final int recordNo = getContext().getObservation().getRecordNo();
        return readLandSeaMask(targetVariable, recordNo);
    }

    private Array readLandSeaMask(Variable targetVariable, int recordNo) throws RuleException {
        final int[] shape = targetVariable.getShape();
        shape[0] = 1;
        final GeoCoding geoCoding;
        final Reader observationReader = getContext().getObservationReader();
        try {
            geoCoding = observationReader.getGeoCoding(recordNo);
        } catch (IOException e) {
            throw new RuleException("Unable to create geo-coding.", e);
        }
        final Point point = getContext().getMatchup().getRefObs().getPoint().getGeometry().getFirstPoint();
        final double lon = point.getX();
        final double lat = point.getY();
        if (lat < -60.0f) {
            final Array targetArray = Array.factory(DataType.BYTE, shape);
            for (int i = 0; i < targetArray.getSize(); i++) {
                targetArray.setByte(i, (byte) 100);
            }
            return targetArray;
        }
        final PixelPos pixelPos = new PixelPos();
        geoCoding.getPixelPos(new GeoPos((float) lat, (float) lon), pixelPos);
        final Array targetArray = Array.factory(DataType.BYTE, shape);
        final Index index = targetArray.getIndex();
        final int minX = (int) (pixelPos.x) - shape[2] / 2;
        final int maxX = (int) (pixelPos.x) + shape[2] / 2;
        final int minY = (int) (pixelPos.y) - shape[1] / 2;
        final int maxY = (int) (pixelPos.y) + shape[1] / 2;
        int xi = 0;
        int yi = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (x >= 0 && y >= 0 && x < observationReader.getElementCount() && y < observationReader.getScanLineCount()) {
                    index.set(0, yi, xi);
                    final byte fraction = classifier.getWaterMaskFraction(geoCoding, x, y);
                    targetArray.setByte(index, fraction);
                }
                yi++;
            }
            yi = 0;
            xi++;
        }
        return targetArray;
    }

    private WatermaskClassifier createWatermaskClassifier() {
        try {
            // TODO - use no hard-coded value, but let subsampling depend on resolution of source image
            return new WatermaskClassifier(WatermaskClassifier.RESOLUTION_50, WatermaskClassifier.MODE_GC, 11, 11);
        } catch (IOException e) {
            throw new ToolException("Unable to create watermask classifier.", e, ToolException.UNKNOWN_ERROR);
        }
    }

    private Array createFilledArray(int[] shape) {
        final Array fillArray = Array.factory(DataType.BYTE, shape);
        final Index index = fillArray.getIndex();
        for (int x = 0; x < shape[1]; x++) {
            for (int y = 0; y < shape[2]; y++) {
                index.set(0, x, y);
                fillArray.setByte(index, (byte) WatermaskClassifier.INVALID_VALUE);
            }
        }
        return fillArray;
    }

}
