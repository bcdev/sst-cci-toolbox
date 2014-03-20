package org.esa.cci.sst.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class InsituDatasetIdTest {

    @Test
    public void testValuesAndNames() {
        assertEquals("drifter", InsituDatasetId.drifter.name());
        assertEquals((byte) 0, InsituDatasetId.drifter.getValue());

        assertEquals("mooring", InsituDatasetId.mooring.name());
        assertEquals((byte) 1, InsituDatasetId.mooring.getValue());

        assertEquals("ship", InsituDatasetId.ship.name());
        assertEquals((byte) 2, InsituDatasetId.ship.getValue());

        assertEquals("gtmba", InsituDatasetId.gtmba.name());
        assertEquals((byte) 3, InsituDatasetId.gtmba.getValue());

        assertEquals("radiometer", InsituDatasetId.radiometer.name());
        assertEquals((byte) 4, InsituDatasetId.radiometer.getValue());

        assertEquals("argo", InsituDatasetId.argo.name());
        assertEquals((byte) 5, InsituDatasetId.argo.getValue());

        assertEquals("argo", InsituDatasetId.argo.name());
        assertEquals((byte) 5, InsituDatasetId.argo.getValue());

        assertEquals("dummy_sea_ice", InsituDatasetId.dummy_sea_ice.name());
        assertEquals((byte) 6, InsituDatasetId.dummy_sea_ice.getValue());

        assertEquals("dummy_diurnal_variability", InsituDatasetId.dummy_diurnal_variability.name());
        assertEquals((byte) 7, InsituDatasetId.dummy_diurnal_variability.getValue());

        assertEquals("dummy_bc", InsituDatasetId.dummy_bc.name());
        assertEquals((byte) 8, InsituDatasetId.dummy_bc.getValue());

        assertEquals("xbt", InsituDatasetId.xbt.name());
        assertEquals((byte) 9, InsituDatasetId.xbt.getValue());

        assertEquals("mbt", InsituDatasetId.mbt.name());
        assertEquals((byte) 10, InsituDatasetId.mbt.getValue());

        assertEquals("ctd", InsituDatasetId.ctd.name());
        assertEquals((byte) 11, InsituDatasetId.ctd.getValue());

        assertEquals("animal", InsituDatasetId.animal.name());
        assertEquals((byte) 12, InsituDatasetId.animal.getValue());

        assertEquals("bottle", InsituDatasetId.bottle.name());
        assertEquals((byte) 13, InsituDatasetId.bottle.getValue());


        //"drifter mooring ship gtmba radiometer argo dummy_sea_ice dummy_diurnal_variability dummy_bc xbt mbt ctd animal bottle"
    }

    @Test
    public void testCreate() {
        InsituDatasetId dsId = InsituDatasetId.create((byte) 9);
        assertEquals(InsituDatasetId.xbt, dsId);

        dsId = InsituDatasetId.create((byte) 4);
        assertEquals(InsituDatasetId.radiometer, dsId);
    }

    @Test
    public void testCreate_throwsOnIllegalInput() {
        try {
            InsituDatasetId.create((byte) 14);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Invalid InsituDatasetId: 14", expected.getMessage());
        }
    }

    @Test
    public void testGetNames() {
         assertEquals("drifter mooring ship gtmba radiometer argo dummy_sea_ice dummy_diurnal_variability dummy_bc xbt mbt ctd animal bottle",
                 InsituDatasetId.getNames());
    }

    @Test
    public void testGetValues() {
        final byte[] values = InsituDatasetId.getValues();
        assertEquals(14, values.length);
        assertEquals((byte) 1, values[1]);
        assertEquals((byte) 7, values[7]);
        assertEquals((byte) 11, values[11]);
    }
}
