package org.esa.cci.sst.tools;

import org.junit.Test;

import static org.junit.Assert.*;

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

    @Test
    public void testVariance_withNaN() throws Exception {
        final double[] data = {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};

        assertEquals(0.0, SelectionTool.variance(data), 0.0);
    }

    @Test
    public void testAcceptZenithAngles() {
        final double[] vza1 = new double[12];
        final double[] vza2 = new double[14];

        vza1[6] = 13.0;
        vza2[7] = 12.0;
        assertTrue(SelectionTool.acceptZenithAngles(vza1, vza2));

        vza1[6] = -19.0;
        vza2[7] = -21.0;
        assertTrue(SelectionTool.acceptZenithAngles(vza1, vza2));

        vza1[6] = -19.0;
        vza2[7] = 0.0;
        assertFalse(SelectionTool.acceptZenithAngles(vza1, vza2));

        vza1[6] = 2.1;
        vza2[7] = 108.0;
        assertFalse(SelectionTool.acceptZenithAngles(vza1, vza2));
    }
}
