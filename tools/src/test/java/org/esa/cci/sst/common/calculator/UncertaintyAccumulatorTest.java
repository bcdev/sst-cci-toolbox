package org.esa.cci.sst.common.calculator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UncertaintyAccumulatorTest {

    private UncertaintyAccumulator accumulator;

    @Before
    public void setUp() {
        accumulator = new UncertaintyAccumulator();
    }

    @Test
    public void testAccumulateAndCombine_noSamples() {
        assertEquals(0, accumulator.getSampleCount());
        assertEquals(Double.NaN, accumulator.combine(), 1e-8);
    }

    @Test
    public void testAccumulateAndCombine_zeroSum() {
        accumulator.accumulateSample(0.0, 1.0);
        accumulator.accumulateSample(0.0, 1.0);

        assertEquals(2, accumulator.getSampleCount());
        assertEquals(0.0, accumulator.combine(), 1e-8);
    }

    @Test
    public void testAccumulateAndCombine() {
        accumulator.accumulateSample(1.0, 1.0);
        accumulator.accumulateSample(2.0, 1.0);
        accumulator.accumulateSample(3.0, 1.0);

        assertEquals(3, accumulator.getSampleCount());
        assertEquals(3.7416573867739413, accumulator.combine(), 1e-8);
    }

    @Test
    public void testAccumulateAndCombine_exceptionOnIllegalWeight() {
        try {
            accumulator.accumulateSample(1.0, 11.98);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

    }

}
