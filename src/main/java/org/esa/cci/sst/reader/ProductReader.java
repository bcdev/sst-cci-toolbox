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

import org.esa.beam.dataio.atsr.AtsrConstants;
import org.esa.beam.dataio.avhrr.AvhrrReaderPlugIn;
import org.esa.beam.dataio.cci.sst.HdfOsiProductReaderPlugIn;
import org.esa.beam.dataio.cci.sst.NcOsiProductReaderPlugIn;
import org.esa.beam.dataio.cci.sst.PmwProductReaderPlugIn;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.dataio.envisat.EnvisatProductReader;
import org.esa.beam.framework.dataio.ProductFlipper;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.PixelGeoCodingWrapper;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.util.BoundaryCalculator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A product IO handler that produces {@link RelatedObservation}s. This handler
 * is used for all satellite input data except for Aerosol AAI.
 * <p/>
 * Supported satellite data include AATSR, ATSR1, ATSR2, OSI-SAF, PMW, and
 * AVHRR-GAC.
 *
 * @author Ralf Quast
 */
class ProductReader extends AbstractProductReader {

    private final BoundaryCalculator bc;

    ProductReader(String sensorName) {
        super(sensorName,
              EnvisatConstants.ENVISAT_FORMAT_NAME,
              AtsrConstants.ATSR_FORMAT_NAME,
              HdfOsiProductReaderPlugIn.FORMAT_NAME,
              NcOsiProductReaderPlugIn.FORMAT_NAME,
              PmwProductReaderPlugIn.FORMAT_NAME,
              AvhrrReaderPlugIn.FORMAT_NAME);
        this.bc = new BoundaryCalculator();
    }

    @Override
    protected final Product readProduct(DataFile dataFile) throws IOException {
        Product product = super.readProduct(dataFile);
        if (product.getProductReader() instanceof EnvisatProductReader) {
            if (product.getName().startsWith("ATS")) {
                // we need pixels arranged in scan direction, so flip the product horizontally when it is from AATSR
                product = createHorizontallyFlippedProduct(product);
            }
            if (product.getName().startsWith("AT1")) {
                product = shiftForwardBands(3, 0, product);
            } else if (product.getName().startsWith("AT2")) {
                product = shiftForwardBands(1, -1, product);
            } else if (product.getName().startsWith("ATS")) {
                product = shiftForwardBands(-1, -2, product);
            }
        }
        if (product.getGeoCoding() instanceof PixelGeoCoding) {
            product.setGeoCoding(new PixelGeoCodingWrapper((PixelGeoCoding) product.getGeoCoding()));
        }
        return product;
    }

    private Product shiftForwardBands(int xi, int yi, Product product) {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("shiftX", xi);
        params.put("shiftY", yi);
        params.put("bandNamesPattern", ".*_fward_.*");
        params.put("fillValue", product.getBand("btemp_fward_1200").getNoDataValue());
        return GPF.createProduct(OperatorSpi.getOperatorAlias(ShiftOp.class), params, product);
    }

    @Override
    public final RelatedObservation readObservation(int recordNo) throws IOException {
        final RelatedObservation observation = new RelatedObservation();
        try {
            observation.setLocation(bc.getGeoBoundary(getProduct()));
        } catch (Exception e) {
            throw new IOException(e);
        }
        observation.setTime(getCenterTimeAsDate());

        observation.setDatafile(getDatafile());
        observation.setRecordNo(0);
        observation.setSensor(getSensorName());

        return observation;
    }

    @Override
    public int getLineSkip() {
        final MetadataElement metadataElement = getProduct().getMetadataRoot().getElement("reader_generated");
        if(metadataElement != null) {
            final MetadataAttribute metadataAttribute = metadataElement.getAttribute("lead_line_skip");
            if(metadataAttribute != null) {
                return metadataAttribute.getData().getElemInt();
            }
        }
        return 0;
    }

    private Product createHorizontallyFlippedProduct(Product product) throws IOException {
        final ProductData.UTC startTime = product.getStartTime();
        final ProductData.UTC endTime = product.getEndTime();
        product = ProductFlipper.createFlippedProduct(product,
                                                      true,
                                                      ProductFlipper.FLIP_HORIZONTAL,
                                                      product.getName(),
                                                      product.getDescription());
        product.setStartTime(startTime);
        product.setEndTime(endTime);
        return product;
    }
}
