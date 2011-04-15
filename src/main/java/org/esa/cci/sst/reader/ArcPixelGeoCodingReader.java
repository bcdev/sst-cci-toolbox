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

import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfGeocodingPart;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.util.PixelFinder;
import org.esa.cci.sst.util.QuadTreePixelFinder;
import org.esa.cci.sst.util.RasterDataNodeSampleSource;
import org.esa.cci.sst.util.SampleSource;

import java.io.IOException;

/**
 * @author Thomas Storm
 */
public class ArcPixelGeoCodingReader extends CfGeocodingPart {

    @Override
    public void preDecode(final ProfileReadContext ctx, final Product p) throws IOException {
        super.preDecode(ctx, p);
    }

    @Override
    public void decode(final ProfileReadContext ctx, final Product p) throws IOException {
        super.decode(ctx, p);
        final PixelGeoCoding sourceGeoCoding = (PixelGeoCoding) p.getGeoCoding();
        final Band latBand = sourceGeoCoding.getLatBand();
        final Band lonBand = sourceGeoCoding.getLonBand();
        final SampleSource latSource = new RasterDataNodeSampleSource(latBand);
        final SampleSource lonSource = new RasterDataNodeSampleSource(lonBand);
        final PixelFinder pixelFinder = new QuadTreePixelFinder(lonSource, latSource);
        final PixelGeoCodingWithFallback geoCoding = new PixelGeoCodingWithFallback(sourceGeoCoding, pixelFinder);
        p.setGeoCoding(geoCoding);
    }
}
