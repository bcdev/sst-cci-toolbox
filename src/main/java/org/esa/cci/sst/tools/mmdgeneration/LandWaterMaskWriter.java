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

package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.watermask.operator.WatermaskClassifier;
import org.esa.beam.watermask.operator.WatermaskOp;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;

import java.awt.image.Raster;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes a land water mask band to a given netcdf file.
 * Prerequisites:
 * <ul>
 * <li>the target file has to comprise the correct structure already, that is</li>
 * <li>the target variable has to exist</li>
 * <li>the dimensions of the target variable have the same lengths as the product the water mask shall be written for</li>
 * </ul>
 *
 * @author Thomas Storm
 */
class LandWaterMaskWriter {

    private static final int SUBSAMPLING_FACTOR = 10;

    private final NetcdfFileWriteable file;

    LandWaterMaskWriter(final NetcdfFileWriteable file) throws IOException {
        this.file = file;
    }

    void writeLandWaterMask(Product product, int matchupIndex, String targetVariable) throws IOException {
        final String operatorName = OperatorSpi.getOperatorAlias(WatermaskOp.class);
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("resolution", WatermaskClassifier.RESOLUTION_50);
        parameters.put("subSamplingFactor", SUBSAMPLING_FACTOR);
        final Product maskProduct = GPF.createProduct(operatorName, parameters, product);
        final Band waterFractionBand = maskProduct.getBand("land_water_fraction");
        final Raster data = waterFractionBand.getSourceImage().getData();
        for (int x = 0; x < waterFractionBand.getSceneRasterWidth(); x++) {
            for (int y = 0; y < waterFractionBand.getSceneRasterHeight(); y++) {
                final int sample = data.getSample(x, y, 0);
                writeValue(targetVariable, Array.factory(sample), new int[]{matchupIndex, x, y});
            }
        }
    }

    private void writeValue(final String targetVariable, final Array value, final int[] origin) throws IOException {
        try {
            file.write(NetcdfFile.escapeName(targetVariable), origin, value);
        } catch (InvalidRangeException e) {
            throw new IOException(
                    MessageFormat.format("Unable to write into variable ''{0}''.", targetVariable));
        }
    }
}
