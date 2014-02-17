package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.util.SamplingPoint;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SamplePointExporter {


    // package access for testing only tb 2014-02-14
    static List<SamplingPoint> extractSamples(List<SamplingPoint> samples, TimeRange extractRange) {
        final LinkedList<SamplingPoint> extracted = new LinkedList<>();

        final Iterator<SamplingPoint> iterator = samples.iterator();
        while (iterator.hasNext()) {
            final SamplingPoint point = iterator.next();
            if (extractRange.includes(new Date(point.getTime()))) {
                extracted.add(point);
                iterator.remove();
            }
        }
        return extracted;
    }
}
