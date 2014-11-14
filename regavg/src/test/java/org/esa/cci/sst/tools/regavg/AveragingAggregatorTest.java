package org.esa.cci.sst.tools.regavg;

import org.esa.cci.sst.grid.RegionMask;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AveragingAggregatorTest {

    @Test
    public void testMustAggregateTo90() {
        final RegionMask globe = RegionMask.create("globe", -180, 90, 180, -90);
        assertTrue(AveragingAggregator.mustAggregateTo90(globe));

        final RegionMask north = RegionMask.create("north", -180, 90, 180, 0);
        assertTrue(AveragingAggregator.mustAggregateTo90(north));

        final RegionMask south = RegionMask.create("north", -180, 0, 180, -90);
        assertTrue(AveragingAggregator.mustAggregateTo90(south));

        final RegionMask any = RegionMask.create("any", -120, 23, -111, 11);
        assertFalse(AveragingAggregator.mustAggregateTo90(any));
    }
}
