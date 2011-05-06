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

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Observation;
import org.postgis.PGgeometry;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import javax.naming.OperationNotSupportedException;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Base class for product IO handlers.
 *
 * @author Ralf Quast
 */
abstract class AbstractProductHandler implements IOHandler {

    private final String sensorName;
    private final String[] formatNames;

    private DataFile dataFile;
    private Product product;

    protected AbstractProductHandler(String sensorName, String... formatNames) {
        this.sensorName = sensorName;
        this.formatNames = formatNames;
    }

    private static Rectangle createSubsceneRectangle(final PixelPos center, final int[] shape) {
        final int w = shape[2];
        final int h = shape[1];
        final int x = (int) Math.floor(center.getX()) - w / 2;
        final int y = (int) Math.floor(center.getY()) - h / 2;

        return new Rectangle(x, y, w, h);
    }

    private static Array readSubsceneData(final RasterDataNode node,
                                          final DataType targetDataType,
                                          final int[] targetShape,
                                          final Rectangle rectangle) {
        final Array targetArray = Array.factory(targetDataType, new int[]{1, targetShape[1], targetShape[2]});

        final MultiLevelImage sourceImage = node.getSourceImage();
        final int minX = sourceImage.getMinX();
        final int minY = sourceImage.getMinY();
        final int maxX = sourceImage.getMaxX();
        final int maxY = sourceImage.getMaxY();

        final int x = Math.max(rectangle.x, minX);
        final int y = Math.max(rectangle.y, minY);
        final int w = Math.min(rectangle.x + rectangle.width - 1, maxX) - x + 1;
        final int h = Math.min(rectangle.y + rectangle.height - 1, maxY) - y + 1;
        final Rectangle validRectangle = new Rectangle(x, y, w, h);
        final Raster raster = sourceImage.getData(validRectangle);

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

    @Override
    public final void init(DataFile dataFile) throws IOException {
        if (getProduct() != null) {
            close();
        }
        final Product product = ProductIO.readProduct(new File(dataFile.getPath()), formatNames);
        if (product == null) {
            throw new IOException(
                    MessageFormat.format("Cannot read product file ''{0}''.", dataFile.getPath()));
        }

        this.product = product;
        this.dataFile = dataFile;
    }

    @Override
    public final void close() {
        if (product != null) {
            product.dispose();
        }
        dataFile = null;
        product = null;
    }

    public final Product getProduct() {
        return product;
    }

    @Override
    public final DataFile getDataFile() {
        return dataFile;
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
    public final Item[] getColumns() throws IOException {
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
    public final void write(NetcdfFileWriteable targetFile,
                            Observation observation,
                            String sourceVariableName,
                            String targetVariableName,
                            int matchupIndex,
                            PGgeometry refPoint,
                            Date refTime) throws IOException {
        final RasterDataNode node;
        if (product.containsBand(sourceVariableName)) {
            node = product.getBand(sourceVariableName);
        } else {
            node = product.getTiePointGrid(sourceVariableName);
        }

        final PixelPos pixelPos = findPixelPos(refPoint);
        final Variable targetVariable = targetFile.findVariable(NetcdfFile.escapeName(targetVariableName));
        final int[] targetOrigin = new int[]{matchupIndex, 0, 0};
        final int[] targetShape = targetVariable.getShape();
        final Rectangle rectangle = createSubsceneRectangle(pixelPos, targetShape);
        final DataType dataType = DataTypeUtils.getNetcdfDataType(node);
        final Array array = readSubsceneData(node, dataType, targetShape, rectangle);
        try {
            targetFile.write(NetcdfFile.escapeName(targetVariableName), targetOrigin, array);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public final InsituRecord readInsituRecord(int recordNo) throws OperationNotSupportedException {
        throw new OperationNotSupportedException();
    }

    protected final Item createColumn(final RasterDataNode node) {
        final String name = getSensorName() + "." + node.getName();
        final DataType type = DataTypeUtils.getNetcdfDataType(node);
        final ColumnBuilder builder = new ColumnBuilder();
        builder.setName(name);
        builder.setType(type);
        builder.setDimensions("record ni nj");
        final String unit = node.getUnit();
        if (unit != null && !unit.isEmpty()) {
            builder.setUnit(unit);
        }
        if (node.isScalingApplied()) {
            builder.setAddOffset(node.getScalingOffset());
            builder.setScaleFactor(node.getScalingFactor());
        }
        if (node.isNoDataValueUsed()) {
            builder.setFillValue(node.getNoDataValue());
        }
        builder.setRole(node.getName());
        builder.setSensor(getDataFile().getSensor());

        return builder.build();
    }

    protected final Date getCenterTimeAsDate() throws IOException {
        final ProductData.UTC startTime = getProduct().getStartTime();
        if (startTime == null) {
            throw new IOException("Unable to get start time for product '" + product.getName() + "'.");
        }
        final ProductData.UTC endTime = getProduct().getEndTime();
        if (endTime == null) {
            return startTime.getAsDate();
        }
        final ProductData.UTC centerTime = new ProductData.UTC(0.5 * (startTime.getMJD() + endTime.getMJD()));
        return centerTime.getAsDate();
    }

    protected final PixelPos findPixelPos(PGgeometry referencePoint) throws IOException {
        final float lon = (float) referencePoint.getGeometry().getFirstPoint().x;
        final float lat = (float) referencePoint.getGeometry().getFirstPoint().y;
        final GeoPos geoPos = new GeoPos(lat, lon);
        if (!geoPos.isValid()) {
            throw new IOException("Geo-location of reference point is invalid.");
        }
        final GeoCoding geoCoding = getProduct().getGeoCoding();
        final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, new PixelPos());
        if (!pixelPos.isValid()) {
            final String message = MessageFormat.format("Unable to find pixel at ({0}, {1}) in product ''{2}''.",
                                                        geoPos.getLon(),
                                                        geoPos.getLat(),
                                                        getProduct().getName());
            throw new IOException(message);
        }
        return pixelPos;
    }
}
