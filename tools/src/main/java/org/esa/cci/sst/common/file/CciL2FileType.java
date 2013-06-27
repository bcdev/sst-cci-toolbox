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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.internal.OperatorExecutor;
import org.esa.cci.sst.common.AggregationContext;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.cellgrid.ArrayGrid;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.util.NcUtils;
import ucar.nc2.NetcdfFile;

import javax.media.jai.PlanarImage;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import java.awt.Rectangle;
import java.awt.image.Raster;
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
            final Projector projector = new Projector(getGridDef());
            final Product targetProduct = projector.createProjection(sourceProduct,
                                                                     bandNames.toArray(
                                                                             new String[bandNames.size()]));

            /* for the purpose of debugging
            GPF.writeProduct(targetProduct,
                             new File("out/projected_" + sourceProduct.getFileLocation().getName().replace(".nc", "")),
                             "BEAM-DIMAP", false, ProgressMonitor.NULL);
            */

            setGrids(context, targetProduct, bandNames);
            /*
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
            */
        } finally {
            sourceProduct.dispose();
        }

        return context;
    }

    private void setGrids(AggregationContext context, Product targetProduct, List<String> bandNames) {
        final int bandCount = bandNames.size();
        final GridDef gridDef = getGridDef();
        final int w = gridDef.getWidth();
        final int h = gridDef.getHeight();
        final float[][] data = new float[bandCount][w * h];

        final PlanarImage[] images = new PlanarImage[bandCount];
        final TileComputationListener[] listeners = new TileComputationListener[bandCount];
        for (int i = 0; i < bandCount; i++) {
            final PlanarImage image = targetProduct.getBand(bandNames.get(i)).getGeophysicalImage();
            final TileComputationListener listener = new TCL(data[i]);
            image.addTileComputationListener(listener);
            images[i] = image;
            listeners[i] = listener;
        }

        final OperatorExecutor operatorExecutor = new OperatorExecutor(images,
                                                                       images[0].getNumXTiles(),
                                                                       images[0].getNumYTiles());
        operatorExecutor.execute(OperatorExecutor.ExecutionOrder.SCHEDULE_ROW_COLUMN_BAND, ProgressMonitor.NULL);
        for (int i = bandCount; --i > 0; ) {
            images[i].removeTileComputationListener(listeners[i]);
            images[i].dispose();
        }

        context.setSstGrid(ArrayGrid.create(gridDef, data[0]));
        context.setQualityGrid(ArrayGrid.create(gridDef, data[1]));
        context.setRandomUncertaintyGrid(ArrayGrid.create(gridDef, data[2]));
        context.setLargeScaleUncertaintyGrid(ArrayGrid.create(gridDef, data[3]));
        context.setSynopticUncertaintyGrid(ArrayGrid.create(gridDef, data[4]));
        if (bandCount > 5) {
            context.setAdjustmentUncertaintyGrid(ArrayGrid.create(gridDef, data[5]));
        }
    }

    private Grid createGrid(Product product, String bandName) {
        final Band band = product.getBand(bandName);
        final PlanarImage dataImage = band.getGeophysicalImage();
        final PlanarImage maskImage = band.getValidMaskImage();
        final GridDef gridDef = getGridDef();
        final int w = gridDef.getWidth();
        final int h = gridDef.getHeight();
        final float[] data = new float[w * h];

        for (int tileY = 0; tileY < dataImage.getNumYTiles(); tileY++) {
            for (int tileX = 0; tileX < dataImage.getNumXTiles(); tileX++) {
                final Raster dataTile = dataImage.getTile(tileX, tileY);
                final Raster maskTile = maskImage.getTile(tileX, tileY);
                for (int i = 0; i < dataTile.getHeight(); i++) {
                    for (int k = 0; k < dataTile.getWidth(); k++) {
                        final int x = dataTile.getMinX() + k;
                        final int y = dataTile.getMinY() + i;
                        if (maskTile.getSample(x, y, 0) != 0) {
                            data[x + y * w] = dataTile.getSampleFloat(x, y, 0);
                        } else {
                            data[x + y * w] = Float.NaN;
                        }
                    }
                }
            }
        }

        return ArrayGrid.create(gridDef, data);
    }

    @Override
    public String getFilenameRegex() {
        return "\\d{14}-" + getRdac() + "-L2P_GHRSST-SST((skin)|(subskin)|(depth)|(fnd))[-]" +
               "((ATSR1)|(ATSR2)|(AATSR)|(AVHRR\\d{2}_G)|(AMSRE)|(SEVIRI_SST)|(TMI))[-]((LT)|(DM))-" +
               "v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";
    }

    private static class TCL implements TileComputationListener {

        private final float[] data;

        public TCL(float[] data) {
            this.data = data;
        }

        @Override
        public void tileComputed(Object o, TileRequest[] tileRequests, PlanarImage planarImage, int tileX, int tileY,
                                 Raster raster) {
            final int width = planarImage.getWidth();
            final Rectangle tileRectangle = planarImage.getTileRect(tileX, tileY);
            for (int i = 0; i < tileRectangle.getHeight(); i++) {
                for (int k = 0; k < tileRectangle.getWidth(); k++) {
                    final int x = tileRectangle.x + k;
                    final int y = tileRectangle.y + i;
                    if (true) {
                        data[x + y * width] = raster.getSampleFloat(x, y, 0);
                    } else {
                        data[x + y * width] = Float.NaN;
                    }
                }
            }
        }

        @Override
        public void tileCancelled(Object o, TileRequest[] tileRequests, PlanarImage planarImage, int tileX, int tileY) {
        }

        @Override
        public void tileComputationFailure(Object o, TileRequest[] tileRequests, PlanarImage planarImage,
                                           int tileX, int tileY, Throwable throwable) {
        }
    }
}
