package org.esa.beam.util;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.util.IoUtil;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IoUtilTest {

    @Test
    public void testCreateColumnBuilder() {
        final Variable variable = mock(Variable.class);
        when(variable.getDataType()).thenReturn(DataType.DOUBLE);
        when(variable.getDimensionsString()).thenReturn("long wide");
        when(variable.getRank()).thenReturn(2);
        when(variable.getShortName()).thenReturn("short_name");
        when(variable.isUnsigned()).thenReturn(true);
        when(variable.getUnitsString()).thenReturn("square inch per degree");

        final ColumnBuilder builder = IoUtil.createColumnBuilder(variable, "a_senso_r");
        assertNotNull(builder);

        final Item item = builder.build();
        assertNotNull(item);
        assertEquals("a_senso_r.short_name", item.getName());
        assertEquals(DataType.DOUBLE.name(), item.getType());
        assertTrue(item.isUnsigned());
        assertEquals(2, item.getRank());
        assertEquals("long wide", item.getDimensions());
        assertEquals("square inch per degree", item.getUnit());
        assertEquals("short_name", item.getRole());
    }
}
