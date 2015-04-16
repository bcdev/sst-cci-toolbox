/*
 * Copyright (c) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.util.ByteConversion;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Ralf Quast
 */
final class Amsr2ScanDataQuality extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.BYTE;
    private static final byte FILL_VALUE = 0;

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws
                                                                                                     RuleException {
        targetColumnBuilder.type(DATA_TYPE);
        targetColumnBuilder.unsigned(true);
        targetColumnBuilder.fillValue(FILL_VALUE);
    }

    @Override
    public final Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Variable targetVariable = getContext().getTargetVariable();
        final int ny = targetVariable.getShape(1);
        final int nx = targetVariable.getShape(2);
        final Array targetArray = Array.factory(DATA_TYPE, new int[]{1, ny, nx});
        Arrays.fill((byte[]) targetArray.getStorage(), FILL_VALUE);
        readScanDataQuality(nx, ny, targetArray);
        return targetArray;
    }

    private void readScanDataQuality(int nx, int ny, Array array) throws RuleException {
        final Context context = getContext();
        final Reader reader = context.getObservationReader();
        if (reader == null) {
            return;
        }

        final Matchup matchup = context.getMatchup();
        final ReferenceObservation referenceObservation = matchup.getRefObs();
        final Observation observation = context.getObservation();
        final int recordNo = observation.getRecordNo();
        final Point point = referenceObservation.getPoint().getGeometry().getFirstPoint();
        final double lon = point.getX();
        final double lat = point.getY();
        final GeoPos geoPos = new GeoPos((float) lat, (float) lon);

        try {
            final GeoCoding geoCoding = reader.getGeoCoding(recordNo);
            if (geoCoding == null) {
                throw new RuleException("Cannot read scan data quality.");
            }
            final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
            if (!pixelPos.isValid()) {
                throw new RuleException("Cannot read scan data quality.");
            }
            final int pixelY = (int) pixelPos.y;
            final int[] ints = new int[ny * (nx / 4)];
            final Product product = reader.getProduct();
            if (product == null) {
                throw new RuleException("Cannot read scan data quality.");
            }
            final Band band = product.getBand("scan_data_quality");
            if (band == null) {
                throw new RuleException("Cannot read scan data quality.");
            }
            band.readPixels(0, pixelY - ny / 2, nx / 4, ny, ints);
            final byte[] bytes = ByteConversion.intsToBytes(ints);
            for (int i = 0; i < bytes.length; i++) {
                array.setByte(i, bytes[i]);
            }
        } catch (IOException e) {
            throw new RuleException("Cannot read scan data quality.", e);
        }
    }

}
