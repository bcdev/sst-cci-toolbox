package org.esa.beam.dataio.metop;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MetopReaderTest {

    @Test
    public void testComputeAda() {
        assertEquals(-55.6, MetopReader.computeAda(123.5, 67.9), 1e-8);
        assertEquals(-10.1, MetopReader.computeAda(-172.5, 177.4), 1e-8);
        assertEquals(7.7, MetopReader.computeAda(175.2, -177.1), 1e-8);
    }

    @Test
    public void testIsChannel3a() {
        assertTrue(MetopReader.isChannel3a(1));
        assertTrue(MetopReader.isChannel3a(17));

        assertFalse(MetopReader.isChannel3a(2));
        assertFalse(MetopReader.isChannel3a(128));
    }
}
