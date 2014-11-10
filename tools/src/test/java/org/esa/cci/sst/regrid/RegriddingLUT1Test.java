package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.LUT;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.GridDef;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public class RegriddingLUT1Test {

    // ocean pixel coordinates
    private static final int OCEAN_X = 3547;
    private static final int OCEAN_Y = 1556;

    // land pixel coordinates (Australia)
    private static final int LAND_X = 6373;
    private static final int LAND_Y = 1338;

    @Test
    public void testReadGrid() throws Exception {
        final File file = getResourceAsFile("20070321-UKMO-L4HRfnd-GLOB-v01-fv02-OSTIARANanom_stdev.nc");

        final Grid grid = RegriddingLUT1.readGrid(file);

        assertNotNull(grid);

        assertEquals(3600, grid.getGridDef().getHeight());
        assertEquals(7200, grid.getGridDef().getWidth());

        assertEquals(39, grid.getSampleInt(OCEAN_X, OCEAN_Y));
        assertEquals(39.0 * 0.01, grid.getSampleDouble(OCEAN_X, OCEAN_Y), 1.0e-7);

        assertEquals(0, grid.getSampleInt(LAND_X, LAND_Y));
        assertTrue(Double.isNaN(grid.getSampleDouble(LAND_X, LAND_Y)));
    }

    @Test
    public void testLutGridIsFlipped() throws Exception {
        final File file = getResourceAsFile("20070321-UKMO-L4HRfnd-GLOB-v01-fv02-OSTIARANanom_stdev.nc");

        final LUT lut = RegriddingLUT1.create(file, GridDef.createGlobal(0.05));

        assertNotNull(lut);

        final Grid grid = lut.getGrid();

        assertNotNull(grid);

        assertEquals(3600, grid.getGridDef().getHeight());
        assertEquals(7200, grid.getGridDef().getWidth());

        int flippedY;

        flippedY = 3600 - 1 - OCEAN_Y;
        assertEquals(39, grid.getSampleInt(OCEAN_X, flippedY));
        assertEquals(39.0 * 0.01, grid.getSampleDouble(OCEAN_X, flippedY), 1.0e-7);

        flippedY = 3600 - 1 - LAND_Y;
        assertEquals(0, grid.getSampleInt(LAND_X, flippedY));
        assertTrue(Double.isNaN(grid.getSampleDouble(LAND_X, flippedY)));
    }

    private File getResourceAsFile(String name) throws URISyntaxException {
        final URL url = getClass().getResource(name);
        final URI uri = url.toURI();
        return new File(uri);
    }
}
