package org.esa.cci.sst.util;

/**
 * @author Norman
 */
public class CellTest {
    /*
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
        cell.accumulate(0.5, 1.0);
        cell.accumulate(0.7, 1.0);
        cell.accumulate(Double.NaN, 1.0); // ignored
        cell.accumulate(0.1, 1.0);
        cell.accumulate(0.3, 1.0);
        assertEquals(0.4, cell.getSampleMean(), 1e-10);
        assertEquals(0.22360679775, cell.getSampleSigma(), 1e-10);
        assertEquals(4L, cell.getSampleCount());
    }

    @Test
    public void testAccumulateSamples() throws Exception {

        Cell cell1 = new Cell();
        cell1.accumulate(0.5, 1.0);

        Cell cell2 = new Cell();
        cell2.accumulate(0.7, 1.0);
        cell2.accumulate(0.7, 1.0);
        cell2.accumulate(Double.NaN, 1.0); // ignored
        cell2.accumulate(0.7, 1.0);

        Cell cell3 = new Cell();
        cell3.accumulate(0.3, 1.0);
        cell3.accumulate(0.1, Double.NaN); // ignored

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

    */
}
