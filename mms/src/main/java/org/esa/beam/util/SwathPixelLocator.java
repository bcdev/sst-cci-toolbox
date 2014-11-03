package org.esa.beam.util;

import org.esa.beam.framework.datamodel.GeoApproximation;
import org.esa.beam.framework.datamodel.PixelLocator;
import org.esa.beam.util.math.CosineDistance;
import org.esa.beam.util.math.DistanceMeasure;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author Ralf Quast
 */
class SwathPixelLocator extends AbstractPixelLocator {

    private final PixelLocationEstimator estimator;
    private final PixelLocationSearcher searcher;

    static PixelLocator create(RasterDataNodeSampleSource lonSource,
                               RasterDataNodeSampleSource latSource) {
        final PlanarImage lonImage = lonSource.getNode().getGeophysicalImage();
        final PlanarImage latImage = latSource.getNode().getGeophysicalImage();
        final PlanarImage maskImage = lonSource.getNode().getValidMaskImage();

        final GeoApproximation[] approximations = createApproximations(lonImage, latImage, maskImage);
        final Rectangle bounds = new Rectangle(0, 0, lonImage.getWidth(), latImage.getHeight());
        final PixelLocationEstimator estimator = new PixelLocationEstimator(approximations);

        final SampleSource maskSource;
        if (maskImage != null) {
            maskSource = new MaskSampleSource(maskImage);
        } else {
            maskSource = null;
        }
        final PixelLocationSearcher searcher = new PixelLocationSearcher(lonSource,
                                                                         latSource,
                                                                         maskSource
        );

        return new SwathPixelLocator(lonSource, latSource, estimator, searcher);
    }

    private SwathPixelLocator(SampleSource lonSource, SampleSource latSource,
                              PixelLocationEstimator estimator, PixelLocationSearcher searcher) {
        super(lonSource, latSource);
        this.estimator = estimator;
        this.searcher = searcher;
    }

    @Override
    public boolean getPixelLocation(double lon, double lat, Point2D p) {
        return estimator.estimatePixelLocation(lon, lat, p) && searcher.searchPixelLocation(lon, lat, p);
    }

    private static GeoApproximation[] createApproximations(PlanarImage lonImage,
                                                           PlanarImage latImage,
                                                           PlanarImage maskImage) {
        return GeoApproximation.createApproximations(lonImage, latImage, maskImage, 0.5);
    }

    private static final class PixelLocationEstimator {

        private final GeoApproximation[] approximations;

        public PixelLocationEstimator(GeoApproximation[] approximations) {
            this.approximations = approximations;
        }

        public boolean estimatePixelLocation(double lon, double lat, Point2D p) {
            GeoApproximation approximation;
            if (approximations != null) {
                approximation = findMostSuitable(approximations, lat, lon);
                if (approximation != null) {
                    p.setLocation(lon, lat);
                    g2p(approximation, p);
                } else {
                    return false;
                }
            }
            return true;
        }

        private static GeoApproximation findMostSuitable(GeoApproximation[] approximations, double lat, double lon) {
            GeoApproximation bestApproximation = null;
            if (approximations.length == 1) {
                GeoApproximation a = approximations[0];
                final double distance = a.getDistance(lat, lon);
                if (distance < a.getMaxDistance()) {
                    bestApproximation = a;
                }
            } else {
                double minDistance = Double.MAX_VALUE;
                for (final GeoApproximation a : approximations) {
                    final double distance = a.getDistance(lat, lon);
                    if (distance < minDistance && distance < a.getMaxDistance()) {
                        minDistance = distance;
                        bestApproximation = a;
                    }
                }
            }
            return bestApproximation;
        }

        private static void g2p(GeoApproximation geoApproximation, Point2D g) {
            geoApproximation.getRotator().transform(g);
            final double lon = g.getX();
            final double lat = g.getY();
            final double x = geoApproximation.getFX().getValue(lat, lon);
            final double y = geoApproximation.getFY().getValue(lat, lon);
            g.setLocation(x, y);
        }
    }

    private static final class PixelLocationSearcher {

        private static final int R = 128;

        private final SampleSource lonSource;
        private final SampleSource latSource;
        private final SampleSource maskSource;
        private final int sourceW;
        private final int sourceH;

        public PixelLocationSearcher(SampleSource lonSource, SampleSource latSource, SampleSource maskSource) {
            this.lonSource = lonSource;
            this.latSource = latSource;
            this.maskSource = maskSource;

            sourceW = lonSource.getWidth();
            sourceH = lonSource.getHeight();
        }

        public boolean searchPixelLocation(double lon, double lat, Point2D p) {
            int x = (int) Math.floor(p.getX());
            int y = (int) Math.floor(p.getY());

            if (x < 0) {
                x = 0;
            } else if (x >= sourceW) {
                x = sourceW - 1;
            }
            if (y < 0) {
                y = 0;
            } else if (y >= sourceH) {
                y = sourceH - 1;
            }

            final int minX = max(x - R, 0);
            final int minY = max(y - R, 0);
            final int maxX = min(x + R, sourceW - 1);
            final int maxY = min(y + R, sourceH - 1);

            final DistanceMeasure d = new CosineDistance(lon, lat);
            final Result result = new Result(d, x, y, 2.0).invoke(x, y);

            for (int r = R; r > 0; r >>= 1) {
                final int midX = result.getX();
                final int midY = result.getY();

                final int outerMinX = max(minX, midX - r);
                final int outerMaxX = min(maxX, midX + r);
                final int outerMinY = max(minY, midY - r);
                final int outerMaxY = min(maxY, midY + r);

                // consider outer points in the N, S, E, and W
                result.invoke(outerMinX, midY);
                result.invoke(outerMaxX, midY);
                result.invoke(midX, outerMaxY);
                result.invoke(midX, outerMinY);
                // consider outer points in the NW, SW, SE, and NE
                result.invoke(outerMinX, outerMinY);
                result.invoke(outerMinX, outerMaxY);
                result.invoke(outerMaxX, outerMaxY);
                result.invoke(outerMaxX, outerMinY);

                //noinspection ConstantConditions,ConstantIfStatement
                if (true) {
                    // consider inner points in the NW, SW, SE, and NE
                    final int innerMinX = max(outerMinX, midX - (r >> 1));
                    final int innerMaxX = min(outerMaxX, midX + (r >> 1));
                    final int innerMinY = max(outerMinY, midY - (r >> 1));
                    final int innerMaxY = min(outerMaxY, midY + (r >> 1));

                    result.invoke(innerMinX, innerMinY);
                    result.invoke(innerMinX, innerMaxY);
                    result.invoke(innerMaxX, innerMaxY);
                    result.invoke(innerMaxX, innerMinY);
                }
            }
            final boolean found = result.getX() > minX && result.getX() < maxX && result.getY() > minY && result.getY() < maxY;
            if (found) {
                p.setLocation(result.getX() + 0.5, result.getY() + 0.5);
            }

            return found;
        }

        private final class Result {

            private final DistanceMeasure distanceMeasure;

            private int x;
            private int y;
            private double distance;

            public Result(DistanceMeasure distanceMeasure, int x, int y, double distance) {
                this.distanceMeasure = distanceMeasure;
                this.x = x;
                this.y = y;
                this.distance = distance;
            }

            public int getX() {
                return x;
            }

            public int getY() {
                return y;
            }

            public Result invoke(int otherX, int otherY) {
                if (maskSource == null || maskSource.getSample(otherX, otherY) != 0.0) {
                    final double lon = lonSource.getSample(otherX, otherY);
                    final double lat = latSource.getSample(otherX, otherY);
                    final double d = distanceMeasure.distance(lon, lat);

                    if (d < distance) {
                        x = otherX;
                        y = otherY;
                        distance = d;
                    }
                }
                return this;
            }
        }

    }

}
