package org.esa.cci.sst.util.calculators;

import org.esa.cci.sst.common.calculators.ArithmeticMeanAccumulator;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class MeanAccumulatorTest {

    @Test
    public void testInitState() throws Exception {
        ArithmeticMeanAccumulator cell = new ArithmeticMeanAccumulator();
        assertEquals(NaN, cell.combine(), 1e-10);
        assertEquals(0L, cell.getSampleCount());
    }

    @Test
    public void testAccumulateSample() throws Exception {
        ArithmeticMeanAccumulator cell = new ArithmeticMeanAccumulator();
        cell.accumulate(0.5, 1.0);
        cell.accumulate(0.7, 1.0);
        cell.accumulate(NaN, 1.0); // rejected
        cell.accumulate(0.8, NaN); // rejected
        cell.accumulate(0.1, 1.0);
        cell.accumulate(0.3, 1.0);
        assertEquals(4L, cell.getSampleCount());
        assertEquals(0.4, cell.combine(), 1e-10);
    }

}
