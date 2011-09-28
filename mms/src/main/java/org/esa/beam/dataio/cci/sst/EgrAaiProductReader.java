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

package org.esa.beam.dataio.cci.sst;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.UnpackedImageData;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class EgrAaiProductReader extends AbstractProductReader {

    private static final int COL_COUNT = 288;
    private static final int ROW_COUNT = 180;

    private static final double SCALING_OFFSET = 450.0;
    private static final double SCALING_FACTOR = 10.0;
    private static final Number NO_DATA_VALUE = 999;

    protected EgrAaiProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File file = getInputFile();
        final Product product = new Product(file.getName(), "AerosolAai", COL_COUNT, ROW_COUNT);
        product.setPreferredTileSize(COL_COUNT, ROW_COUNT);
        product.setFileLocation(file);

        final ProductData.UTC startTime = readReferenceTime(file);
        product.setStartTime(startTime);
        final ProductData.UTC endTime = new ProductData.UTC(startTime.getDaysFraction() + 1, 0, 0);
        product.setEndTime(endTime);

        final GeoCoding geoCoding = createGeoCoding(new Dimension(COL_COUNT, ROW_COUNT));
        product.setGeoCoding(geoCoding);

        final Band band = product.addBand("absorbing_aerosol_index", ProductData.TYPE_INT16);
        band.setScalingOffset(SCALING_OFFSET);
        band.setScalingFactor(SCALING_FACTOR);
        band.setNoDataValue(NO_DATA_VALUE.doubleValue());
        band.setNoDataValueUsed(true);
        band.setSourceImage(createSourceImage(band.getDataType(),
                                              band.getSceneRasterWidth(),
                                              band.getSceneRasterHeight(),
                                              createSampleReader(file)));

        return product;
    }

    @Override
    protected final void readBandRasterDataImpl(int sourceOffsetX,
                                                int sourceOffsetY,
                                                int sourceWidth,
                                                int sourceHeight,
                                                int sourceStepX,
                                                int sourceStepY,
                                                Band targetBand,
                                                int targetOffsetX,
                                                int targetOffsetY,
                                                int targetWidth,
                                                int targetHeight,
                                                ProductData targetBuffer,
                                                ProgressMonitor pm) {
        synchronized (this) {
            final RenderedImage image = targetBand.getSourceImage();
            final Raster data = image.getData(new Rectangle(targetOffsetX, targetOffsetY, targetWidth, targetHeight));

            data.getDataElements(targetOffsetX, targetOffsetY, targetWidth, targetHeight, targetBuffer.getElems());
        }
    }

    private File getInputFile() {
        return new File(getInput().toString());
    }

    static GeoCoding createGeoCoding(Dimension dimension) {
        final double scaleX = 360.0 / dimension.getWidth();
        final double scaleY = 180.0 / dimension.getHeight();
        final AffineTransform transform = new AffineTransform();
        transform.translate(-180.0, -90.0);
        transform.scale(scaleX, scaleY);

        try {
            return new CrsGeoCoding(DefaultGeographicCRS.WGS84, new Rectangle(dimension), transform);
        } catch (FactoryException e) {
            throw new IllegalArgumentException("dimension");
        } catch (TransformException e) {
            throw new IllegalArgumentException("dimension");
        }
    }

    static SampleReader createSampleReader(final File file) {
        return new SampleReader() {
            @Override
            public Number[] readAllSamples() {
                return readSamples(file);
            }
        };
    }

    static RenderedImage createSourceImage(int dataType, int w, int h, SampleReader sampleReader) {
        return new SingleTileOpImage(dataType, w, h, sampleReader);
    }

    static ProductData.UTC readReferenceTime(File file) throws IOException {
        Scanner scanner = null;
        try {
            scanner = new Scanner(file, "US-ASCII");
            scanner.useLocale(Locale.US);

            final String text = scanner.next("\\d\\d\\d\\d-\\d\\d-\\d\\d");
            try {
                return ProductData.UTC.parse(text, "yyyy-MM-dd");
            } catch (ParseException e) {
                throw new IOException(MessageFormat.format(
                        "Cannot read reference time for file ''{0}'',", file.getPath()));
            }
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    static Number[] readSamples(File file) {
        final Number[] samples = new Number[COL_COUNT * ROW_COUNT];
        Arrays.fill(samples, NO_DATA_VALUE);

        Scanner scanner = null;
        try {
            scanner = new Scanner(file, "US-ASCII");
            scanner.useLocale(Locale.US);

            scanner.nextLine();
            scanner.nextLine();
            scanner.nextLine();
            scanner.useDelimiter("\\s*lat\\s=\\s.*\\n");

            for (int i = 0; i < ROW_COUNT; i++) {
                final String line = scanner.next().replaceAll("\\s", "");
                for (int k = 0, index = 0; k < COL_COUNT; k++, index += 3) {
                    try {
                        samples[i * COL_COUNT + k] = Short.parseShort(line.substring(index, index + 3));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return samples;
    }

    interface SampleReader {

        Number[] readAllSamples();
    }

    private static class SingleTileOpImage extends SingleBandedOpImage {

        private final SampleReader sampleReader;

        private SingleTileOpImage(int dataType, int w, int h, SampleReader sampleReader) {
            super(ImageManager.getDataBufferType(dataType),
                  w,
                  h,
                  new Dimension(w, h),
                  null,
                  // no configuration
                  new ResolutionLevel(0, 1.0));
            this.sampleReader = sampleReader;
        }

        @Override
        protected final void computeRect(PlanarImage[] planarImages, WritableRaster targetRaster, Rectangle rectangle) {
            final PixelAccessor targetAccessor = new PixelAccessor(targetRaster.getSampleModel(), null);
            final UnpackedImageData targetData = targetAccessor.getPixels(
                    targetRaster, rectangle, targetRaster.getSampleModel().getTransferType(), true);

            switch (targetData.type) {
                case DataBuffer.TYPE_SHORT:
                case DataBuffer.TYPE_USHORT:
                    shortLoop(targetData);
                    break;
                default:
                    // do nothing
                    break;
            }

            targetAccessor.setPixels(targetData);
        }

        private void shortLoop(UnpackedImageData targetData) {
            final int w = targetData.rect.width;
            final int h = targetData.rect.height;
            Assert.state(w == getWidth());
            Assert.state(h == getHeight());
            final Number[] samples = sampleReader.readAllSamples();

            final int targetPixelStride = targetData.pixelStride;
            final int targetLineStride = targetData.lineStride;
            @SuppressWarnings({"MismatchedReadAndWriteOfArray"})
            final short[] targetDataArray = targetData.getShortData(0);
            int targetLineOffset = targetData.getOffset(0);

            for (int y = 0; y < h; ++y) {
                int targetPixelOffset = targetLineOffset;
                for (int x = 0; x < w; ++x) {
                    targetDataArray[targetPixelOffset] = samples[y * w + x].shortValue();
                    targetPixelOffset += targetPixelStride;
                }
                targetLineOffset += targetLineStride;
            }
        }
    }
}
