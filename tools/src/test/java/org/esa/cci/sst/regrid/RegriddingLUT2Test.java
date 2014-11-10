package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.Grid;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RegriddingLUT2Test {

    @Test
    public void testReadGrid() throws Exception {
        final File file = getResourceAsFile("x0_time.txt");
        final Grid grid = RegriddingLUT2.readGrid(file, -32768.0);

        assertTrue(Double.isNaN(grid.getSampleDouble(0, 0)));
        assertEquals(4.29, grid.getSampleDouble(1, 5), 0.0);
        assertEquals(2.64, grid.getSampleDouble(0, 13), 0.0);
        assertEquals(2.66, grid.getSampleDouble(1, 13), 0.0);
        assertEquals(4.99, grid.getSampleDouble(179, 87), 0.0);
        assertEquals(1.00, grid.getSampleDouble(0, 88), 0.0);
        assertEquals(10.0, grid.getSampleDouble(0, 89), 0.0);
    }

    private File getResourceAsFile(String name) throws URISyntaxException {
        final URL url = getClass().getResource(name);
        final URI uri = url.toURI();
        return new File(uri);
    }
}
