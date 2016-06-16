package org.esa.beam.dataio.metop;


import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ScanlineValueShortReaderTest {

    private ScanlineValueShortReader reader;

    @Before
    public void setUp(){
        final ScanlineBandDescription description = new ScanlineBandDescription("calibration_quality_ch3_flags",
                "Calibration Quality Flags for ch3",
                ProductData.TYPE_INT16,
                29);
        reader =  new ScanlineValueShortReader(null, null, description);
        // we do not access the MetopFile or the inputStream in this test - set to null tb 2016-06-15
    }

    @Test
    public void testGetBandName() {
        assertEquals("calibration_quality_ch3_flags", reader.getBandName());
    }

    @Test
    public void testGetBandUnit() {
        assertNull(reader.getBandUnit());
    }

    @Test
    public void testGetBandDescription() {
        assertEquals("Calibration Quality Flags for ch3", reader.getBandDescription());
    }

    @Test
    public void testGetScalingFactor() {
        assertEquals(1.0, reader.getScalingFactor(), 1e-8);
    }

    @Test
    public void testGetDataType(){
        assertEquals(ProductData.TYPE_INT16, reader.getDataType());
    }
}
