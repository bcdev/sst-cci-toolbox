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

import org.esa.beam.framework.datamodel.Rotator;
import org.esa.beam.util.math.DistanceCalculator;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.SphericalDistanceCalculator;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.util.NcUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
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

    private final GridDef gridDef;
    private final Logger logger;

    Projector(GridDef gridDef, Logger logger) {
        this.gridDef = gridDef;
        this.logger = logger;
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
            final boolean completed = executorService.awaitTermination(30, TimeUnit.MINUTES);
            if (!completed) {
                if (logger != null && logger.isLoggable(Level.WARNING)) {
                    logger.warning(MessageFormat.format("Projecting file ''{0}'' was not completed in time.",
                                                        datafile.getLocation()));
                }
            }
        } catch (InterruptedException ignored) {
            // ignore, cannot happen
        }

        for (int y = 0; y < h; y++) {
            try {
                final float[][] rowData = futures.get(y).get();
                for (int i = 0; i < variableCount; i++) {
                    System.arraycopy(rowData[i], 0, projectedData[i], y * w, w);
                }
            } catch (InterruptedException ignored) {
                // ignore, cannot happen
            } catch (ExecutionException e) {
                if (logger != null && logger.isLoggable(Level.WARNING)) {
                    logger.warning(
                            MessageFormat.format("An error has occurred while projecting file ''{0}''.",
                                                 datafile.getLocation()));
                }
            }
        }

        return projectedData;
    }

    private class RowTask implements Callable<float[][]> {

        private final Grid[] sourceGrids;
        private final int y;
        private PixelLocator pixelLocator;

        public RowTask(Grid[] sourceGrids, int y, PixelLocator pixelLocator) {
            this.sourceGrids = sourceGrids;
            this.y = y;
            this.pixelLocator = pixelLocator;
        }

        @Override
        public float[][] call() throws Exception {
            if (logger != null && logger.isLoggable(Level.FINE)) {
                logger.fine(MessageFormat.format("Starting computing projection for row {0}.", y));
            }
            final float[][] data = new float[sourceGrids.length][gridDef.getWidth()];

            final Point2D p = new Point2D.Double();
            for (int x = 0; x < gridDef.getWidth(); x++) {
                final double lon = gridDef.getCenterLon(x);
                final double lat = gridDef.getCenterLat(y);
                pixelLocator.getPixelLocation(lon, lat, p);
                if (Double.isNaN(p.getX()) || Double.isNaN(p.getY())) {
                    for (int i = 0; i < sourceGrids.length; ++i) {
                        data[i][x] = Float.NaN; // todo - use no-data value?
                    }
                } else {
                    for (int i = 0; i < sourceGrids.length; ++i) {
                        data[i][x] = (float) sourceGrids[i].getSampleDouble((int) p.getX(), (int) p.getY());
                    }
                }
            }

            if (logger != null && logger.isLoggable(Level.FINE)) {
                logger.fine(MessageFormat.format("Finished computing projection for row {0}.", y));
            }

            return data;
        }
    }

    private static class PixelLocator {

        private final int searchCycleCount = 30;
        private final double targetPixelSize = Math.toRadians(0.05);

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

        Point2D getPixelLocation(final double lon0, final double lat0, Point2D p) {
            if (approximations != null && lon0 >= -180.0 && lon0 <= 180.0 && lat0 >= -90.0 && lat0 <= 90.0) {
                final Approximation approximation = Approximation.findMostSuitable(approximations, lat0, lon0);
                if (approximation != null) {
                    final Rectangle range = approximation.getRange();
                    final Rotator rotator = approximation.getRotator();
                    p.setLocation(lon0, lat0);
                    rotator.transform(p);
                    final double lon = p.getX();
                    final double lat = p.getY();
                    final double x = approximation.getFX().getValue(lat, lon);
                    if (x < range.getMinX() || x > range.getMaxX()) {
                        p.setLocation(Double.NaN, Double.NaN);
                    } else {
                        final double y = approximation.getFY().getValue(lat, lon);
                        if (y < range.getMinY() || y > range.getMaxY()) {
                            p.setLocation(Double.NaN, Double.NaN);
                        } else {
                            p.setLocation(x, y);
                            refinePixelLocation(lon0, lat0, p);
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

        private void refinePixelLocation(final double lon0, final double lat0, Point2D p) {
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

                    // enlarge search region along negative x direction, if pixel is not valid at boundary
                    while (minX > rasterMinX) {
                        if (isValid(minX, y1)) {
                            break;
                        }
                        if (minX > rasterMinX) {
                            minX--;
                        }
                    }
                    // enlarge search region along positive x direction, if pixel is not valid at boundary
                    while (maxX < rasterMaxX) {
                        if (isValid(maxX, y1)) {
                            break;
                        }
                        if (maxX < rasterMaxX) {
                            maxX++;
                        }
                    }
                    // search for pixel closest to (lon0, lat0)
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
                    // if pixel location did not change, terminate loop
                    if (x1 == x0 && y1 == y0) {
                        break;
                    }

                    x0 = x1;
                    y0 = y1;
                }
                if (minDistance <= targetPixelSize) {
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
}
