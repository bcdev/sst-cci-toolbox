package org.esa.cci.sst.accumulate;

import org.junit.Before;
import org.junit.Test;

import static java.lang.Double.NaN;
import static java.lang.Math.sqrt;
import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class WeightedUncertaintyAccumulatorTest {

    private WeightedUncertaintyAccumulator accumulator;

    @Before
    public void setUp() {
        accumulator = new WeightedUncertaintyAccumulator();
    }

    @Test
    public void testInitState() throws Exception {
        assertEquals(NaN, accumulator.combine(), 1e-10);
        assertEquals(0L, accumulator.getSampleCount());
    }

    @Test
    public void testAccumulateUnweighted() throws Exception {
        accumulator.accumulate(0.5, 1.0);
        accumulator.accumulate(0.7, 1.0);
        accumulator.accumulate(NaN, 1.0);  // ignored
        accumulator.accumulate(0.8, NaN);  // ignored
        accumulator.accumulate(0.1, 1.0);
        accumulator.accumulate(0.3, 1.0);
        assertEquals(4L, accumulator.getSampleCount());
        assertEquals(sqrt(1.0 / sqr(4) * (
                sqr(0.5)
                        + sqr(0.7)
                        + sqr(0.1)
                        + sqr(0.3))), accumulator.combine(), 1e-10);
    }

    @Test
    public void testAccumulateWeighted() throws Exception {
        accumulator.accumulate(0.5, 0.1);
        accumulator.accumulate(0.7, 0.6);
        accumulator.accumulate(NaN, 0.1);  // ignored
        accumulator.accumulate(0.8, NaN);  // ignored
        accumulator.accumulate(0.1, 0.2);
        accumulator.accumulate(0.3, 0.1);
        assertEquals(4L, accumulator.getSampleCount());
        assertEquals(sqrt(
                sqr(0.5 * 0.1)
                        + sqr(0.7 * 0.6)
                        + sqr(0.1 * 0.2)
                        + sqr(0.3 * 0.1)), accumulator.combine(), 1e-10);
    }

    @Test
    public void testAccumulateAndCombine_sumIsZero() {
        accumulator.accumulate(0.0);
        accumulator.accumulate(0.0);

        assertEquals(2, accumulator.getSampleCount());
        assertEquals(0.0, accumulator.combine(), 1e-8);
    }

    @Test
    public void testAcumulateAndCombine_sumOfWeightsIsZero() {
        accumulator.accumulate(2.0, 1.0);
        accumulator.accumulate(3.0, -1.0);

        assertEquals(2, accumulator.getSampleCount());
        assertEquals(Double.NaN, accumulator.combine(), 1e-8);
    }

    static double sqr(double x) {
        return x * x;
    }
}
