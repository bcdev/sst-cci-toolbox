package org.esa.cci.sst.util.calculators;

import org.esa.cci.sst.common.calculators.RandomUncertaintyAccumulator;
import org.junit.Test;

import static java.lang.Double.NaN;
import static java.lang.Math.sqrt;
import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class RandomUncertaintyAccumulatorTest {

    @Test
    public void testInitState() throws Exception {
        RandomUncertaintyAccumulator acc = new RandomUncertaintyAccumulator();
        assertEquals(NaN, acc.combine(), 1e-10);
        assertEquals(0L, acc.getSampleCount());
    }

    @Test
    public void testAccumulateUnweighted() throws Exception {
        RandomUncertaintyAccumulator acc = new RandomUncertaintyAccumulator();
        acc.accumulate(0.5, 1.0);
        acc.accumulate(0.7, 1.0);
        acc.accumulate(NaN, 1.0);  // ignored
        acc.accumulate(0.8, NaN);  // ignored
        acc.accumulate(0.1, 1.0);
        acc.accumulate(0.3, 1.0);
        assertEquals(4L, acc.getSampleCount());
        assertEquals(sqrt(1.0 / sqr(4) * (
                sqr(0.5)
                        + sqr(0.7)
                        + sqr(0.1)
                        + sqr(0.3))), acc.combine(), 1e-10);
    }

    @Test
    public void testAccumulateWeighted() throws Exception {
        RandomUncertaintyAccumulator acc = new RandomUncertaintyAccumulator();
        acc.accumulate(0.5, 0.1);
        acc.accumulate(0.7, 0.6);
        acc.accumulate(NaN, 0.1);  // ignored
        acc.accumulate(0.8, NaN);  // ignored
        acc.accumulate(0.1, 0.2);
        acc.accumulate(0.3, 0.1);
        assertEquals(4L, acc.getSampleCount());
        assertEquals(sqrt(
                sqr(0.5 * 0.1)
                        + sqr(0.7 * 0.6)
                        + sqr(0.1 * 0.2)
                        + sqr(0.3 * 0.1)), acc.combine(), 1e-10);
    }

    static double sqr(double x) {
        return x * x;
    }
}
