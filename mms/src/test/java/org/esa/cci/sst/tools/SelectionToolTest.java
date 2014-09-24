package org.esa.cci.sst.tools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ralf Quast
 */
public class SelectionToolTest {

    @Test
    public void testVariance_withZero() throws Exception {
        final double[] data = {0.0, 0.0, 0.0, 0.0, 0.0};

        assertEquals(0.0, SelectionTool.variance(data), 0.0);
    }

    @Test
    public void testVariance_withUnity() throws Exception {
        final double[] data = {1.0, 1.0, 1.0, 1.0, 1.0};

        assertEquals(0.0, SelectionTool.variance(data), 0.0);
    }

    @Test
    public void testVariance_withOtherNumbers() throws Exception {
        final double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};

        assertEquals(2.5, SelectionTool.variance(data), 0.0);
    }

    @Test
    public void testVariance_withMoreNumbers() throws Exception {
        final double[] data = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0};

        assertEquals(7.5, SelectionTool.variance(data), 0.0);
    }

}
