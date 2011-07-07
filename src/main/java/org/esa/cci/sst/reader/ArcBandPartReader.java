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

import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ImageUtils;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.List;

/**
 * @author Thomas Storm
 */
public class ArcBandPartReader extends CfBandPart {

    private static final String LONGITUDE_BAND_NAME = "lon";
    private static final String DIMENSION_NAME_WIDTH = "scan_elem";
    private static final String DIMENSION_NAME_HEIGHT = "scan_line";
    private static final double FILL_VALUE = -1.0E30;
    private final String locationFile;

    public ArcBandPartReader(final String locationFile) {
        this.locationFile = locationFile;
    }

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        super.decode(ctx, p);
        p.removeBand(p.getBand(LONGITUDE_BAND_NAME));
        final NetcdfFile file = NetcdfFile.open(locationFile);
        final Variable variable = file.findVariable(NetcdfFile.escapeName(LONGITUDE_BAND_NAME));
        final SourcelessOpImage image = createOpImage(file, variable);
        final Band lon = p.addBand(LONGITUDE_BAND_NAME, ProductData.TYPE_FLOAT32);
        final List<Variable> variables = file.getVariables();
        for (Variable variable1 : variables) {
            final Band band = p.getBand(variable1.getName());
            final Attribute missingValue = variable1.findAttribute("missing_value");
            if(band != null && missingValue != null) {
                band.setNoDataValue(missingValue.getNumericValue().doubleValue());
                band.setNoDataValueUsed(true);
            }
        }
        lon.setSourceImage(image);
    }

    private SourcelessOpImage createOpImage(final NetcdfFile file, final Variable lonVar) throws IOException {
        int width = file.findDimension(DIMENSION_NAME_WIDTH).getLength();
        int height = file.findDimension(DIMENSION_NAME_HEIGHT).getLength();
        final ImageLayout layout = ImageManager.createSingleBandedImageLayout(DataBuffer.TYPE_FLOAT,
                                                                              width, height, width, height);
        final SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(DataBuffer.TYPE_FLOAT, width, height);
        return new LongitudeNormaliseImage(layout, sampleModel, width, height, lonVar);
    }

    private static class LongitudeNormaliseImage extends SourcelessOpImage {

        private static final long TILE_CACHE_SIZE = 8 * 300 * 1024 * 1024L;
        private final Variable lonVar;

        LongitudeNormaliseImage(final ImageLayout layout, final SampleModel sampleModel, final int width,
                                final int height, final Variable lonVar) throws IOException {
            super(layout, null, sampleModel, 0, 0, width, height);
            this.lonVar = lonVar;
            setTileCache(JAI.createTileCache(TILE_CACHE_SIZE));
        }

        @Override
        protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
            for (int x = (int) destRect.getMinX(); x < destRect.getMaxX(); x++) {
                for (int y = (int) destRect.getMinY(); y < destRect.getMaxY(); y++) {
                    float value = computeValue(x, y);
                    dest.setSample(x, y, 0, value);
                }
            }
        }

        @Override
        protected void computeRect(final PlanarImage[] sources, final WritableRaster dest, final Rectangle destRect) {
            computeRect(new Raster[0], dest, destRect);
        }

        private float computeValue(final int x, final int y) {
            float sourceSample = getSourceSample(x, y);
            if (sourceSample == FILL_VALUE) {
                return Float.NaN;
            }
            float normalisedLon = sourceSample + 180.0f;
            normalisedLon %= 360.0;
            normalisedLon -= 180.0;
            return normalisedLon;
        }

        private float getSourceSample(final int x, final int y) {
            final float sourceSample;
            try {
                sourceSample = lonVar.read(new int[]{y, x}, new int[]{1, 1}).getFloat(0);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return sourceSample;
        }
    }
}
