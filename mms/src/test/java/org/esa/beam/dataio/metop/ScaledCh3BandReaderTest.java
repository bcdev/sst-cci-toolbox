package org.esa.beam.dataio.metop;


import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScaledCh3BandReaderTest {

    private ScaledCh3BandReader readerCh_3A;
    private ScaledCh3BandReader readerCh_3B;

    @Before
    public void setUp() {
        readerCh_3A = new ScaledCh3BandReader(AvhrrConstants.CH_3A, null, null, null);
        readerCh_3B = new ScaledCh3BandReader(AvhrrConstants.CH_3B, null, null, null);
    }

    @Test
    public void testGetBandName() {
        assertEquals("reflec_3a", readerCh_3A.getBandName());
        assertEquals("temp_3b", readerCh_3B.getBandName());
    }

    @Test
    public void testGetBandUnit() {
        assertEquals("%", readerCh_3A.getBandUnit());
        assertEquals("K", readerCh_3B.getBandUnit());
    }

    @Test
    public void testGetBandDescription() {
        assertEquals("Reflectance factor for channel 3a", readerCh_3A.getBandDescription());
        assertEquals("Blackbody temperature for channel 3b", readerCh_3B.getBandDescription());
    }

    @Test
    public void testGetScalingFactor() {
        assertEquals(1.0, readerCh_3A.getScalingFactor(), 1e-8);
        assertEquals(1.0, readerCh_3B.getScalingFactor(), 1e-8);
    }

    @Test
    public void testGetDataType() {
        assertEquals(ProductData.TYPE_FLOAT32, readerCh_3A.getDataType());
        assertEquals(ProductData.TYPE_FLOAT32, readerCh_3B.getDataType());
    }

    @Test
    public void testCreateFillValueLine() {
        final int ch3a_flag = 1;
        final int ch3b_flag = 0;

        assertTrue(readerCh_3A.createFillValueLine(ch3b_flag));
        assertFalse(readerCh_3A.createFillValueLine(ch3a_flag));

        assertTrue(readerCh_3B.createFillValueLine(ch3a_flag));
        assertFalse(readerCh_3B.createFillValueLine(ch3b_flag));
    }
}
