package org.esa.cci.sst.common.calculator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArithmeticMeanAccumulatorTest {

    @Test
    public void testAccumulateAndCombine_noSamples()  {
        final ArithmeticMeanAccumulator accumulator= new ArithmeticMeanAccumulator();

        assertEquals(0, accumulator.getSampleCount());
        assertEquals(Double.NaN, accumulator.combine(), 1e-8);
    }

    @Test
    public void testAccumulateAndCombine_sumX_zero()  {
        final ArithmeticMeanAccumulator accumulator= new ArithmeticMeanAccumulator();

        accumulator.accumulateSample(0.0, 1.0);
        assertEquals(1, accumulator.getSampleCount());
        assertEquals(0.0, accumulator.combine(), 1e-8);
    }

    @Test
    public void testAccumulateAndCombine_sumWeights_zero()  {
        final ArithmeticMeanAccumulator accumulator= new ArithmeticMeanAccumulator();

        accumulator.accumulateSample(1.0, 1.0);
        accumulator.accumulateSample(2.0, -1.0);
        assertEquals(2, accumulator.getSampleCount());
        assertEquals(Double.NaN, accumulator.combine(), 1e-8);
    }

    @Test
    public void testAccumulateAndCombine()  {
        final ArithmeticMeanAccumulator accumulator= new ArithmeticMeanAccumulator();

        accumulator.accumulateSample(1.0, 0.0);
        accumulator.accumulateSample(2.0, 0.5);
        assertEquals(2, accumulator.getSampleCount());
        assertEquals(2.0, accumulator.combine(), 1e-8);
    }
}
