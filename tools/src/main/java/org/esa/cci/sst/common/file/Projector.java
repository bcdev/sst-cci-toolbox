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
import org.esa.beam.framework.datamodel.Rotator;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.math.DistanceCalculator;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.SphericalDistanceCalculator;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.util.NcUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.esa.beam.framework.datamodel.PixelPosEstimator.Approximation;
import static org.esa.beam.framework.datamodel.PixelPosEstimator.SampleSource;

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
    private final Logger logger;

    static {
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(2147483648L);
        JAI.getDefaultInstance().getTileScheduler().setParallelism(Runtime.getRuntime().availableProcessors());
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

    Projector(GridDef gridDef, Logger logger) {
        this.gridDef = gridDef;
        this.logger = logger;
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

        final ExecutorService executorService = Executors.newCachedThreadPool();

        final Band[] bands = new Band[bandCount];
        final PlanarImage[] images = new PlanarImage[bandCount];
        for (int i = 0; i < bandCount; i++) {
            bands[i] = targetProduct.getBand(bandNames.get(i));
            images[i] = bands[i].getGeophysicalImage();
        }
        final int tileCountX = images[0].getNumXTiles();
        for (int tileX = 0; tileX < tileCountX; tileX++) {
            for (int i = 0; i < images.length; i++) {
                final TileTask tileTask = new TileTask(images, tileX, data,
                                                       (float) bands[i].getGeophysicalNoDataValue());
                executorService.submit(tileTask);
            }
        }
        executorService.shutdown();

        try {
            final boolean b = executorService.awaitTermination(10, TimeUnit.DAYS);
        } catch (InterruptedException ignored) {
        }

        /*
        final PlanarImage[] images = new PlanarImage[bandCount];
        for (int i = 0; i < bandCount; i++) {
            final Band band = targetProduct.getBand(bandNames.get(i));
            final PlanarImage image = band.getGeophysicalImage();
            final TileComputationListener listener = new TCL(data[i], (float) band.getGeophysicalNoDataValue(), logger);
            image.addTileComputationListener(listener);
            images[i] = image;
        }

        final int tileCountX = images[0].getNumXTiles();
        final int tileCountY = images[0].getNumYTiles();
        final List<TileRequest> tileRequests = new ArrayList<TileRequest>();
        if (logger != null) {
            logger.info(MessageFormat.format("Tile scheduler parallelism: {0}",
                                             JAI.getDefaultInstance().getTileScheduler().getParallelism()));
        }
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            for (int tileX = 0; tileX < tileCountX; tileX++) {
                for (int i = 0; i < images.length; i++) {
                    final PlanarImage image = images[i];
                    final String bandName = bandNames.get(i);
                    final Point point = new Point(tileX, tileY);
                    if (logger != null) {
                        logger.fine(MessageFormat.format("Queueing tile ({0}, {1}) of band ''{2}''.", tileX, tileY,
                                                         bandName));
                    }
                    final TileRequest tileRequest = image.queueTiles(new Point[]{point});
                    tileRequests.add(tileRequest);
                }
            }
        }
        final long startTime = System.currentTimeMillis();
        while (true) {
            boolean completed = true;
            search:
            for (final TileRequest tileRequest : tileRequests) {
                final Point[] points = tileRequest.getTileIndices();
                for (final Point p : points) {
                    final int status = tileRequest.getTileStatus(p.x, p.y);
                    if (status != TileRequest.TILE_STATUS_COMPUTED) {
                        completed = false;
                        break search;
                    }
                }
            }
            if (completed) {
                break;
            }
            if (System.currentTimeMillis() - startTime > 3600000) {
                if (logger != null) {
                    logger.severe(
                            MessageFormat.format("Projection of product ''{0}'' could not be completed due to timeout.",
                                                 sourceProduct.getFileLocation()));
                }
            }
        }
        */

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

    /*
     AVHRR L2P products are self-overlapping, so there may be the need to create a mosaic of subsets

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
      */

    // AVHRR L2P products are self-overlapping, so there may be the need to create a mosaic of subsets
    private static Product createSubset(Product sourceProduct, Rectangle rectangle) {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("region", rectangle);

        return GPF.createProduct("Subset", parameters, sourceProduct);
    }

    public float[][] createProjectedData(NetcdfFile datafile, List<String> variableNames) throws IOException {
        final Variable lat = NcUtils.getVariable(datafile, "lat");
        final Variable lon = NcUtils.getVariable(datafile, "lon");

        final int rank = lat.getRank();
        final GridDef sourceRaster = GridDef.createRaster(lat.getShape(rank - 1), lat.getShape(rank - 2));
        final Grid lonGrid = NcUtils.readGrid(datafile, lon, sourceRaster, 0);
        final Grid latGrid = NcUtils.readGrid(datafile, lat, sourceRaster, 0);
        final PixelLocator sourcePixelLocator = new PixelLocator(lonGrid, latGrid);

        final int variableCount = variableNames.size();
        final Grid[] sourceGrids = new Grid[variableCount];
        for (int i = 0; i < variableCount; ++i) {
            sourceGrids[i] = NcUtils.readGrid(datafile, variableNames.get(i), sourceRaster);
        }

        final int w = gridDef.getWidth();
        final int h = gridDef.getHeight();
        final float[][] projectedData = new float[variableCount][w * h];

        final ExecutorService executorService = Executors.newCachedThreadPool();
        final List<Future<float[][]>> futures = new ArrayList<Future<float[][]>>(h);
        for (int y = 0; y < h; y++) {
            final RowTask rowTask = new RowTask(sourceGrids, y, sourcePixelLocator);
            futures.add(executorService.submit(rowTask));
        }
        executorService.shutdown();

        try {
            final boolean b = executorService.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
        }

        for (int y = 0; y < h; y++) {
            try {
                final float[][] rowData = futures.get(y).get();
                for (int i = 0; i < variableCount; i++) {
                    System.arraycopy(rowData[i], 0, projectedData[i], y * w, w);
                }
            } catch (InterruptedException ignored) {
                // ignore
            } catch (ExecutionException ignored) {
                // ignore
            }
        }

        return projectedData;
    }

    private static class TCL implements TileComputationListener {

        private final float[] data;
        private final float noDataValue;
        private final Logger logger;

        public TCL(float[] data, float noDataValue, Logger logger) {
            this.data = data;
            this.noDataValue = noDataValue;
            this.logger = logger;
        }

        @Override
        public void tileComputed(Object o, TileRequest[] tileRequests, PlanarImage planarImage, int tileX, int tileY,
                                 Raster raster) {
            if (logger != null) {
                logger.info(MessageFormat.format("Completed request for tile ({0}, {1}).", tileX, tileY));
            }
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
            if (logger != null) {
                logger.severe(MessageFormat.format("Failed to compute tile ({0}, {1}).", tileX, tileY));
            }
        }
    }

    private class TileTask implements Callable<Void> {

        private final PlanarImage[] images;
        private final int tileX;
        private final float[][] data;
        private final float noDataValue;

        public TileTask(PlanarImage[] images, int tileX, float[][] data, float noDataValue) {
            this.images = images;
            this.tileX = tileX;
            this.data = data;
            this.noDataValue = noDataValue;
        }

        @Override
        public Void call() throws Exception {
            if (logger != null) {
                logger.info(MessageFormat.format("Computing projection of column {0}.", tileX));
            }
            for (int i = 0; i < images.length; ++i) {
                final int width = images[i].getWidth();
                for (int tileY = 0; tileY < images[i].getMaxTileY(); tileY++) {
                    final Raster raster = images[i].getTile(tileX, tileY);
                    final Rectangle tileRectangle = images[i].getTileRect(tileX, tileY);
                    final float[] floats = data[i];

                    for (int m = 0; m < tileRectangle.getHeight(); m++) {
                        for (int n = 0; n < tileRectangle.getWidth(); n++) {
                            final int x = tileRectangle.x + n;
                            final int y = tileRectangle.y + m;
                            final float sampleValue = raster.getSampleFloat(x, y, 0);
                            if (sampleValue != noDataValue) {
                                floats[x + y * width] = sampleValue;
                            } else {
                                floats[x + y * width] = Float.NaN;
                            }
                        }
                    }
                }
            }
            if (logger != null) {
                logger.info(MessageFormat.format("Computed projection of column {0}.", tileX));
            }

            return null;
        }
    }

    private class RowTask implements Callable<float[][]> {

        private final Grid[] sourceGrids;
        private final int y;
        private PixelLocator sourcePixelLocator;

        public RowTask(Grid[] sourceGrids, int y, PixelLocator sourcePixelLocator) {
            this.sourceGrids = sourceGrids;
            this.y = y;
            this.sourcePixelLocator = sourcePixelLocator;
        }

        @Override
        public float[][] call() throws Exception {
            if (logger != null && logger.isLoggable(Level.INFO)) {
                logger.info(MessageFormat.format("Starting computing projection for row {0}.", y));
            }
            final float[][] data = new float[sourceGrids.length][gridDef.getWidth()];

            final Point2D p = new Point2D.Double();
            for (int x = 0; x < gridDef.getWidth(); x++) {
                final double lon = gridDef.getCenterLon(x);
                final double lat = gridDef.getCenterLat(y);
                sourcePixelLocator.getPixelLocation(lon, lat, p);
                if (!Double.isNaN(p.getX()) && !Double.isNaN(p.getY())) {
                    for (int i = 0; i < sourceGrids.length; ++i) {
                        data[i][x] = (float) sourceGrids[i].getSampleDouble((int) p.getX(), (int) p.getY());
                    }
                } else {
                    for (int i = 0; i < sourceGrids.length; ++i) {
                        data[i][x] = Float.NaN;
                    }
                }
            }

            if (logger != null && logger.isLoggable(Level.INFO)) {
                logger.info(MessageFormat.format("Finished computing projection for row {0}.", y));
            }

            return data;
        }
    }

    private static class PixelLocator {

        private final int searchCycleCount = 30;
        private final double pixelDiagonal = Math.toRadians(0.05);

        private final Grid lonGrid;
        private final Grid latGrid;
        private final Approximation[] approximations;
        private final int gridW;
        private final int gridH;

        PixelLocator(Grid lonGrid, Grid latGrid) {
            this.lonGrid = lonGrid;
            this.latGrid = latGrid;

            gridW = lonGrid.getGridDef().getWidth();
            gridH = lonGrid.getGridDef().getHeight();

            approximations = createApproximations(lonGrid, latGrid, 0.5);
        }

        Point2D getPixelLocation(double lon, double lat, Point2D p) {
            if (approximations != null && lon >= -180.0 && lon <= 180.0 && lat >= -90.0 && lat <= 90.0) {
                final Approximation approximation = Approximation.findMostSuitable(approximations, lat, lon);
                if (approximation != null) {
                    final Rectangle range = approximation.getRange();
                    final Rotator rotator = approximation.getRotator();
                    p.setLocation(lon, lat);
                    rotator.transform(p);
                    lon = p.getX();
                    lat = p.getY();
                    final double x = approximation.getFX().getValue(lat, lon);
                    if (x < range.getMinX() || x > range.getMaxX()) {
                        p.setLocation(Double.NaN, Double.NaN);
                    } else {
                        final double y = approximation.getFY().getValue(lat, lon);
                        if (y < range.getMinY() || y > range.getMaxY()) {
                            p.setLocation(Double.NaN, Double.NaN);
                        } else {
                            p.setLocation(x, y);
                            refinePixelLocation(lon, lat, p);
                        }
                    }
                } else {
                    p.setLocation(Double.NaN, Double.NaN);
                }
            } else {
                p.setLocation(Double.NaN, Double.NaN);
            }

            return p;
        }

        void refinePixelLocation(final double lon0, final double lat0, Point2D p) {
            int x0 = (int) Math.floor(p.getX());
            int y0 = (int) Math.floor(p.getY());

            if (x0 >= 0 && x0 < gridW && y0 >= 0 && y0 < gridH) {
                final int searchRadius = 2 * searchCycleCount;

                int x1 = Math.max(x0 - searchRadius, 0);
                int y1 = Math.max(y0 - searchRadius, 0);
                int x2 = Math.min(x0 + searchRadius, gridW - 1);
                int y2 = Math.min(y0 + searchRadius, gridH - 1);

                final int rasterMinX = x1;
                final int rasterMinY = y1;
                @SuppressWarnings("UnnecessaryLocalVariable")
                final int rasterMaxX = x2;
                @SuppressWarnings("UnnecessaryLocalVariable")
                final int rasterMaxY = y2;

                final DistanceCalculator dc = new SphericalDistanceCalculator(lon0, lat0);

                double minDistance;
                double lon = lonGrid.getSampleDouble(x0, y0);
                double lat = latGrid.getSampleDouble(x0, y0);

                if (lon >= -180.0 && lon <= 180.0 && lat >= -90.0 && lat <= 90.0) {
                    minDistance = dc.distance(lon, lat);
                } else {
                    minDistance = Double.POSITIVE_INFINITY;
                }

                for (int i = 0; i < searchCycleCount; i++) {
                    x1 = x0;
                    y1 = y0;

                    int minX = Math.max(x1 - 2, rasterMinX);
                    int minY = Math.max(y1 - 2, rasterMinY);
                    int maxX = Math.min(x1 + 2, rasterMaxX);
                    int maxY = Math.min(y1 + 2, rasterMaxY);

                    while (minX > rasterMinX) {
                        if (isValid(minX, y1)) {
                            break;
                        }
                        if (minX > rasterMinX) {
                            minX--;
                        }
                    }
                    while (maxX < rasterMaxX) {
                        if (isValid(maxX, y1)) {
                            break;
                        }
                        if (maxX < rasterMaxX) {
                            maxX++;
                        }
                    }

                    for (int y = minY; y <= maxY; y++) {
                        for (int x = minX; x <= maxX; x++) {
                            if (y != y0 || x != x0) {
                                lon = lonGrid.getSampleDouble(x, y);
                                lat = latGrid.getSampleDouble(x, y);
                                if (lon >= -180.0 && lon <= 180.0 && lat >= -90.0 && lat <= 90.0) {
                                    lon = lonGrid.getSampleDouble(x, y);
                                    lat = latGrid.getSampleDouble(x, y);
                                    final double d = dc.distance(lon, lat);
                                    if (d < minDistance) {
                                        x1 = x;
                                        y1 = y;
                                        minDistance = d;
                                    }
                                }
                            }
                        }
                    }
                    if (x1 == x0 && y1 == y0) {
                        break;
                    }

                    x0 = x1;
                    y0 = y1;
                }
                if (minDistance <= pixelDiagonal) {
                    p.setLocation(x0 + 0.5, y0 + 0.5);
                } else {
                    p.setLocation(Double.NaN, Double.NaN);
                }
            } else {
                p.setLocation(Double.NaN, Double.NaN);
            }
        }

        private boolean isValid(int x, int y) {
            final double lon = lonGrid.getSampleDouble(x, y);
            final double lat = latGrid.getSampleDouble(x, y);

            return lon >= -180.0 && lon <= 180.0 && lat >= -90.0 && lat <= 90.0;
        }

        private static Approximation[] createApproximations(Grid lonGrid,
                                                            Grid latGrid,
                                                            double accuracy) {
            final GridDef gridDef = lonGrid.getGridDef();
            final int w = gridDef.getWidth();
            final int h = gridDef.getHeight();
            final Rectangle[] rectangles = MathUtils.subdivideRectangle(w, h, 1, 1 + h / w, 1);

            final SampleSource lonSamples = new GridSampleSource(lonGrid);
            final SampleSource latSamples = new GridSampleSource(latGrid);
            final SampleSource maskSamples = new ConstantSampleSource(1);

            return Approximation.createApproximations(lonSamples, latSamples, maskSamples, accuracy, rectangles);
        }

    }

    private static final class GridSampleSource implements SampleSource {

        private final Grid grid;

        public GridSampleSource(Grid grid) {
            this.grid = grid;
        }

        @Override
        public int getSample(int x, int y) {
            return grid.getSampleInt(x, y);
        }

        @Override
        public double getSampleDouble(int x, int y) {
            return grid.getSampleDouble(x, y);
        }
    }

    private static final class ConstantSampleSource implements SampleSource {

        private final int value;

        private ConstantSampleSource(int value) {
            this.value = value;
        }

        @Override
        public final int getSample(int x, int y) {
            return value;
        }

        @Override
        public final double getSampleDouble(int x, int y) {
            return value;
        }
    }

    /*
    private class PixelFinder {

        private final SampleSource lonSamples;
        private final SampleSource latSamples;
        private final int maxSearchCycleCount = 30;
        private final int imageW;
        private final int imageH;

        private PixelFinder(SampleSource lonSamples, SampleSource latSamples) {
            this.lonSamples = lonSamples;
            this.latSamples = latSamples;

            imageW = lonSamples.getWidth();
            imageH = lonSamples.getHeight();
        }

        @Override
        public void findPixelPos(double lon, double lat, Point2D pixelPos) {
            int x0 = (int) Math.floor(pixelPos.getX());
            int y0 = (int) Math.floor(pixelPos.getY());

            if (x0 >= 0 && x0 < imageW && y0 >= 0 && y0 < imageH) {
                final int searchRadius = 2 * maxSearchCycleCount;

                int x1 = Math.max(x0 - searchRadius, 0);
                int y1 = Math.max(y0 - searchRadius, 0);
                int x2 = Math.min(x0 + searchRadius, imageW - 1);
                int y2 = Math.min(y0 + searchRadius, imageH - 1);

                final int rasterMinX = x1;
                final int rasterMinY = y1;
                @SuppressWarnings("UnnecessaryLocalVariable")
                final int rasterMaxX = x2;
                @SuppressWarnings("UnnecessaryLocalVariable")
                final int rasterMaxY = y2;

                final double lat0 = lat;
                final double lon0 = lon;
                final DistanceCalculator dc = new SinusoidalDistanceCalculator(lon0, lat0);

                double minDistance;
                final double lon00 = lonSamples.getSampleDouble(x0, y0);
                final double lat00 = latSamples.getSampleDouble(x0, y0);
                if (lon00 >= -180.0 && lon00 <= 180.0 && lat00 >= -90.0 && lat00 <= 90.0) {
                    minDistance = dc.distance(lon00, lat00);
                } else {
                    minDistance = Double.POSITIVE_INFINITY;
                }

                for (int i = 0; i < maxSearchCycleCount; i++) {
                    x1 = x0;
                    y1 = y0;

                    int minX = Math.max(x1 - 2, rasterMinX);
                    int minY = Math.max(y1 - 2, rasterMinY);
                    int maxX = Math.min(x1 + 2, rasterMaxX);
                    int maxY = Math.min(y1 + 2, rasterMaxY);

                    while (minX > rasterMinX) {
                        if (getSample(minX, y1, maskImage) != 0) {
                            break;
                        }
                        if (minX > rasterMinX) {
                            minX--;
                        }
                    }
                    while (maxX < rasterMaxX) {
                        if (getSample(maxX, y1, maskImage) != 0) {
                            break;
                        }
                        if (maxX < rasterMaxX) {
                            maxX++;
                        }
                    }

                    for (int y = minY; y <= maxY; y++) {
                        for (int x = minX; x <= maxX; x++) {
                            if (y != y0 || x != x0) {
                                if (getSample(x, y, maskImage) != 0) {
                                    final double lat = getSampleDouble(x, y, latSamples);
                                    final double lon = getSampleDouble(x, y, lonSamples);
                                    final double d = dc.distance(lon, lat);
                                    if (d < minDistance) {
                                        x1 = x;
                                        y1 = y;
                                        minDistance = d;
                                    }
                                }
                            }
                        }
                    }
                    if (x1 == x0 && y1 == y0) {
                        break;
                    }

                    x0 = x1;
                    y0 = y1;
                }
                if (minDistance < pixelDiagonalSquared) {
                    pixelPos.setLocation(x0 + 0.5f, y0 + 0.5f);
                } else {
                    pixelPos.setInvalid();
                }
            } else {
                pixelPos.setInvalid();
            }
        }
    }
    */
}
