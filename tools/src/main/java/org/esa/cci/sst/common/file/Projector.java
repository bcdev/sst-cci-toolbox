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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelGeoCoding2;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.cci.sst.common.cellgrid.GridDef;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static org.esa.beam.gpf.operators.standard.MosaicOp.Variable;

/**
 * For doing a re-projection of full-orbit AVHRR L2P products.
 *
 * @author Bettina Scholze
 * @author Sabine Embacher
 * @author Ralf Quast
 */
class Projector {

    private static final String MASK_EXPRESSION = "lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0";

    private final GridDef gridDef;

    static {
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(536870912);
        JAI.getDefaultInstance().getTileScheduler().setParallelism(Runtime.getRuntime().availableProcessors());
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

    Projector(GridDef gridDef) {
        this.gridDef = gridDef;
    }

    float[][] createProjectedData(Product sourceProduct, List<String> bandNames) {
        final Product targetProduct = createProjection(sourceProduct, bandNames.toArray(new String[bandNames.size()]));

            /* for the purpose of debugging
            GPF.writeProduct(targetProduct,
                             new File("out/projected_" + sourceProduct.getFileLocation().getName().replace(".nc", "")),
                             "BEAM-DIMAP", false, ProgressMonitor.NULL);
            */

        final int bandCount = bandNames.size();
        final int w = gridDef.getWidth();
        final int h = gridDef.getHeight();
        final float[][] data = new float[bandCount][w * h];

        final PlanarImage[] images = new PlanarImage[bandCount];
        for (int i = 0; i < bandCount; i++) {
            final Band band = targetProduct.getBand(bandNames.get(i));
            final PlanarImage image = band.getGeophysicalImage();
            final TileComputationListener listener = new TCL(data[i], (float) band.getGeophysicalNoDataValue());
            image.addTileComputationListener(listener);
            images[i] = image;
        }

        final int tileCountX = images[0].getNumXTiles();
        final int tileCountY = images[0].getNumYTiles();
        final List<TileRequest> tileRequests = new ArrayList<TileRequest>();
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            for (int tileX = 0; tileX < tileCountX; tileX++) {
                // TODO - logging
                for (final PlanarImage image : images) {
                    final Point point = new Point(tileX, tileY);
                    tileRequests.add(image.queueTiles(new Point[]{point}));
                }
            }
        }
        while (true) {
            boolean completed = true;
            for (final TileRequest tileRequest : tileRequests) {
                final Point point = tileRequest.getTileIndices()[0];
                if (tileRequest.getTileStatus(point.x, point.y) != TileRequest.TILE_STATUS_COMPUTED) {
                    completed = false;
                    break;
                }
            }
            if (completed) {
                break;
            }
        }

        return data;
    }

    Product createProjection(Product sourceProduct, String... bandNames) {
        sourceProduct.getGeoCoding().dispose();
        sourceProduct.setGeoCoding(null);
        sourceProduct.setGeoCoding(new PixelGeoCoding2(sourceProduct.getBand("lat"),
                                                       sourceProduct.getBand("lon"),
                                                       MASK_EXPRESSION));

        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("crs", "EPSG:4326");
        parameters.put("resampling", "Nearest");
        parameters.put("referencePixelX", 0.0);
        parameters.put("referencePixelY", 0.0);
        parameters.put("northing", 90.0);
        parameters.put("easting", -180.0);
        parameters.put("pixelSizeX", gridDef.getResolutionX());
        parameters.put("pixelSizeY", gridDef.getResolutionY());
        parameters.put("width", gridDef.getWidth());
        parameters.put("height", gridDef.getHeight());

        return GPF.createProduct("Reproject", parameters, sourceProduct);
    }

    // AVHRR L2P products are self-overlapping, so there may be the need to create a mosaic of subsets
    private Product createMosaic(Product[] sourceProducts, String... bandNames) {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("crs", "EPSG:4326");
        parameters.put("resampling", "Nearest");
        parameters.put("northBound", 90.0);
        parameters.put("southBound", -90.0);
        parameters.put("westBound", -180.0);
        parameters.put("eastBound", 180.0);
        parameters.put("pixelSizeX", gridDef.getResolutionX());
        parameters.put("pixelSizeY", gridDef.getResolutionY());
        final Variable[] variables = new Variable[bandNames.length];
        for (int i = 0; i < bandNames.length; i++) {
            variables[i] = new Variable(bandNames[i], bandNames[i]);
        }
        parameters.put("variables", variables);

        return GPF.createProduct("Mosaic", parameters, sourceProducts);
    }

    // AVHRR L2P products are self-overlapping, so there may be the need to create a mosaic of subsets
    private static Product createSubset(Product sourceProduct, Rectangle rectangle) {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("region", rectangle);

        return GPF.createProduct("Subset", parameters, sourceProduct);
    }

    private static class TCL implements TileComputationListener {

        private final float[] data;
        private final float noDataValue;

        public TCL(float[] data, float noDataValue) {
            this.data = data;
            this.noDataValue = noDataValue;
        }

        @Override
        public void tileComputed(Object o, TileRequest[] tileRequests, PlanarImage planarImage, int tileX, int tileY,
                                 Raster raster) {
            // TODO - logging
            System.out.println("Completed " + raster.getBounds());
            final int width = planarImage.getWidth();
            final Rectangle tileRectangle = planarImage.getTileRect(tileX, tileY);
            synchronized (data) {
                for (int i = 0; i < tileRectangle.getHeight(); i++) {
                    for (int k = 0; k < tileRectangle.getWidth(); k++) {
                        final int x = tileRectangle.x + k;
                        final int y = tileRectangle.y + i;
                        final float sampleValue = raster.getSampleFloat(x, y, 0);
                        if (sampleValue != noDataValue) {
                            data[x + y * width] = sampleValue;
                        } else {
                            data[x + y * width] = Float.NaN;
                        }
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
            // TODO - logging
        }
    }

}
