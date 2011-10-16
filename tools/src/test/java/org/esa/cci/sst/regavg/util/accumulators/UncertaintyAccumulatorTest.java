package org.esa.cci.sst.regavg.util.accumulators;

import org.esa.cci.sst.util.accumulators.RandomUncertaintyAccumulator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class UncertaintyAccumulatorTest {

    @Test
    public void testInitState() throws Exception {
        RandomUncertaintyAccumulator cell = new RandomUncertaintyAccumulator();
        assertEquals(Double.NaN, cell.computeAverage(), 1e-10);
        assertEquals(0L, cell.getSampleCount());
        assertEquals(0L, cell.getTotalCount());
    }

    @Test
    public void testAccumulateSample() throws Exception {
        RandomUncertaintyAccumulator cell = new RandomUncertaintyAccumulator();
        cell.accumulateSample(0.5, 1.0, 1L);
        cell.accumulateSample(0.7, 1.0, 3L);
        cell.accumulateSample(Double.NaN, 1.0, 1L);
        cell.accumulateSample(0.1, 1.0, 0L); // ignored
        cell.accumulateSample(0.3, 1.0, 1L);
        assertEquals(0.5259911279353167, cell.computeAverage(), 1e-10);
        assertEquals(3L, cell.getSampleCount());
        assertEquals(5L, cell.getTotalCount());
    }

    @Test
    public void testAccumulateCell() throws Exception {

        RandomUncertaintyAccumulator cell1 = new RandomUncertaintyAccumulator();
        cell1.accumulateSample(0.5, 1.0, 1L);

        RandomUncertaintyAccumulator cell2 = new RandomUncertaintyAccumulator();
        cell2.accumulateSample(0.7, 1.0, 1L);
        cell2.accumulateSample(0.7, 1.0, 1L);
        cell2.accumulateSample(Double.NaN, 1.0, 1L);
        cell2.accumulateSample(0.7, 1.0, 1L);

        RandomUncertaintyAccumulator cell3 = new RandomUncertaintyAccumulator();
        cell3.accumulateSample(0.3, 1.0, 1L);
        cell3.accumulateSample(0.1, 1.0, 0L); // ignored

        RandomUncertaintyAccumulator cell4 = new RandomUncertaintyAccumulator();   // ignored

        RandomUncertaintyAccumulator cell = new RandomUncertaintyAccumulator();
        cell.accumulate(cell1);
        cell.accumulate(cell2);
        cell.accumulate(cell3);
        cell.accumulate(cell4);

        assertEquals(0.5259911279353167, cell.computeAverage(), 1e-10);
        assertEquals(3L, cell.getSampleCount());
        assertEquals(5L, cell.getTotalCount());
    }

}
