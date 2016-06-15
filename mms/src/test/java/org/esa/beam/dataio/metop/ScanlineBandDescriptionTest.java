package org.esa.beam.dataio.metop;

import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScanlineBandDescriptionTest {

    @Test
    public void testConstruction() {
        final ScanlineBandDescription description = new ScanlineBandDescription("name", "description", ProductData.TYPE_INT8, 12);
        assertEquals("name", description.getName());
        assertEquals("description", description.getDescription());
        assertEquals(ProductData.TYPE_INT8, description.getDataType());
        assertEquals(12, description.getLineOffset());
    }
}
