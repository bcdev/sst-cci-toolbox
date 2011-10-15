package org.esa.cci.sst.regavg.util.accumulators;

import org.esa.cci.sst.util.accumulators.UncertaintyAccumulator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class UncertaintyAccumulatorTest {

    @Test
    public void testInitState() throws Exception {
        UncertaintyAccumulator cell = new UncertaintyAccumulator();
        assertEquals(Double.NaN, cell.computeAverage(), 1e-10);
        assertEquals(0L, cell.getAccuCount());
        assertEquals(0L, cell.getTotalCount());
    }

    @Test
    public void testAccumulateSample() throws Exception {
        UncertaintyAccumulator cell = new UncertaintyAccumulator();
        cell.accumulateSample(0.5, 1.0, 1L);
        cell.accumulateSample(0.7, 1.0, 3L);
        cell.accumulateSample(Double.NaN, 1.0, 1L);
        cell.accumulateSample(0.1, 1.0, 0L); // ignored
        cell.accumulateSample(0.3, 1.0, 1L);
        assertEquals(0.5259911279353167, cell.computeAverage(), 1e-10);
        assertEquals(3L, cell.getAccuCount());
        assertEquals(5L, cell.getTotalCount());
    }

    @Test
    public void testAccumulateCell() throws Exception {

        UncertaintyAccumulator cell1 = new UncertaintyAccumulator();
        cell1.accumulateSample(0.5, 1.0, 1L);

        UncertaintyAccumulator cell2 = new UncertaintyAccumulator();
        cell2.accumulateSample(0.7, 1.0, 1L);
        cell2.accumulateSample(0.7, 1.0, 1L);
        cell2.accumulateSample(Double.NaN, 1.0, 1L);
        cell2.accumulateSample(0.7, 1.0, 1L);

        UncertaintyAccumulator cell3 = new UncertaintyAccumulator();
        cell3.accumulateSample(0.3, 1.0, 1L);
        cell3.accumulateSample(0.1, 1.0, 0L); // ignored

        UncertaintyAccumulator cell4 = new UncertaintyAccumulator();   // ignored

        UncertaintyAccumulator cell = new UncertaintyAccumulator();
        cell.accumulate(cell1);
        cell.accumulate(cell2);
        cell.accumulate(cell3);
        cell.accumulate(cell4);

        assertEquals(0.5259911279353167, cell.computeAverage(), 1e-10);
        assertEquals(3L, cell.getAccuCount());
        assertEquals(5L, cell.getTotalCount());
    }

}
