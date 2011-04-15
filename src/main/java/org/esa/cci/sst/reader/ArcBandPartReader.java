/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ImageUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;

/**
 * @author Thomas Storm
 */
public class ArcBandPartReader extends CfBandPart {

    private static final String LONGITUDE_BAND_NAME = "lon";

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        super.decode(ctx, p);
        final Band lon = p.getBand(LONGITUDE_BAND_NAME);
        final MultiLevelImage sourceImage = lon.getSourceImage();
        final SourcelessOpImage image = createOpImage(lon, sourceImage);
        lon.setSourceImage(image);
    }

    private SourcelessOpImage createOpImage(final Band lon, final MultiLevelImage sourceImage) {
        final MultiLevelImage lonImage = lon.getSourceImage();
        int width = lonImage.getWidth();
        int height = lonImage.getHeight();
        final ImageLayout layout = ImageManager.createSingleBandedImageLayout(DataBuffer.TYPE_FLOAT,
                                                                              width, height, width, height);
        final SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(DataBuffer.TYPE_FLOAT, width, height);
        return new LongitudeNormaliseImage(layout, sampleModel, width, height, sourceImage);
    }

    private static class LongitudeNormaliseImage extends SourcelessOpImage {

        private static final double FILL_VALUE = -1.0E30;
        private final Raster data;

        LongitudeNormaliseImage(final ImageLayout layout, final SampleModel sampleModel, final int width,
                                final int height, final MultiLevelImage sourceImage) {
            super(layout, null, sampleModel, 0, 0, width, height);
            data = sourceImage.getData();
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
            final float sourceSample = data.getSampleFloat(x, y, 0);
            if(sourceSample == FILL_VALUE) {
                return Float.NaN;
            }
            float normalisedLon = sourceSample + 180.0f;
            normalisedLon %= 360.0;
            normalisedLon -= 180.0;
            return normalisedLon;
        }

    }
}
