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
import org.esa.beam.dataio.atsr.AtsrConstants;
import org.esa.beam.dataio.avhrr.AvhrrReaderPlugIn;
import org.esa.beam.dataio.cci.sst.OsiProductReaderPlugIn;
import org.esa.beam.dataio.cci.sst.PmwProductReaderPlugIn;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGeoCodingWithFallback;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.PixelFinder;
import org.esa.beam.util.QuadTreePixelFinder;
import org.esa.beam.util.RasterDataNodeSampleSource;
import org.esa.beam.util.SampleSource;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.VariableDescriptor;
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

public class ProductIOHandler implements IOHandler {

    final String sensorName;

    DataFile dataFile;
    Product product;

    public ProductIOHandler(String sensorName) {
        this.sensorName = sensorName;
    }

    @Override
    public void init(DataFile dataFile) throws IOException {
        if (this.product != null) {
            close();
        }
        Product product = ProductIO.readProduct(new File(dataFile.getPath()),
                                                EnvisatConstants.ENVISAT_FORMAT_NAME,
                                                AtsrConstants.ATSR_FORMAT_NAME,
                                                PmwProductReaderPlugIn.FORMAT_NAME,
                                                OsiProductReaderPlugIn.FORMAT_NAME,
                                                AvhrrReaderPlugIn.FORMAT_NAME);
        if (product == null) {
            throw new IOException(
                    MessageFormat.format("Unable to read observation file ''{0}''.", dataFile.getPath()));
        }
        workAroundBeamIssue1241(product);

        this.product = product;
        this.dataFile = dataFile;
    }

    @Override
    public void close() {
        if (product != null) {
            product.dispose();
        }
        dataFile = null;
        product = null;
    }

    @Override
    public final int getNumRecords() {
        return 1;
    }

    @Override
    public Observation readObservation(int recordNo) throws IOException {
        final GlobalObservation globalObservation = new GlobalObservation();
        globalObservation.setTime(getCenterTimeAsDate());
        globalObservation.setDatafile(dataFile);
        globalObservation.setRecordNo(0);
        globalObservation.setSensor(sensorName);

        return globalObservation;
    }

    @Override
    public Descriptor[] getVariableDescriptors() throws IOException {
        final ArrayList<Descriptor> variableDescriptorList = new ArrayList<Descriptor>();
        for (RasterDataNode node : product.getTiePointGrids()) {
            final Descriptor variableDescriptor = setUpVariableDescriptor(node);
            variableDescriptorList.add(variableDescriptor);
        }
        for (RasterDataNode node : product.getBands()) {
            final Descriptor variableDescriptor = setUpVariableDescriptor(node);
            variableDescriptorList.add(variableDescriptor);
        }
        return variableDescriptorList.toArray(new Descriptor[variableDescriptorList.size()]);
    }

    Product getProduct() {
        return product;
    }

    Descriptor setUpVariableDescriptor(final RasterDataNode node) {
        final VariableDescriptor descriptor = new VariableDescriptor();
        descriptor.setRole(node.getName());
        descriptor.setName(String.format("%s.%s", sensorName, node.getName()));
        descriptor.setSensor(dataFile.getSensor());
        final DataType dataType = DataTypeUtils.getNetcdfDataType(node);
        descriptor.setType(dataType.name());
        final String unit = node.getUnit();
        if (unit != null && !unit.isEmpty()) {
            descriptor.setUnit(unit);
        }
        descriptor.setDimensions("ni nj");
        if (node.isScalingApplied()) {
            descriptor.setAddOffset(node.getScalingOffset());
            descriptor.setScaleFactor(node.getScalingFactor());
        }
        if (node.isNoDataValueUsed()) {
            descriptor.setFillValue(node.getNoDataValue());
        }
        return descriptor;
    }

    @Override
    public void write(NetcdfFileWriteable targetFile, Observation observation, String sourceVariableName,
                      String targetVariableName, int matchupIndex, final PGgeometry refPoint, final Date refTime) throws
                                                                                                                  IOException {
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
    public InsituRecord readInsituRecord(int recordNo) throws OperationNotSupportedException {
        throw new OperationNotSupportedException();
    }

    @Override
    public DataFile getDataFile() {
        return dataFile;
    }

    PixelPos findPixelPos(PGgeometry refPoint) throws IOException {
        final float lon = (float) refPoint.getGeometry().getFirstPoint().x;
        final float lat = (float) refPoint.getGeometry().getFirstPoint().y;
        final GeoPos geoPos = new GeoPos(lat, lon);
        if (!geoPos.isValid()) {
            throw new IOException("Invalid reference geo-location.");
        }
        final GeoCoding geoCoding = product.getGeoCoding();
        final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, new PixelPos());
        if (!pixelPos.isValid()) {
            throw new IOException(MessageFormat.format("Unable to find pixel at ({0}, {1}) in product ''{2}''.",
                                                       geoPos.getLon(),
                                                       geoPos.getLat(),
                                                       product.getName()));
        }
        return pixelPos;
    }

    Date getCenterTimeAsDate() throws IOException {
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

    static Rectangle createSubsceneRectangle(final PixelPos subsceneCenter, final int[] subsceneShape) {
        final int w = subsceneShape[2];
        final int h = subsceneShape[1];
        final int x = (int) Math.floor(subsceneCenter.getX()) - w / 2;
        final int y = (int) Math.floor(subsceneCenter.getY()) - h / 2;
        return new Rectangle(x, y, w, h);
    }

    private static Array readSubsceneData(final RasterDataNode node, final DataType type, final int[] targetShape,
                                          final Rectangle rectangle) {
        final Array array = Array.factory(type, new int[]{1, targetShape[1], targetShape[2]});

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
                    try {
                        value = getSample(raster, j, i);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
                array.setObject(k, value);
            }
        }
        return array;
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

    static void workAroundBeamIssue1241(Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding instanceof TiePointGeoCoding) {
            final TiePointGeoCoding tiePointGeoCoding = (TiePointGeoCoding) geoCoding;
            final TiePointGrid latGrid = tiePointGeoCoding.getLatGrid();
            final TiePointGrid lonGrid = tiePointGeoCoding.getLonGrid();
            final SampleSource latSource = new RasterDataNodeSampleSource(latGrid);
            final SampleSource lonSource = new RasterDataNodeSampleSource(lonGrid);
            final PixelFinder pixelFinder = new QuadTreePixelFinder(lonSource, latSource);
            product.setGeoCoding(new TiePointGeoCodingWithFallback(tiePointGeoCoding, pixelFinder));
        }
    }

}
