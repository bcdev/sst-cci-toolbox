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

package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * Product reader for AVHRR MD files in netcdf-format.
 *
 * @author Thomas Storm
 */
public class AvhrrMdProductReader extends NetcdfProductReaderTemplate {

    protected AvhrrMdProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected void addBands(Product product) throws IOException {
    }

    @Override
    protected void addGeoCoding(Product product) {
    }

    @Override
    protected void addMetadata(Product product) {
    }

    @Override
    protected Product createPlainProduct() throws IOException {
        return null;
    }

    @Override
    protected RenderedImage createSourceImage(Band band) {
        return null;
    }

    @Override
    protected void setTime(Product product) {
    }
}
