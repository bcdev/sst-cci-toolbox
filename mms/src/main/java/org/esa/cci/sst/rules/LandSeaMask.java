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
import org.esa.beam.framework.datamodel.PixelLocator;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.util.Watermask;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;

import java.awt.geom.Point2D;
import java.io.IOException;

/**
 * Sets the land/sea mask.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
final class LandSeaMask extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.BYTE;
    private final Watermask classifier;

    LandSeaMask() {
        classifier = new Watermask();
    }

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        targetColumnBuilder.type(DATA_TYPE);
        targetColumnBuilder.fillValue(Watermask.INVALID_WATER_FRACTION);
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final int[] shape = getContext().getTargetVariable().getShape();
        final int sizeY = shape[1];
        final int sizeX = shape[2];
        final Array targetArray = createTargetArray(sizeY, sizeX);

        final Reader observationReader = getContext().getObservationReader();
        if (observationReader == null) {
            return targetArray;
        }

        final int recordNo = getContext().getObservation().getRecordNo();
        final GeoCoding geoCoding;
        try {
            geoCoding = observationReader.getGeoCoding(recordNo);
        } catch (IOException ignored) {
            return targetArray;
        }

        final Point point = getContext().getMatchup().getRefObs().getPoint().getGeometry().getFirstPoint();
        final double lon = point.getX();
        final double lat = point.getY();
        final PixelLocator pixelLocator = new GeoCodingWrapper(geoCoding);
        final Point2D p = new Point2D.Double();
        final boolean found = pixelLocator.getPixelLocation(lon, lat, p);
        if (found) {
            final Index index = targetArray.getIndex();
            final int minX = (int) (p.getX()) - sizeX / 2;
            final int maxX = (int) (p.getX()) + sizeX / 2;
            final int minY = (int) (p.getY()) - sizeY / 2;
            final int maxY = (int) (p.getY()) + sizeY / 2;
            for (int y = minY, yi = 0; y <= maxY; y++, yi++) {
                if (y >= 0 && y < observationReader.getScanLineCount()) {
                    for (int x = minX, xi = 0; x <= maxX; x++, xi++) {
                        if (x >= 0 && x < observationReader.getElementCount()) {
                            targetArray.setByte(index.set(0, yi, xi), classifier.getWaterFraction(x, y, pixelLocator));
                        }
                    }
                }
            }
        }
        return targetArray;
    }

    private static Array createTargetArray(int sizeY, int sizeX) {
        final Array fillArray = Array.factory(DataType.BYTE, new int[]{1, sizeY, sizeX});
        final Index index = fillArray.getIndex();
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                index.set(0, y, x);
                fillArray.setByte(index, Watermask.INVALID_WATER_FRACTION);
            }
        }
        return fillArray;
    }

    private static class GeoCodingWrapper implements PixelLocator {

        private final GeoCoding geoCoding;
        private PixelPos pp;
        private GeoPos gp;

        public GeoCodingWrapper(GeoCoding geoCoding) {
            this.geoCoding = geoCoding;
            pp = new PixelPos();
            gp = new GeoPos();
        }

        @Override
        public boolean getGeoLocation(double x, double y, Point2D g) {
            if (geoCoding.canGetGeoPos()) {
                pp.setLocation(x, y);
                geoCoding.getGeoPos(pp, gp);
                if (gp.isValid()) {
                    g.setLocation(gp.getLon(), gp.getLat());
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean getPixelLocation(double lon, double lat, Point2D p) {
            if (geoCoding.canGetPixelPos()) {
                gp.setLocation((float) lat, (float) lon);
                geoCoding.getPixelPos(gp, pp);
                if (pp.isValid()) {
                    p.setLocation(pp);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void dispose() {

        }
    }
}
