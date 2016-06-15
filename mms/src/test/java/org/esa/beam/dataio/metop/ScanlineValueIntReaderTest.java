package org.esa.beam.dataio.metop;

import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ScanlineValueIntReaderTest {

    private ScanlineValueIntReader reader;

    @Before
    public void setUp(){

        final ScanlineBandDescription description = new ScanlineBandDescription("quality_indicator_flags",
                "Quality indicator bit field",
                ProductData.TYPE_INT32,
                45);
        reader =  new ScanlineValueIntReader(null, null, description);
        // we do not access the MetopFile or the inputStream in this test - set to null tb 2016-06-15
    }

    @Test
    public void testGetBandName() {
        assertEquals("quality_indicator_flags", reader.getBandName());
    }

    @Test
    public void testGetBandUnit() {
        assertNull(reader.getBandUnit());
    }

    @Test
    public void testGetBandDescription() {
         assertEquals("Quality indicator bit field", reader.getBandDescription());
    }

    @Test
    public void testGetScalingFactor() {
         assertEquals(1.0, reader.getScalingFactor(), 1e-8);
    }

    @Test
    public void testGetDataType(){
         assertEquals(ProductData.TYPE_INT32, reader.getDataType());
    }
}
