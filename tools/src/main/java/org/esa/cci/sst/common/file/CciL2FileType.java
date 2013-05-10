/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.common.file;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.common.AggregationContext;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.cellgrid.ArrayGrid;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.util.NcUtils;
import ucar.nc2.NetcdfFile;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the SST-CCI L2P file type.
 * <p/>
 * The implementation reads the L2P product file and makes a re-projection onto the L3U grid.
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
class CciL2FileType extends CciL3FileType {

    final static FileType INSTANCE = new CciL2FileType();

    private static final String QUALITY_LEVEL = "quality_level";
    private static final String UNCORRELATED_UNCERTAINTY = "uncorrelated_uncertainty";
    private static final String LARGE_SCALE_CORRELATED_UNCERTAINTY = "large_scale_correlated_uncertainty";
    private static final String SYNOPTIC_UNCERTAINTY = "synoptically_correlated_uncertainty";
    private static final String ADJUSTMENT_UNCERTAINTY = "adjustment_uncertainty";

    @Override
    public AggregationContext readSourceGrids(NetcdfFile datafile, SstDepth sstDepth, AggregationContext context) throws
                                                                                                                  IOException {
        final File file = new File(datafile.getLocation());
        final Product sourceProduct = ProductIO.readProduct(file, "NetCDF-CF");
        if (sourceProduct == null) {
            throw new IOException("Cannot not find reader for product'" + file.getPath() + "'.");
        }
        try {
            final List<String> bandNames = new ArrayList<String>(8);
            if (sstDepth == SstDepth.skin) {
                bandNames.add("sea_surface_temperature");
            } else {
                bandNames.add("sea_surface_temperature_depth");
            }
            bandNames.add(QUALITY_LEVEL);
            bandNames.add(UNCORRELATED_UNCERTAINTY);
            bandNames.add(LARGE_SCALE_CORRELATED_UNCERTAINTY);
            bandNames.add(SYNOPTIC_UNCERTAINTY);
            if (NcUtils.hasVariable(datafile, ADJUSTMENT_UNCERTAINTY)) {
                bandNames.add(ADJUSTMENT_UNCERTAINTY);
            }
            final Projector projector = new Projector(getGridDef(), 1000);
            final Product targetProduct = projector.createProjection(sourceProduct,
                                                                     bandNames.toArray(
                                                                             new String[bandNames.size()]));

            context.setSstGrid(createGrid(targetProduct, bandNames.get(0)));
            context.setQualityGrid(createGrid(targetProduct, QUALITY_LEVEL));
            context.setRandomUncertaintyGrid(createGrid(targetProduct, UNCORRELATED_UNCERTAINTY));
            context.setLargeScaleUncertaintyGrid(
                    createGrid(targetProduct, LARGE_SCALE_CORRELATED_UNCERTAINTY));
            context.setSynopticUncertaintyGrid(
                    createGrid(targetProduct, SYNOPTIC_UNCERTAINTY));
            if (NcUtils.hasVariable(datafile, ADJUSTMENT_UNCERTAINTY)) {
                context.setAdjustmentUncertaintyGrid(createGrid(targetProduct, ADJUSTMENT_UNCERTAINTY));
            }
        } finally {
            try {
                sourceProduct.closeIO();
            } catch (IOException ignored) {
                // ignore
            }
        }

        return context;
    }

    private Grid createGrid(Product product, String bandName) {
        final Band band = product.getBand(bandName);
        final RenderedImage image = band.getSourceImage().getImage(0);
        final Raster raster = image.getData();
        final GridDef gridDef = getGridDef();
        final int w = gridDef.getWidth();
        final int h = gridDef.getHeight();
        final float[] data = new float[w * h];
        raster.getDataElements(0, 0, w, h, data);

        return ArrayGrid.create(gridDef, data);
    }

    @Override
    public String getFilenameRegex() {
        return "\\d{14}-" + getRdac() + "-L2P_GHRSST-SST((skin)|(subskin)|(depth)|(fnd))[-]" +
               "((ATSR1)|(ATSR2)|(AATSR)|(AMSRE)|(SEVIRI_SST)|(TMI))[-]((LT)|(DM))-" +
               "v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";
    }
}
