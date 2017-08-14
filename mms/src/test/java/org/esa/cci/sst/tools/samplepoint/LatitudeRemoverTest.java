package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LatitudeRemoverTest {

    @Test
    public void testRemove_emptyList() {
        final LatitudeRemover latitudeRemover = new LatitudeRemover(45.8, Double.NaN);
        final List<SamplingPoint> samples = new ArrayList<>();

        final List<SamplingPoint> removed = latitudeRemover.remove(samples);
        assertEquals(0, removed.size());
    }

    @Test
    public void testRemove_onlyMin() {
        final LatitudeRemover latitudeRemover = new LatitudeRemover(Double.NaN, -78.5);
        final List<SamplingPoint> samples = new ArrayList<>();
        samples.add(new SamplingPoint(108.6, -64.5, 0, 0)); // remove
        samples.add(new SamplingPoint(109.7, -78.6, 0, 0)); // keep
        samples.add(new SamplingPoint(110.8, -78.4, 0, 0)); // remove
        samples.add(new SamplingPoint(111.9, -69.5, 0, 0)); // remove
        samples.add(new SamplingPoint(113.0, -82.5, 0, 0)); // keep

        final List<SamplingPoint> removed = latitudeRemover.remove(samples);
        assertEquals(2, removed.size());

        assertEquals(-78.6, removed.get(0).getLat(), 1e-8);
        assertEquals(-82.5, removed.get(1).getLat(), 1e-8);
    }

    @Test
    public void testRemove_onlyMax() {
        final LatitudeRemover latitudeRemover = new LatitudeRemover(62.8, Double.NaN);
        final List<SamplingPoint> samples = new ArrayList<>();
        samples.add(new SamplingPoint(108.6, 64.5, 0, 0)); // keep
        samples.add(new SamplingPoint(109.7, 78.6, 0, 0)); // keep
        samples.add(new SamplingPoint(110.8, 19.4, 0, 0)); // remove
        samples.add(new SamplingPoint(111.9, -69.5, 0, 0)); // remove
        samples.add(new SamplingPoint(113.0, -12.5, 0, 0)); // remove

        final List<SamplingPoint> removed = latitudeRemover.remove(samples);
        assertEquals(2, removed.size());

        assertEquals(64.5, removed.get(0).getLat(), 1e-8);
        assertEquals(78.6, removed.get(1).getLat(), 1e-8);
    }

    @Test
    public void testRemove_minAndMax() {
        final LatitudeRemover latitudeRemover = new LatitudeRemover(62.8, -55.3);
        final List<SamplingPoint> samples = new ArrayList<>();
        samples.add(new SamplingPoint(108.6, 64.5, 0, 0)); // keep
        samples.add(new SamplingPoint(109.7, -72.5, 0, 0)); // keep
        samples.add(new SamplingPoint(110.8, 19.4, 0, 0)); // remove
        samples.add(new SamplingPoint(111.9, -12.5, 0, 0)); // remove
        samples.add(new SamplingPoint(113.0, -62.5, 0, 0)); // keep

        final List<SamplingPoint> removed = latitudeRemover.remove(samples);
        assertEquals(3, removed.size());

        assertEquals(64.5, removed.get(0).getLat(), 1e-8);
        assertEquals(-72.5, removed.get(1).getLat(), 1e-8);
        assertEquals(-62.5, removed.get(2).getLat(), 1e-8);
    }
}
