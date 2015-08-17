package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TimeDeltaPointRemoverTest {

    private TimeDeltaPointRemover remover;
    private ArrayList<SamplingPoint> sampleList;

    @Before
    public void setUp() {
        remover = new TimeDeltaPointRemover();
        sampleList = new ArrayList<>();
    }

    @Test
    public void testRemove_emptyList() {
        final List<SamplingPoint> cleanedList = remover.removeSamples(sampleList, 500L);
        assertEquals(0, cleanedList.size());
    }

    @Test
    public void testRemove_oneSample_matchingInterval() {
        final SamplingPoint samplingPoint = createSamplingPoint(1000000000L, 1000000020L);
        sampleList.add(samplingPoint);

        final List<SamplingPoint> cleanedList = remover.removeSamples(sampleList, 500L);
        assertEquals(1, cleanedList.size());
    }

    @Test
    public void testRemove_oneSample_matchingInterval_swapped() {
        final SamplingPoint samplingPoint = createSamplingPoint(1000000020L, 1000000000L);
        sampleList.add(samplingPoint);

        final List<SamplingPoint> cleanedList = remover.removeSamples(sampleList, 500L);
        assertEquals(1, cleanedList.size());
    }

    @Test
    public void testRemove_oneSample_intervalTooLarge() {
        final SamplingPoint samplingPoint = createSamplingPoint(1000000000L, 1000000510L);
        sampleList.add(samplingPoint);

        final List<SamplingPoint> cleanedList = remover.removeSamples(sampleList, 500L);
        assertEquals(0, cleanedList.size());
    }

    @Test
    public void testRemove_oneSample_intervalTooLarge_swapped() {
        final SamplingPoint samplingPoint = createSamplingPoint(1000000510L, 1000000000L);
        sampleList.add(samplingPoint);

        final List<SamplingPoint> cleanedList = remover.removeSamples(sampleList, 500L);
        assertEquals(0, cleanedList.size());
    }

    @Test
    public void testRemove_manySamples() {
        sampleList.add(createSamplingPoint(1000001510L, 1000000000L));      // removed
        sampleList.add(createSamplingPoint(1000000000L, 1000000998L));      // kept
        sampleList.add(createSamplingPoint(1000001000L, 1000001895L));      // kept
        sampleList.add(createSamplingPoint(1000001000L, 1000002023L));      // removed
        sampleList.add(createSamplingPoint(1000002000L, 1000001000L));      // kept
        sampleList.add(createSamplingPoint(1000003000L, 1000004010L));      // removed

        final List<SamplingPoint> cleanedList = remover.removeSamples(sampleList, 1000L);
        assertEquals(3, cleanedList.size());
        SamplingPoint cleanedPoint = cleanedList.get(0);
        assertEquals(1000000000L, cleanedPoint.getReferenceTime());
        assertEquals(1000000998L, cleanedPoint.getReference2Time());

        cleanedPoint = cleanedList.get(1);
        assertEquals(1000001000L, cleanedPoint.getReferenceTime());
        assertEquals(1000001895L, cleanedPoint.getReference2Time());

        cleanedPoint = cleanedList.get(2);
        assertEquals(1000002000L, cleanedPoint.getReferenceTime());
        assertEquals(1000001000L, cleanedPoint.getReference2Time());
    }

    private SamplingPoint createSamplingPoint(long referenceTime, long reference2Time) {
        final SamplingPoint samplingPoint = new SamplingPoint();
        samplingPoint.setReferenceTime(referenceTime);
        samplingPoint.setReference2Time(reference2Time);
        return samplingPoint;
    }
}
