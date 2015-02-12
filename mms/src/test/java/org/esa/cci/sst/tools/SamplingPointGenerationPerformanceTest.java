package org.esa.cci.sst.tools;


import org.esa.cci.sst.TestHelper;
import org.esa.cci.sst.tools.samplepoint.SobolSamplePointGenerator;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.StopWatch;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

@Ignore
public class SamplingPointGenerationPerformanceTest {


    @Test
    public void testGenerateAndExport() throws IOException {
        final int numSamples = 500000;

        final SobolSamplePointGenerator sobolSamplePointGenerator = new SobolSamplePointGenerator();

        TestHelper.traceMemory();
        final StopWatch stopWatch = new StopWatch();
        System.out.println("Generating " + numSamples + " sampling points");
        stopWatch.start();

        final List<SamplingPoint> samples = sobolSamplePointGenerator.createSamples(numSamples, 0, new Date().getTime(), new Date().getTime());

        stopWatch.stop();
        System.out.println("Done " + stopWatch.getElapsedMillis() / 1000.0 + " sec");
        TestHelper.traceMemory();

        final DummyLandPointRemover landPointRemover = new DummyLandPointRemover(0.95);
        System.out.println("LandPointRemoval");
        stopWatch.start();

        landPointRemover.removeSamples(samples);

        stopWatch.stop();
        System.out.println("Done " + stopWatch.getElapsedMillis() / 1000.0 + " sec, remaining " + samples.size());
        TestHelper.traceMemory();

        final DummyClearSkyRemover dummyClearSkyRemover = new DummyClearSkyRemover(0.93);
        System.out.println("ClearSkyRemoval");
        stopWatch.start();

        dummyClearSkyRemover.removeSamples(samples);

        stopWatch.stop();
        System.out.println("Done " + stopWatch.getElapsedMillis() / 1000.0 + " sec, remaining " + samples.size());
        TestHelper.traceMemory();

        final DummyFindObservations dummyFindObservations = new DummyFindObservations(0.89);
        System.out.println("FindObservations");
        stopWatch.start();

        dummyFindObservations.removeSamples(samples);

        stopWatch.stop();
        System.out.println("Done " + stopWatch.getElapsedMillis() / 1000.0 + " sec, remaining " + samples.size());
        TestHelper.traceMemory();

        final DummyExporter_split dummyExporter_split = new DummyExporter_split(0.1, 0.82, 0.89, true);
        System.out.println("Split list");
        stopWatch.start();

        dummyExporter_split.split(samples);

        stopWatch.stop();
        System.out.println("Done " + stopWatch.getElapsedMillis() / 1000.0 + " sec");
        TestHelper.traceMemory();
    }

    private class DummyLandPointRemover {

        private final double threshold;

        public DummyLandPointRemover(double threshold) {
            this.threshold = threshold;
        }

        public void removeSamples(List<SamplingPoint> samples) {
            final List<SamplingPoint> waterSamples = new ArrayList<>(samples.size());
            for (final SamplingPoint point : samples) {
                if (point.getRandom() < threshold) {
                    waterSamples.add(point);
                }
            }

            samples.clear();
            samples.addAll(waterSamples);
        }
    }

    private class DummyClearSkyRemover {

        private final double threshold;

        public DummyClearSkyRemover(double threshold) {
            this.threshold = threshold;
        }

        public void removeSamples(List<SamplingPoint> samples) {
            final List<SamplingPoint> remainingSamples = new ArrayList<>(samples.size());

            for (final SamplingPoint point : samples) {
                if (point.getRandom() <= threshold) {
                    remainingSamples.add(point);
                }
            }

            samples.clear();
            samples.addAll(remainingSamples);
        }
    }

    private class DummyFindObservations {

        private final double threshold;

        public DummyFindObservations(double threshold) {
            this.threshold = threshold;
        }

        public void removeSamples(List<SamplingPoint> samples) {
            final List<SamplingPoint> remainingSamples = new ArrayList<>(samples.size());

            for (final SamplingPoint point : samples) {
                if (point.getRandom() <= threshold) {
                    remainingSamples.add(point);
                }
            }

            samples.clear();
            samples.addAll(remainingSamples);
        }
    }

    private class DummyExporter_split {

        private final double low;
        private final double mid;
        private final double high;
        private final boolean array;

        public DummyExporter_split(double low, double mid, double high, boolean array) {
            this.mid = mid;
            this.low = low;
            this.high = high;
            this.array = array;
        }

        public void split(List<SamplingPoint> samples) {
            final List<SamplingPoint> pointsMonthBefore = extractSamples(samples, low);
            final List<SamplingPoint> pointsCenterMonth = extractSamples(samples, mid);
            final List<SamplingPoint> pointsMonthAfter = extractSamples(samples, high);
        }

        private List<SamplingPoint> extractSamples(List<SamplingPoint> samples, double threshold) {
            if (!array) {
                final LinkedList<SamplingPoint> extracted = new LinkedList<>();

                final Iterator<SamplingPoint> iterator = samples.iterator();
                while (iterator.hasNext()) {
                    final SamplingPoint point = iterator.next();
                    if (point.getRandom() < threshold) {
                        extracted.add(point);
                        iterator.remove();
                    }
                }
                return extracted;
            } else {
                final List<SamplingPoint> extracted = new LinkedList<>();
                final List<SamplingPoint> remaining = new ArrayList<>(samples.size());

                for (final SamplingPoint point : samples) {
                    if (point.getRandom() < threshold) {
                        extracted.add(point);
                    } else {
                        remaining.add(point);
                    }
                }
                samples.clear();
                samples.addAll(remaining);

                return extracted;
            }
        }
    }
}
