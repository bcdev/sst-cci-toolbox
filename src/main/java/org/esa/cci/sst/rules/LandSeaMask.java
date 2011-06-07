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

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.AbstractGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.QuadTreePixelLocator;
import org.esa.beam.util.SampleSource;
import org.esa.beam.util.VariableSampleSource;
import org.esa.beam.watermask.operator.WatermaskClassifier;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.reader.ExtractDefinition;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.ExtractDefinitionBuilder;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Variable;

import java.awt.geom.Point2D;
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
        final int recordNo = getContext().getMatchup().getRefObs().getRecordNo();
        if (getContext().getObservationReader() == null) {
            final int[] shape = targetVariable.getShape();
            shape[0] = 1;
            return createFilledArray(shape);
        }
        return readLandSeaMask(targetVariable, recordNo);
    }

    private Array readLandSeaMask(Variable targetVariable, int recordNo) throws RuleException {
        final int[] shape = targetVariable.getShape();
        shape[0] = 1;
        final GeoCoding geoCoding = createGeoCoding(recordNo, shape);
        final Array targetArray = Array.factory(DataType.BYTE, shape);
        final PixelPos pixelPos = new PixelPos();
        final Index index = targetArray.getIndex();
        for (int x = 0; x < shape[1]; x++) {
            for (int y = 0; y < shape[2]; y++) {
                pixelPos.setLocation(x, y);
                index.set(0, x, y);
                final byte fraction = classifier.getWaterMaskFraction(geoCoding, pixelPos, 5, 5);
                targetArray.setByte(index, fraction);
            }
        }
        return targetArray;
    }

    private GeoCoding createGeoCoding(int recordNo, int[] shape) throws RuleException {
        final ExtractDefinition extractDefinition = new ExtractDefinitionBuilder()
                .referenceObservation(getContext().getMatchup().getRefObs())
                .recordNo(recordNo)
                .shape(shape)
                .build();
        final Reader reader = getContext().getObservationReader();
        String longitudeVariableName = "lon";
        if (reader.getColumn(longitudeVariableName) == null) {
            longitudeVariableName = "longitude";
        }
        String latitudeVariableName = "lat";
        if (reader.getColumn(latitudeVariableName) == null) {
            latitudeVariableName = "latitude";
        }
        Array lonArray;
        Array latArray;
        try {
            lonArray = reader.read(longitudeVariableName, extractDefinition);
            latArray = reader.read(latitudeVariableName, extractDefinition);
            lonArray = scale(reader.getColumn(longitudeVariableName).getScaleFactor(), lonArray);
            latArray = scale(reader.getColumn(latitudeVariableName).getScaleFactor(), latArray);
        } catch (IOException e) {
            throw new RuleException("Could not create geo coding.", e);
        }
        return new LSGeoCoding(new VariableSampleSource(lonArray), new VariableSampleSource(latArray));
    }

    private Array scale(Number scaleFactor, Array array) {
        if(scaleFactor == null) {
            return array;
        }
        final Array scaledArray = Array.factory(DataType.DOUBLE, array.getShape());
        for (int i = 0; i < array.getSize(); i++) {
            double value = ((Number) array.getObject(i)).doubleValue() * scaleFactor.doubleValue();
            scaledArray.setDouble(i, value);
        }
        return scaledArray;
    }

    private WatermaskClassifier createWatermaskClassifier() {
        try {
            return new WatermaskClassifier(WatermaskClassifier.RESOLUTION_50);
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

    @SuppressWarnings({"deprecation"})
    private static class LSGeoCoding extends AbstractGeoCoding {

        private final QuadTreePixelLocator locator;

        LSGeoCoding(SampleSource lonSource, SampleSource latSource) {
            this.locator = new QuadTreePixelLocator(lonSource, latSource);
        }

        @Override
        public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
            return false;
        }

        @Override
        public boolean isCrossingMeridianAt180() {
            return false;
        }

        @Override
        public boolean canGetPixelPos() {
            return false;
        }

        @Override
        public boolean canGetGeoPos() {
            return true;
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            return null;
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            final Point2D.Double result = new Point2D.Double();
            locator.getGeoLocation(pixelPos.x, pixelPos.y, result);
            geoPos.setLocation((float) result.y, (float) result.x);
            return geoPos;
        }

        @Override
        public Datum getDatum() {
            return null;
        }

        @Override
        public void dispose() {
        }
    }

}
