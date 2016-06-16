package org.esa.beam.dataio.metop;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MetopReaderTest {

    @Test
    public void testComputeAda() {
        assertEquals(-55.6, MetopReader.computeAda(123.5, 67.9), 1e-8);
        assertEquals(-10.1, MetopReader.computeAda(-172.5, 177.4), 1e-8);
        assertEquals(7.7, MetopReader.computeAda(175.2, -177.1), 1e-8);
    }
}
