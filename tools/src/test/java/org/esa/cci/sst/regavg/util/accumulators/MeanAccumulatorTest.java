package org.esa.cci.sst.regavg.util.accumulators;

import org.esa.cci.sst.util.accumulators.WeightedMeanAccumulator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class MeanAccumulatorTest {

    @Test
    public void testInitState() throws Exception {
        WeightedMeanAccumulator cell = new WeightedMeanAccumulator();
        assertEquals(Double.NaN, cell.computeAverage(), 1e-10);
        assertEquals(0L, cell.getSampleCount());
        assertEquals(0L, cell.getTotalCount());
    }

    @Test
    public void testAccumulateSample() throws Exception {
        WeightedMeanAccumulator cell = new WeightedMeanAccumulator();
        cell.accumulateSample(0.5, 1.0, 1L);
        cell.accumulateSample(0.7, 1.0, 3L);
        cell.accumulateSample(Double.NaN, 1.0, 1L);
        cell.accumulateSample(0.1, 1.0, 0L); // ignored
        cell.accumulateSample(0.3, 1.0, 1L);
        assertEquals(0.5, cell.computeAverage(), 1e-10);
        assertEquals(3L, cell.getSampleCount());
        assertEquals(5L, cell.getTotalCount());
    }

    @Test
    public void testAccumulateCell() throws Exception {

        WeightedMeanAccumulator cell1 = new WeightedMeanAccumulator();
        cell1.accumulateSample(0.5, 1.0, 1L);

        WeightedMeanAccumulator cell2 = new WeightedMeanAccumulator();
        cell2.accumulateSample(0.7, 1.0, 1L);
        cell2.accumulateSample(0.7, 1.0, 1L);
        cell2.accumulateSample(Double.NaN, 1.0, 1L);
        cell2.accumulateSample(0.7, 1.0, 1L);

        WeightedMeanAccumulator cell3 = new WeightedMeanAccumulator();
        cell3.accumulateSample(0.3, 1.0, 1L);
        cell3.accumulateSample(0.1, 1.0, 0L); // ignored

        WeightedMeanAccumulator cell4 = new WeightedMeanAccumulator();   // ignored

        WeightedMeanAccumulator cell = new WeightedMeanAccumulator();
        cell.accumulate(cell1);
        cell.accumulate(cell2);
        cell.accumulate(cell3);
        cell.accumulate(cell4);

        assertEquals(0.5, cell.computeAverage(), 1e-10);
        assertEquals(3L, cell.getSampleCount());
        assertEquals(5L, cell.getTotalCount());
    }

}
