package org.esa.cci.sst.regavg.util.accumulators;

import org.esa.cci.sst.util.accumulators.WeightedMeanAccumulator;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class MeanAccumulatorTest {

    @Test
    public void testInitState() throws Exception {
        WeightedMeanAccumulator cell = new WeightedMeanAccumulator();
        assertEquals(NaN, cell.computeAverage(), 1e-10);
        assertEquals(0L, cell.getSampleCount());
    }

    @Test
    public void testAccumulateSample() throws Exception {
        WeightedMeanAccumulator cell = new WeightedMeanAccumulator();
        cell.accumulate(0.5, 1.0);
        cell.accumulate(0.7, 1.0);
        cell.accumulate(NaN, 1.0); // rejected
        cell.accumulate(0.8, NaN); // rejected
        cell.accumulate(0.1, 1.0);
        cell.accumulate(0.3, 1.0);
        assertEquals(4L, cell.getSampleCount());
        assertEquals(0.4, cell.computeAverage(), 1e-10);
    }

    @Test
    public void testAccumulateCell() throws Exception {

        WeightedMeanAccumulator cell1 = new WeightedMeanAccumulator();
        cell1.accumulate(0.5, 1.0);

        WeightedMeanAccumulator cell2 = new WeightedMeanAccumulator();
        cell2.accumulate(0.7, 1.0);
        cell2.accumulate(0.7, 1.0);
        cell2.accumulate(NaN, 1.0);
        cell2.accumulate(0.7, 1.0);

        WeightedMeanAccumulator cell3 = new WeightedMeanAccumulator();
        cell3.accumulate(0.3, 1.0);
        cell3.accumulate(0.1, 1.0); // ignored

        WeightedMeanAccumulator cell4 = new WeightedMeanAccumulator();   // ignored

        WeightedMeanAccumulator cell = new WeightedMeanAccumulator();
        cell.accumulateAverage(cell1,1);
        cell.accumulateAverage(cell2,1);
        cell.accumulateAverage(cell3,1);
        cell.accumulateAverage(cell4,1);

        assertEquals(3L, cell.getSampleCount());
        assertEquals(0.466666666666666, cell.computeAverage(), 1e-10);
    }

}
