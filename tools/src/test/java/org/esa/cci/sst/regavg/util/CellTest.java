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
        assertEquals(0L, cell.getAccuCount());
        assertEquals(0L, cell.getTotalCount());
    }

    @Test
    public void testAccumulateSample() throws Exception {
        Cell cell = new Cell();
        cell.accumulate(0.5, 1.0, 1L);
        cell.accumulate(0.7, 1.0, 3L);
        cell.accumulate(Double.NaN, 1.0, 1L);
        cell.accumulate(0.1, 1.0, 0L); // ignored
        cell.accumulate(0.3, 1.0, 1L);
        assertEquals(0.5, cell.getSampleMean(), 1e-10);
        assertEquals(0.1632993161856, cell.getSampleSigma(), 1e-10);
        assertEquals(3L, cell.getAccuCount());
        assertEquals(5L, cell.getTotalCount());
    }

    @Test
    public void testAccumulateCell() throws Exception {

        Cell cell1 = new Cell();
        cell1.accumulate(0.5, 1.0, 1L);

        Cell cell2 = new Cell();
        cell2.accumulate(0.7, 1.0, 1L);
        cell2.accumulate(0.7, 1.0, 1L);
        cell2.accumulate(Double.NaN, 1.0, 1L);
        cell2.accumulate(0.7, 1.0, 1L);

        Cell cell3 = new Cell();
        cell3.accumulate(0.3, 1.0, 1L);
        cell3.accumulate(0.1, 1.0, 0L); // ignored

        Cell cell4 = new Cell();   // ignored

        Cell cell = new Cell();
        cell.accumulate(cell1);
        cell.accumulate(cell2);
        cell.accumulate(cell3);
        cell.accumulate(cell4);

        assertEquals(0.5, cell.getSampleMean(), 1e-10);
        assertEquals(0.1632993161856, cell.getSampleSigma(), 1e-10);
        assertEquals(3L, cell.getAccuCount());
        assertEquals(5L, cell.getTotalCount());
    }
}
