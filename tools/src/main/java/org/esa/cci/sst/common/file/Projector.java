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

import org.esa.beam.framework.datamodel.RationalFunctionModel;
import org.esa.beam.util.math.MathUtils;
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

import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;


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
        final List<Future<float[][]>> futures = new ArrayList<>(h);
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
                                                 datafile.getLocation())
                    );
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
                final double cellRadius = gridDef.getDiagonal(x, y) / 12742.0;
                pixelLocator.getPixelLocation(lon, lat, p, cellRadius);
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

        Point2D getPixelLocation(final double lon0, final double lat0, Point2D p, double cellRadius) {
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
                            refinePixelLocation(lon0, lat0, p, cellRadius);
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

        private void refinePixelLocation(final double lon0, final double lat0, Point2D p, double cellRadius) {
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

                final SphericalDistance dc = new SphericalDistance(lon0, lat0);

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
                if (minDistance <= cellRadius) {
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

    ///// TODO: stripped-down BEAM5 classes below, to be replaced with BEAM5 classes when BEAM5 is established /////

    private interface SampleSource {

        int getSample(int x, int y);

        double getSampleDouble(int x, int y);
    }

    /**
     * Interface for measuring the distance of a (lon, lat) point to a reference
     * (lon, lat) point, which is defined by the implementing class.
     * <p/>
     * Let p and q denote two points en the Earth. Then, loosely speaking, any
     * distance measure d(p, q) has to satisfy the following properties:
     * <p/>
     * (1) d(p, q) = 0, if p = q
     * <p/>
     * (2) d(p, q) > 0, if p ≠ q
     *
     * @author Ralf Quast
     * @since Version 5.0
     */
    private interface DistanceMeasure {

        /**
         * Returns the distance of a given (lon, lat) point to the reference (lon, lat) point.
         *
         * @param lon The longitude.
         * @param lat The latitude.
         *
         * @return the distance.
         */
        double distance(double lon, double lat);
    }

    /**
     * This class computes the spherical distance (in Radian) between two (lon, lat) points.
     *
     * @author Ralf Quast
     */
    private static final class SphericalDistance implements DistanceMeasure {

        private final double lon;
        private final double si;
        private final double co;

        /**
         * Creates a new instance of this class.
         *
         * @param lon The reference longitude of this distance calculator.
         * @param lat The reference latitude of this distance calculator.
         */
        private SphericalDistance(double lon, double lat) {
            this.lon = lon;
            this.si = Math.sin(Math.toRadians(lat));
            this.co = Math.cos(Math.toRadians(lat));
        }

        /**
         * Returns the spherical distance (in Radian) of a given (lon, lat) point to
         * the reference (lon, lat) point.
         *
         * @param lon The longitude.
         * @param lat The latitude.
         *
         * @return the spherical distance (in Radian) of the given (lon, lat) point
         * to the reference (lon, lat) point.
         */
        @Override
        public double distance(double lon, double lat) {
            final double phi = Math.toRadians(lat);
            return Math.acos(si * Math.sin(phi) + co * Math.cos(phi) * Math.cos(Math.toRadians(lon - this.lon)));
        }
    }

    /**
     * This class uses the negative cosine of the spherical distance as a distance
     * measure between two (lon, lat) points.
     * <p/>
     * This distance measure is computationally much less expensive than the spherical
     * distance, because it is not necessary to evaluate the inverse of the cosine.
     *
     * @author Ralf Quast
     */
    private static final class CosineDistance implements DistanceMeasure {

        private final double lon;
        private final double si;
        private final double co;

        /**
         * Creates a new instance of this class.
         *
         * @param lon The reference longitude of this distance calculator.
         * @param lat The reference latitude of this distance calculator.
         */
        private CosineDistance(double lon, double lat) {
            this.lon = lon;
            this.si = Math.sin(Math.toRadians(lat));
            this.co = Math.cos(Math.toRadians(lat));
        }

        /**
         * Returns the distance of a given (lon, lat) point to
         * the reference (lon, lat) point.
         *
         * @param lon The longitude.
         * @param lat The latitude.
         *
         * @return the distance of the given (lon, lat) point to the
         * reference (lon, lat) point.
         */
        @Override
        public double distance(double lon, double lat) {
            final double phi = Math.toRadians(lat);
            final double cos = si * Math.sin(phi) + co * Math.cos(phi) * Math.cos(Math.toRadians(lon - this.lon));
            return 1.0 - cos;
        }
    }

    private static final class Stepping {

        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;
        private final int pointCountX;
        private final int pointCountY;
        private final int stepX;
        private final int stepY;

        private Stepping(int minX, int minY, int maxX, int maxY, int pointCountX, int pointCountY, int stepX,
                         int stepY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.pointCountX = pointCountX;
            this.pointCountY = pointCountY;
            this.stepX = stepX;
            this.stepY = stepY;
        }

        private int getMinX() {
            return minX;
        }

        private int getMaxX() {
            return maxX;
        }

        private int getMinY() {
            return minY;
        }

        private int getMaxY() {
            return maxY;
        }

        private int getPointCountX() {
            return pointCountX;
        }

        private int getPointCountY() {
            return pointCountY;
        }

        private int getStepX() {
            return stepX;
        }

        private int getStepY() {
            return stepY;
        }

        private int getPointCount() {
            return pointCountX * pointCountY;
        }
    }

    private static interface SteppingFactory {

        Stepping createStepping(Rectangle rectangle, int maxPointCount);
    }

    private static class PixelSteppingFactory implements SteppingFactory {

        @Override
        public Stepping createStepping(Rectangle rectangle, int maxPointCount) {
            final int sw = rectangle.width;
            final int sh = rectangle.height;
            final int minX = rectangle.x;
            final int minY = rectangle.y;
            final int maxX = minX + sw - 1;
            final int maxY = minY + sh - 1;

            // Determine stepX and stepY so that maximum number of points is not exceeded
            int pointCountX = sw;
            int pointCountY = sh;
            int stepX = 1;
            int stepY = 1;

            // Adjust number of warp points to be considered so that a maximum of circa
            // maxPointCount points is not exceeded
            boolean adjustStepX = true;
            while (pointCountX * pointCountY > maxPointCount) {
                if (adjustStepX) {
                    stepX++;
                    pointCountX = sw / stepX + 1;
                } else {
                    stepY++;
                    pointCountY = sh / stepY + 1;
                }
                adjustStepX = !adjustStepX;
            }
            pointCountX = Math.max(1, pointCountX);
            pointCountY = Math.max(1, pointCountY);

            // Make sure we include the right border points,
            // if sw/stepX not divisible without remainder
            if (sw % stepX != 0) {
                pointCountX++;
            }
            // Make sure we include the bottom border points,
            // if sh/stepY not divisible without remainder
            if (sh % stepY != 0) {
                pointCountY++;
            }

            return new Stepping(minX, minY, maxX, maxY, pointCountX, pointCountY, stepX, stepY);
        }
    }

    /**
     * Approximates the x(lat, lon) and y(lat, lon) functions.
     */
    private static final class Approximation {

        private static final int LAT = 0;
        private static final int LON = 1;
        private static final int X = 2;
        private static final int Y = 3;
        private static final int MAX_POINT_COUNT_PER_TILE = 1000;

        private final RationalFunctionModel fX;
        private final RationalFunctionModel fY;
        private final double maxDistance;
        private final Rotator rotator;
        private final DistanceMeasure calculator;
        private final Rectangle range;


        /**
         * Creates an array of approximations for given set of (x, y) rectangles.
         *
         * @param lonSamples  The longitude samples.
         * @param latSamples  The latitude samples.
         * @param maskSamples The mask samples.
         * @param accuracy    The accuracy goal.
         * @param rectangles  The (x, y) rectangles.
         *
         * @return a new approximation or {@code null} if the accuracy goal cannot not be met.
         */
        private static Approximation[] createApproximations(SampleSource lonSamples,
                                                            SampleSource latSamples,
                                                            SampleSource maskSamples,
                                                            double accuracy,
                                                            Rectangle[] rectangles) {
            return createApproximations(lonSamples, latSamples, maskSamples, accuracy, rectangles,
                                        new PixelSteppingFactory());
        }

        /**
         * Creates a new instance of this class.
         *
         * @param lonSamples      The longitude samples.
         * @param latSamples      The latitude samples.
         * @param maskSamples     The mask samples.
         * @param accuracy        The accuracy goal.
         * @param range           The range of the x(lat, lon) and y(lat, lon) functions.
         * @param steppingFactory The stepping factory.
         *
         * @return a new approximation or {@code null} if the accuracy goal cannot not be met.
         */
        private static Approximation create(SampleSource lonSamples,
                                            SampleSource latSamples,
                                            SampleSource maskSamples,
                                            double accuracy,
                                            Rectangle range,
                                            SteppingFactory steppingFactory) {
            final Stepping stepping = steppingFactory.createStepping(range, MAX_POINT_COUNT_PER_TILE);
            final double[][] data = extractWarpPoints(lonSamples, latSamples, maskSamples, stepping);
            return Approximation.create(data, accuracy, range);
        }

        /**
         * Creates a new instance of this class.
         *
         * @param data     The array of (lat, lon, x, y) points that is used to compute the approximations to the
         *                 x(lat, lon) and y(lat, lon) functions. Note that the contents of the data array is modified
         *                 by this method.
         * @param accuracy The accuracy goal.
         * @param range    The range of the x(lat, lon) and y(lat, lon) functions.
         *
         * @return a new approximation or {@code null} if the accuracy goal cannot not be met.
         */
        private static Approximation create(double[][] data, double accuracy, Rectangle range) {
            final Point2D centerPoint = Rotator.calculateCenter(data, LON, LAT);
            final double centerLon = centerPoint.getX();
            final double centerLat = centerPoint.getY();
            // the equation below is correct, if and only if, the cosine distance is used for calculating distances
            final double maxDistance = 1.0 - Math.cos(1.1 * Math.acos(1.0 - maxDistance(data, centerLon, centerLat)));

            final Rotator rotator = new Rotator(centerLon, centerLat);
            rotator.transform(data, LON, LAT);

            final int[] xIndices = new int[]{LAT, LON, X};
            final int[] yIndices = new int[]{LAT, LON, Y};

            final RationalFunctionModel fX = findBestModel(data, xIndices, accuracy);
            final RationalFunctionModel fY = findBestModel(data, yIndices, accuracy);
            if (fX == null || fY == null) {
                return null;
            }

            return new Approximation(fX, fY, maxDistance, rotator, new CosineDistance(centerLon, centerLat), range);
        }

        /**
         * Among several approximations, returns the approximation that is most suitable for a given (lat, lon) point.
         *
         * @param approximations The approximations.
         * @param lat            The latitude.
         * @param lon            The longitude.
         *
         * @return the approximation that is most suitable for the given (lat, lon) point,
         * or {@code null}, if none is suitable.
         */
        private static Approximation findMostSuitable(Approximation[] approximations, double lat, double lon) {
            Approximation bestApproximation = null;
            if (approximations.length == 1) {
                Approximation a = approximations[0];
                final double distance = a.getDistance(lat, lon);
                if (distance < a.getMaxDistance()) {
                    bestApproximation = a;
                }
            } else {
                double minDistance = Double.MAX_VALUE;
                for (final Approximation a : approximations) {
                    final double distance = a.getDistance(lat, lon);
                    if (distance < minDistance && distance < a.getMaxDistance()) {
                        minDistance = distance;
                        bestApproximation = a;
                    }
                }
            }
            return bestApproximation;
        }

        private static Approximation[] createApproximations(SampleSource lonSamples,
                                                            SampleSource latSamples,
                                                            SampleSource maskSamples,
                                                            double accuracy,
                                                            Rectangle[] rectangles,
                                                            SteppingFactory steppingFactory) {
            final ArrayList<Approximation> approximations = new ArrayList<>(rectangles.length);
            for (final Rectangle rectangle : rectangles) {
                final Approximation approximation = create(lonSamples, latSamples, maskSamples, accuracy,
                                                           rectangle, steppingFactory);
                if (approximation == null) {
                    return null;
                }
                approximations.add(approximation);
            }

            return approximations.toArray(new Approximation[approximations.size()]);
        }

        /**
         * Returns the (approximation to) the x(lat, lon) function.
         *
         * @return the (approximation to) the x(lat, lon) function.
         */
        private RationalFunctionModel getFX() {
            return fX;
        }

        /**
         * Returns the (approximation to) the y(lat, lon) function.
         *
         * @return the (approximation to) the y(lat, lon) function.
         */
        private RationalFunctionModel getFY() {
            return fY;
        }

        /**
         * Returns the maximum distance (in radian) within which this approximation is valid.
         *
         * @return the maximum distance (in radian).
         */
        private double getMaxDistance() {
            return maxDistance;
        }

        /**
         * Returns the distance (in radian) of 'the center of this approximation' to a given (lat, lon) point.
         *
         * @param lat The latitude.
         * @param lon The longitude.
         *
         * @return the distance (in radian).
         */
        private double getDistance(double lat, double lon) {
            return calculator.distance(lon, lat);
        }

        /**
         * Returns the {@code Rotator} associated with this approximation.
         *
         * @return the {@code Rotator} associated with this approximation.
         */
        private Rotator getRotator() {
            return rotator;
        }

        /**
         * Returns the range of the x(lat, lon) and y(lat, lon) functions.
         *
         * @return the range of the x(lat, lon) and y(lat, lon) functions.
         */
        private Rectangle getRange() {
            return range;
        }

        private Approximation(RationalFunctionModel fX,
                              RationalFunctionModel fY,
                              double maxDistance,
                              Rotator rotator, DistanceMeasure calculator, Rectangle range) {
            this.fX = fX;
            this.fY = fY;
            this.maxDistance = maxDistance;
            this.rotator = rotator;
            this.calculator = calculator;
            this.range = range;
        }

        private static double maxDistance(final double[][] data, double centerLon, double centerLat) {
            final DistanceMeasure distanceMeasure = new CosineDistance(centerLon, centerLat);
            double maxDistance = 0.0;
            for (final double[] p : data) {
                final double d = distanceMeasure.distance(p[LON], p[LAT]);
                if (d > maxDistance) {
                    maxDistance = d;
                }
            }
            return maxDistance;
        }

        private static RationalFunctionModel findBestModel(double[][] data, int[] indexes, double accuracy) {
            RationalFunctionModel bestModel = null;
            search:
            for (int degreeP = 0; degreeP <= 4; degreeP++) {
                for (int degreeQ = 0; degreeQ <= degreeP; degreeQ++) {
                    final int termCountP = RationalFunctionModel.getTermCountP(degreeP);
                    final int termCountQ = RationalFunctionModel.getTermCountQ(degreeQ);
                    if (data.length >= termCountP + termCountQ) {
                        final RationalFunctionModel model = createModel(degreeP, degreeQ, data, indexes);
                        if (bestModel == null || model.getRmse() < bestModel.getRmse()) {
                            bestModel = model;
                        }
                        if (bestModel.getRmse() < accuracy) {
                            break search;
                        }
                    }
                }
            }
            return bestModel;
        }

        private static RationalFunctionModel createModel(int degreeP, int degreeQ, double[][] data, int[] indexes) {
            final int ix = indexes[0];
            final int iy = indexes[1];
            final int iz = indexes[2];
            final double[] x = new double[data.length];
            final double[] y = new double[data.length];
            final double[] g = new double[data.length];
            for (int i = 0; i < data.length; i++) {
                x[i] = data[i][ix];
                y[i] = data[i][iy];
                g[i] = data[i][iz];
            }

            return new RationalFunctionModel(degreeP, degreeQ, x, y, g);
        }

        private static double[][] extractWarpPoints(SampleSource lonSamples,
                                                    SampleSource latSamples,
                                                    SampleSource maskSamples,
                                                    Stepping stepping) {
            final int minX = stepping.getMinX();
            final int maxX = stepping.getMaxX();
            final int minY = stepping.getMinY();
            final int maxY = stepping.getMaxY();
            final int pointCountX = stepping.getPointCountX();
            final int pointCountY = stepping.getPointCountY();
            final int stepX = stepping.getStepX();
            final int stepY = stepping.getStepY();
            final int pointCount = stepping.getPointCount();
            final List<double[]> pointList = new ArrayList<>(pointCount);

            for (int j = 0, k = 0; j < pointCountY; j++) {
                int y = minY + j * stepY;
                // adjust bottom border
                if (y > maxY) {
                    y = maxY;
                }
                for (int i = 0; i < pointCountX; i++, k++) {
                    int x = minX + i * stepX;
                    // adjust right border
                    if (x > maxX) {
                        x = maxX;
                    }
                    final int mask = maskSamples.getSample(x, y);
                    if (mask != 0) {
                        final double lat = latSamples.getSampleDouble(x, y);
                        final double lon = lonSamples.getSampleDouble(x, y);
                        if (lon >= -180.0 && lon <= 180.0 && lat >= -90.0 && lat <= 90.0) {
                            final double[] point = new double[4];
                            point[LAT] = lat;
                            point[LON] = lon;
                            point[X] = x + 0.5;
                            point[Y] = y + 0.5;
                            pointList.add(point);
                        }
                    }
                }
            }

            return pointList.toArray(new double[pointList.size()][4]);
        }
    }

    /**
     * Class for rotating geographical positions.
     * The rotator makes the given coordinate the origin <code>(0.0, 0.0)</code> of the coordinate system.
     * If the coordinate which shall become the north pole of the new coordinate system is known the latitude value given into the
     * constructor must be changed. Instead of providing directly the latitude value, it must be <code>90 - latitude</code>.
     *
     * @author Ralf Quast
     * @version $Revision$ $Date$
     */
    private static final class Rotator {

        private final double a11;
        private final double a12;
        private final double a13;
        private final double a21;
        private final double a22;
        private final double a23;
        private final double a31;
        private final double a32;
        private final double a33;

        /**
         * Constructs a new rotation. In the rotated system, the point defined
         * by the (lon, lat) parameters is located at the origin, i.e. has the
         * geographical coordinates (0, 0).
         * <p/>
         * The new rotation is composed of a clockwise rotation about the z-axis
         * (which corresponds to S-N direction) and a counter-clockwise rotation
         * about the y-axis (which corresponds to W-E direction).
         *
         * @param lon the geographical longitude of the point becoming the new
         *            origin.
         * @param lat the geographical latitude of the point becoming the new
         *            origin.
         */
        private Rotator(double lon, double lat) {
            this(lon, lat, 0.0);
        }

        /**
         * Constructs a new rotation. In the rotated system, the point defined
         * by the (lon, lat) parameters is located at the origin, i.e. has the
         * geographical coordinates (0, 0).
         * <p/>
         * The new rotation is composed of a clockwise rotation about the z-axis
         * (which corresponds to S-N direction), a counter-clockwise rotation
         * about the y-axis (which corresponds to W-E direction), and a counter
         * clockwise rotation about the x-axis by the angle alpha.
         *
         * @param lon   the geographical longitude of the point becoming the new
         *              origin.
         * @param lat   the geographical latitude of the point becoming the new
         *              origin.
         * @param alpha the rotation angle (degrees) corresponding to the x-axis.
         */
        private Rotator(double lon, double lat, double alpha) {

            final double u = toRadians(lon);
            final double v = toRadians(lat);
            final double w = toRadians(alpha);

            final double cu = cos(u);
            final double cv = cos(v);
            final double cw = cos(w);
            final double su = sin(u);
            final double sv = sin(v);
            final double sw = sin(w);

            a11 = cu * cv;
            a12 = su * cv;
            a13 = sv;

            a21 = sw * (cu * sv) - su * cw;
            a22 = cw * cu + sw * (su * sv);
            a23 = -sw * cv;

            a31 = sw * -su - cw * (cu * sv);
            a32 = sw * cu - cw * (su * sv);
            a33 = cw * cv;
        }

        private static Point2D calculateCenter(double[][] data, int lonIndex, int latIndex) {
            // calculate (x, y, z) in order to avoid issues with anti-meridian and poles
            final int size = data.length;
            final double[] x = new double[size];
            final double[] y = new double[size];
            final double[] z = new double[size];

            calculateXYZ(data, x, y, z, lonIndex, latIndex);

            double xc = 0.0;
            double yc = 0.0;
            double zc = 0.0;
            for (int i = 0; i < size; i++) {
                xc += x[i];
                yc += y[i];
                zc += z[i];
            }
            final double length = Math.sqrt(xc * xc + yc * yc + zc * zc);
            xc /= length;
            yc /= length;
            zc /= length;

            final double lat = toDegrees(asin(zc));
            final double lon = toDegrees(atan2(yc, xc));

            return new Point2D.Double(lon, lat);
        }

        private static void calculateXYZ(double[][] data, double[] x, double[] y, double[] z, int lonIndex,
                                         int latIndex) {
            for (int i = 0; i < data.length; i++) {
                final double lon = data[i][lonIndex];
                final double lat = data[i][latIndex];
                final double u = toRadians(lon);
                final double v = toRadians(lat);
                final double w = cos(v);

                x[i] = cos(u) * w;
                y[i] = sin(u) * w;
                z[i] = sin(v);
            }
        }

        /**
         * Transforms a given geographical point into the rotated coordinate
         * system.
         *
         * @param point the point.
         */
        private void transform(Point2D point) {
            double lon = point.getX();
            double lat = point.getY();

            final double u = toRadians(lon);
            final double v = toRadians(lat);

            final double w = cos(v);
            final double x = cos(u) * w;
            final double y = sin(u) * w;
            final double z = sin(v);

            final double x2 = a11 * x + a12 * y + a13 * z;
            final double y2 = a21 * x + a22 * y + a23 * z;
            final double z2 = a31 * x + a32 * y + a33 * z;

            lat = toDegrees(asin(z2));
            lon = toDegrees(atan2(y2, x2));

            point.setLocation(lon, lat);
        }

        private void transform(double[][] data, final int lonIndex, final int latIndex) {
            for (final double[] p : data) {
                final double u = toRadians(p[lonIndex]);
                final double v = toRadians(p[latIndex]);

                final double w = cos(v);
                final double x = cos(u) * w;
                final double y = sin(u) * w;
                final double z = sin(v);

                final double x2 = a11 * x + a12 * y + a13 * z;
                final double y2 = a21 * x + a22 * y + a23 * z;
                final double z2 = a31 * x + a32 * y + a33 * z;

                p[lonIndex] = toDegrees(atan2(y2, x2));
                p[latIndex] = toDegrees(asin(z2));
            }
        }

    }
}
