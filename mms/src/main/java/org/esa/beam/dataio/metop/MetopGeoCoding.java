/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.metop;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.GeoApproximation;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PixelPosEstimator;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.dataop.maptransf.Datum;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;

/**
 * This geo-coding improves the inverse approximations used in the {@code TiePointGeoCoding} in order
 * to facilitate accurate re-projections and graticule drawing.
 * <p/>
 * Limitation: this geo-coding is not transferred when making subsets and is not saved when a product
 * is written to disk.
 *
 * @author Ralf Quast
 */
final class MetopGeoCoding extends TiePointGeoCoding {

    private transient PixelPosEstimator pixelPosEstimator;
    private transient MetopPixelFinder pixelFinder;

    MetopGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid) {
        super(latGrid, lonGrid, Datum.WGS_72);

        final PlanarImage lonImage = lonGrid.getGeophysicalImage();
        final PlanarImage latImage = latGrid.getGeophysicalImage();

        final GeoApproximation[] approximations = createApproximations(lonImage, latImage);
        final Rectangle bounds = new Rectangle(0, 0, lonGrid.getSceneRasterWidth(), lonGrid.getSceneRasterHeight());
        pixelPosEstimator = new PixelPosEstimator(approximations, bounds);
        pixelFinder = new MetopPixelFinder(lonImage, latImage, null, 0.01);
    }

    @Override
    public boolean canGetPixelPos() {
        return pixelPosEstimator.canGetPixelPos();
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        if (pixelPosEstimator.canGetPixelPos()) {
            pixelPosEstimator.getPixelPos(geoPos, pixelPos);
            if (pixelPos.isValid()) {
                pixelFinder.findPixelPos(geoPos, pixelPos);
            }
        } else {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    private static GeoApproximation[] createApproximations(PlanarImage lonImage, PlanarImage latImage) {
        return GeoApproximation.createApproximations(lonImage, latImage, null, 0.5);
    }

    @Override
    public boolean transferGeoCoding(Scene sourceScene, Scene targetScene, ProductSubsetDef subsetDef) {
        return false;
    }

    @Override
    public void dispose() {
        super.dispose();

        pixelFinder = null;
        pixelPosEstimator = null;
    }
}
