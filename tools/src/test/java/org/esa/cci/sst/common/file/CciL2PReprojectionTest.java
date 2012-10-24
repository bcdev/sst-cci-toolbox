package org.esa.cci.sst.common.file;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * {@author Bettina Scholze}
 * Date: 18.10.12 09:29
 */
public class CciL2PReprojectionTest {

    private CciL2PReprojection cciL2PReprojection;
    private static final String FORMAT = "NetCDF-CF";

    @Before
    public void setUp() throws Exception {
        cciL2PReprojection = new CciL2PReprojection();
    }

//    public void manuallyDoReprojectionStep() throws Exception {
//        cciL2PReprojection.initialiseArray();
//        cciL2PReprojection.setVariable("sea_surface_temperature");
//        URL resource = CciL2PReprojectionTest.class.getResource("/org/esa/cci/sst/util/20100701000000-ESACCI-L2P_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc");
//        Product product = ProductIO.readProduct(new File(resource.toURI()), FORMAT);
//
//        cciL2PReprojection.doReprojectionStep(0, product);
//        System.out.println("\ncciL2PReprojection.getArray().getSize() = " + cciL2PReprojection.getArray().getSize());
//        System.out.println("cciL2PReprojection.getArray() = " + cciL2PReprojection.getArray());
//    }

//    public void manuallyDoReprojection() throws Exception {
//
//        String file = "C:\\Users\\bettina\\Development\\test-data\\sst-cci\\l2p\\20100701000000-ESACCI-L2P_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc";
//        cciL2PReprojection.doReprojection(new File(file), FORMAT, "sea_surface_temperature");
//    }

    @Test
    public void testCalculateMaxSteps() throws Exception {
        assertEquals(3, cciL2PReprojection.calculateMaxSteps(2345));
        assertEquals(28, cciL2PReprojection.calculateMaxSteps(27854));
        assertEquals(2323, cciL2PReprojection.calculateMaxSteps(2322221));
    }

    @Test
    public void testCreateBoundingBoxInPixels() throws Exception {
        double lonMax = 1.0;
        double shiftedLon = -60.0; //real value: -59.83233
        double latMin = 55.0;
        double shiftedLat = 63.0; //real value: 62.723232

        CciL2PReprojection.DoublePoint origin = cciL2PReprojection.new DoublePoint(shiftedLon, shiftedLat);
        CciL2PReprojection.IntPoint bbInPixels = cciL2PReprojection.createBoundingBoxInPixels(origin, lonMax, latMin, 0.5);

        assertEquals("61°, 0.5 resolution -> 61/0.5=122", 122, bbInPixels.getWidth());
        assertEquals("8°, 0.5 resolution -> 8/0.5=16", 16, bbInPixels.getHeight());
    }

    @Test
    public void testShiftLeft() throws Exception {
        String description = "Expect smaller values in degree and a multiple of 0.5";
        double[] values = {-3.532, -1.2, 7.3, 5.0, -5.111, 5.51, 11.3, -8.5};
        double[] expectedLeftShifted = {-4, -1.5, 7.0, 5.0, -5.5, 5.5, 11.0, -8.5};

        for (int i = 0; i < values.length; i++) {
            double result = cciL2PReprojection.shiftLeft(values[i], 0.5);
            assertEquals(description, expectedLeftShifted[i], result);
        }
    }

    @Test
    public void testShiftUp() throws Exception {
        String description = "Expect higher values in degree and a multiple of 0.5";
        double[] values = {-3.532, -1.2, 7.3, 5.0, -5.111, 5.51, 11.3, -8.5, 62.08848};
        double[] expectedUpShifted = {-3.5, -1.0, 7.5, 5.0, -5.0, 6.0, 11.5, -8.5, 62.5};

        for (int i = 0; i < values.length; i++) {
            double result = cciL2PReprojection.shiftUp(values[i], 0.5);
            assertEquals(description, expectedUpShifted[i], result);
        }
    }

    @Test
    public void testGetArray() throws Exception {
        assertNull(cciL2PReprojection.getArray());
    }

    @Test
    public void testInitialiseArray() throws Exception {
        Array array = cciL2PReprojection.initialiseArray();
        assertEquals("[3600, 7200]", Arrays.toString(array.getShape()));
        assertEquals(2, array.getRank());
        assertEquals(7200 * 3600, array.getSize());
    }
}
