package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;

import java.util.ArrayList;
import java.util.List;

public class LatitudeRemover {

    final private double maxThreshold;
    final private double minThreshold;
    final private boolean removeMax;
    final private boolean removeMin;

    public LatitudeRemover(double maxLat, double minLat) {
        maxThreshold = maxLat;
        minThreshold = minLat;

        removeMin = !Double.isNaN(minLat);
        removeMax = !Double.isNaN(maxLat);
    }

    public void remove(List<SamplingPoint> samples) {
        final List<SamplingPoint> clearedSamples = new ArrayList<>(samples.size());

        for (final SamplingPoint point : samples) {
            final double lat = point.getLat();
            boolean keep = true;

            if (removeMin && removeMax) {
                if ((lat > minThreshold) && (lat < maxThreshold)) {
                    keep = false;
                }
            } else {
                if (removeMin) {
                    if (lat > minThreshold) {
                        keep = false;
                    }
                }
                if (removeMax) {
                    if (lat < maxThreshold) {
                        keep = false;
                    }
                }
            }

            if (keep) {
                clearedSamples.add(point);
            }
        }
        samples.clear();
        samples.addAll(clearedSamples);
    }
}
