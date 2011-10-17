package org.esa.cci.sst.regavg.util;

import org.esa.cci.sst.util.Cell;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class CellTest {

    @Test
    public void testInitState() throws Exception {
        Cell cell = new Cell();
        assertEquals(Double.NaN, cell.getSampleMean(), 1e-10);
        assertEquals(Double.NaN, cell.getSampleSigma(), 1e-10);
        assertEquals(0L, cell.getSampleCount());
    }

    @Test
    public void testAccumulateSample() throws Exception {
        Cell cell = new Cell();
        cell.accumulateSample(0.5, 1.0);
        cell.accumulateSample(0.7, 1.0);
        cell.accumulateSample(Double.NaN, 1.0); // ignored
        cell.accumulateSample(0.1, 1.0);
        cell.accumulateSample(0.3, 1.0);
        assertEquals(0.4, cell.getSampleMean(), 1e-10);
        assertEquals(0.22360679775, cell.getSampleSigma(), 1e-10);
        assertEquals(4L, cell.getSampleCount());
    }

    @Test
    public void testAccumulateSamples() throws Exception {

        Cell cell1 = new Cell();
        cell1.accumulateSample(0.5, 1.0);

        Cell cell2 = new Cell();
        cell2.accumulateSample(0.7, 1.0);
        cell2.accumulateSample(0.7, 1.0);
        cell2.accumulateSample(Double.NaN, 1.0); // ignored
        cell2.accumulateSample(0.7, 1.0);

        Cell cell3 = new Cell();
        cell3.accumulateSample(0.3, 1.0);
        cell3.accumulateSample(0.1, Double.NaN); // ignored

        Cell cell4 = new Cell(); // ignored

        Cell cell = new Cell();
        cell.accumulateCellAverage(cell1, 1.0);
        cell.accumulateCellAverage(cell2, 1.0);
        cell.accumulateCellAverage(cell3, 1.0);
        cell.accumulateCellAverage(cell4, 1.0);  // ignored

        assertEquals(0.5, cell.getSampleMean(), 1e-10);
        assertEquals(0.1632993161856, cell.getSampleSigma(), 1e-10);
        assertEquals(3L, cell.getSampleCount());
    }
}
