package org.esa.cci.sst.regavg.util.accumulators;

import org.esa.cci.sst.util.accumulators.RandomUncertaintyAccumulator;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class UncertaintyAccumulatorTest {

    @Test
    public void testInitState() throws Exception {
        RandomUncertaintyAccumulator cell = new RandomUncertaintyAccumulator();
        assertEquals(NaN, cell.computeAverage(), 1e-10);
        assertEquals(0L, cell.getSampleCount());
    }

    @Test
    public void testAccumulateSample() throws Exception {
        RandomUncertaintyAccumulator cell = new RandomUncertaintyAccumulator();
        cell.accumulate(0.5, 1.0);
        cell.accumulate(0.7, 1.0);
        cell.accumulate(NaN, 1.0);  // ignored
        cell.accumulate(0.8, NaN);  // ignored
        cell.accumulate(0.1, 1.0);
        cell.accumulate(0.3, 1.0);
        assertEquals(4L, cell.getSampleCount());
        assertEquals(0.458257569495584, cell.computeAverage(), 1e-10);
    }

    @Test
    public void testAccumulateCell() throws Exception {

        RandomUncertaintyAccumulator cell1 = new RandomUncertaintyAccumulator();
        cell1.accumulate(0.5, 1.0);

        RandomUncertaintyAccumulator cell2 = new RandomUncertaintyAccumulator();
        cell2.accumulate(0.7, 1.0);
        cell2.accumulate(0.7, 1.0);
        cell2.accumulate(NaN, 1.0);
        cell2.accumulate(0.7, 1.0);

        RandomUncertaintyAccumulator cell3 = new RandomUncertaintyAccumulator();
        cell3.accumulate(0.3, 1.0);
        cell3.accumulate(0.1, 1.0); // ignored

        RandomUncertaintyAccumulator cell4 = new RandomUncertaintyAccumulator();   // ignored

        RandomUncertaintyAccumulator cell = new RandomUncertaintyAccumulator();
        cell.accumulateAverage(cell1,1);
        cell.accumulateAverage(cell2,1);
        cell.accumulateAverage(cell3,1);
        cell.accumulateAverage(cell4,1);

        assertEquals(3L, cell.getSampleCount());
        assertEquals(0.5131601439446885, cell.computeAverage(), 1e-10);
    }

}
