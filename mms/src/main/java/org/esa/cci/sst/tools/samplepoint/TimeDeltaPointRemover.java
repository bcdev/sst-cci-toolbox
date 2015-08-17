package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;

import java.util.ArrayList;
import java.util.List;

public class TimeDeltaPointRemover {

    public List<SamplingPoint> removeSamples(List<SamplingPoint> sampleList, long timeDeltaMax) {
        final List<SamplingPoint> timeDeltaCleanedSamples = new ArrayList<>(sampleList.size());
        for (final SamplingPoint point : sampleList) {
            if (Math.abs(point.getReferenceTime() - point.getReference2Time()) <= timeDeltaMax) {
                timeDeltaCleanedSamples.add(point);
            }
        }
        sampleList.clear();
        sampleList.addAll(timeDeltaCleanedSamples);
        return sampleList;
    }
}
