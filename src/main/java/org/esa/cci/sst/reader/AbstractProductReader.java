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

import com.bc.ceres.core.Assert;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.ProductUtils;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Base class for product IO handlers.
 *
 * @author Ralf Quast
 */
abstract class AbstractProductReader implements Reader {

    private final String sensorName;
    private final String[] formatNames;

    private DataFile datafile;
    private Product product;

    static {
        System.setProperty("beam.pixelGeoCoding.useTiling", "true");
        System.setProperty("beam.pixelGeoCoding.fractionAccuracy", "true");
    }

    protected AbstractProductReader(String sensorName, String... formatNames) {
        this.sensorName = sensorName;
        this.formatNames = formatNames;
    }

    @Override
    public final void init(DataFile datafile) throws IOException {
        Assert.state(product == null, "product != null");

        this.product = readProduct(datafile);
        this.datafile = datafile;
    }

    @Override
    public final void close() {
        if (product != null) {
            product.dispose();
        }
        datafile = null;
        product = null;
    }

    public final Product getProduct() {
        return product;
    }

    @Override
    public final DataFile getDatafile() {
        return datafile;
    }

    public final String getSensorName() {
        return sensorName;
    }

    @Override
    public final int getNumRecords() {
        return 1;
    }

    @Override
    public abstract Observation readObservation(int recordNo) throws IOException;

    @Override
    public Array read(String role, ExtractDefinition extractDefinition) {
        Assert.argument(role != null, "role == null");
        Assert.argument(extractDefinition != null, "extractDefinition == null");
        Assert.state(product != null, "product == null");

        final RasterDataNode node;
        if (product.containsBand(role)) {
            node = product.getBand(role);
        } else {
            node = product.getTiePointGrid(role);
        }
        if (node == null) {
            return null;
        }

        @SuppressWarnings({"ConstantConditions"})
        final double lon = extractDefinition.getLon();
        final double lat = extractDefinition.getLat();
        final int[] shape = extractDefinition.getShape();

        final PixelPos p = findPixelPos(lon, lat);
        final Rectangle rectangle = createSubsceneRectangle(p, shape);
        return readSubsceneData(node, shape, rectangle);
    }

    @Override
    public Item getColumn(String role) {
        Assert.state(product != null, "product == null");
        final RasterDataNode node = product.getRasterDataNode(role);
        if (node != null) {
            return createColumn(node);
        }
        return null;
    }

    @Override
    public final Item[] getColumns() {
        Assert.state(product != null, "product == null");
        final List<Item> columnList = new ArrayList<Item>();
        for (final RasterDataNode node : product.getTiePointGrids()) {
            final Item column = createColumn(node);
            columnList.add(column);
        }
        for (final RasterDataNode node : product.getBands()) {
            final Item column = createColumn(node);
            columnList.add(column);
        }
        return columnList.toArray(new Item[columnList.size()]);
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos) throws IOException {
        return product.getGeoCoding().getPixelPos(geoPos, null);
    }

    @Override
    public int getDTime(int recordNo, int scanLine) throws IOException {
        final Date date = product.getStartTime().getAsDate();
        final int julian = (int) TimeUtil.toJulianDate(date);
        return getTime(recordNo, scanLine) - julian;
    }

    @Override
    public int getTime(int recordNo, int scanLine) throws IOException {
        final ProductData.UTC utc = ProductUtils.getScanLineTime(product, scanLine);
        return (int) TimeUtil.toJulianDate(utc.getAsDate());
    }

    @Override
    public GeoCoding getGeoCoding(int recordNo) {
        return product.getGeoCoding();
    }

    protected Product readProduct(DataFile dataFile) throws IOException {
        Product product = ProductIO.readProduct(new File(dataFile.getPath()), formatNames);
        if (product == null) {
            throw new IOException(
                    MessageFormat.format("Cannot read product file ''{0}''.", dataFile.getPath()));
        }
        return product;
    }

    protected final Date getCenterTimeAsDate() throws IOException {
        final ProductData.UTC startTime = product.getStartTime();
        if (startTime == null) {
            throw new IOException("Unable to get start time for product '" + product.getName() + "'.");
        }
        final ProductData.UTC endTime = product.getEndTime();
        if (endTime == null) {
            return startTime.getAsDate();
        }
        final ProductData.UTC centerTime = new ProductData.UTC(0.5 * (startTime.getMJD() + endTime.getMJD()));
        return centerTime.getAsDate();
    }

    private Item createColumn(final RasterDataNode node) {
        final ColumnBuilder builder = new ColumnBuilder();
        builder.name(sensorName + "." + node.getName());
        builder.type(DataTypeUtils.getNetcdfDataType(node.getDataType()));
        builder.unsigned(ProductData.isUIntType(node.getDataType()));
        builder.rank(3);
        builder.dimensions("record ny nx");
        final String unit = node.getUnit();
        if (unit != null && !unit.isEmpty()) {
            builder.unit(unit);
        }
        if (node instanceof Band) {
            final Band band = (Band) node;
            final FlagCoding flagCoding = band.getFlagCoding();
            if (flagCoding != null) {
                final String[] meanings = flagCoding.getFlagNames();
                final StringBuilder masksStringBuilder = new StringBuilder();
                final StringBuilder meaningsStringBuilder = new StringBuilder();
                for (final String meaning : meanings) {
                    if (meaningsStringBuilder.length() > 0) {
                        masksStringBuilder.append(" ");
                        meaningsStringBuilder.append(" ");
                    }
                    final int flagMask = flagCoding.getFlagMask(meaning);
                    masksStringBuilder.append(Integer.toString(flagMask));
                    meaningsStringBuilder.append(meaning);
                }
                builder.flagMasks(masksStringBuilder.toString());
                builder.flagMeanings(meaningsStringBuilder.toString());
            }
        }
        if (node.isScalingApplied()) {
            builder.addOffset(node.getScalingOffset());
            builder.scaleFactor(node.getScalingFactor());
        }
        if (node.isNoDataValueUsed()) {
            builder.fillValue(node.getNoDataValue());
        }
        builder.role(node.getName());
        builder.sensor(datafile.getSensor());

        return builder.build();
    }

    private PixelPos findPixelPos(double lon, double lat) {
        final GeoPos geoPos = new GeoPos((float) lat, (float) lon);
        if (!geoPos.isValid()) {
            throw new ToolException("Geo-location of reference point is invalid.", ToolException.TOOL_ERROR);
        }
        final GeoCoding geoCoding = product.getGeoCoding();
        final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, new PixelPos());
        if (!pixelPos.isValid()) {
            final Logger logger = Logger.getLogger("org.esa.cci.sst");
            final String message = MessageFormat.format("Unable to find pixel at ({0}, {1}) in product ''{2}''.",
                                                        geoPos.getLon(),
                                                        geoPos.getLat(),
                                                        product.getName());
            logger.fine(message);
        }
        return pixelPos;
    }

    private static Rectangle createSubsceneRectangle(Point2D p, int[] shape) {
        final int w = shape[2];
        final int h = shape[1];
        final int x = (int) Math.floor(p.getX()) - w / 2;
        final int y = (int) Math.floor(p.getY()) - h / 2;

        return new Rectangle(x, y, w, h);
    }

    private static Array readSubsceneData(RasterDataNode node, int[] shape, Rectangle rectangle) {
        final Array targetArray = Array.factory(DataTypeUtils.getNetcdfDataType(node.getDataType()), shape);

        final RenderedImage sourceImage = node.getSourceImage().getImage(0);
        final int minX = sourceImage.getMinX();
        final int minY = sourceImage.getMinY();

        final Rectangle imageRectangle = new Rectangle(minX, minY, sourceImage.getWidth(), sourceImage.getHeight());
        final Rectangle validRectangle = imageRectangle.intersection(rectangle);
        if (validRectangle.isEmpty()) {
            for (int i = 0; i < targetArray.getSize(); i++) {
                targetArray.setObject(i, node.getNoDataValue());
            }
        } else {
            final Raster raster = sourceImage.getData(validRectangle);
            if (validRectangle.equals(rectangle)) {
                raster.getDataElements(rectangle.x, rectangle.y, rectangle.width, rectangle.height,
                                       targetArray.getStorage());
            } else {
                final int maxX = minX + sourceImage.getWidth() - 1;
                final int maxY = minY + sourceImage.getHeight() - 1;
                for (int i = rectangle.y, k = 0; i < rectangle.y + rectangle.height; i++) {
                    for (int j = rectangle.x; j < rectangle.x + rectangle.width; j++, k++) {
                        final Number value;
                        if (i < minY || i > maxY || j < minX || j > maxX) {
                            value = node.getNoDataValue();
                        } else {
                            value = getSample(raster, j, i);
                        }
                        targetArray.setObject(k, value);
                    }
                }
            }
        }
        return targetArray;
    }

    private static Number getSample(Raster raster, int x, int y) {
        switch (raster.getTransferType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_INT:
                return raster.getSample(x, y, 0);
            case DataBuffer.TYPE_FLOAT:
                return raster.getSampleFloat(x, y, 0);
            case DataBuffer.TYPE_DOUBLE:
                return raster.getSampleDouble(x, y, 0);
            default:
                throw new IllegalArgumentException("Unsupported transfer type " + raster.getTransferType() + ".");
        }
    }
}
