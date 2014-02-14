package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.util.SamplingPoint;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertTrue;

class TestHelper {

    static void assertPointsInTimeRange(Date startDate, Date stopDate, List<SamplingPoint> inSituPoints) {
        final TimeRange timeRange = new TimeRange(startDate, stopDate);
        for (SamplingPoint next : inSituPoints) {
            assertTrue(timeRange.isWithin(new Date(next.getTime())));
        }
    }
}
