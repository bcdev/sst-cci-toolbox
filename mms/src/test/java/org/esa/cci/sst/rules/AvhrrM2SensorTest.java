package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Sensor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AvhrrM2SensorTest {

    @Test
    public void testSensorName() throws RuleException {
        final ColumnBuilder columnBuilder = new ColumnBuilder();
        final AvhrrM2Sensor avhrrM2Sensor = new AvhrrM2Sensor();

        avhrrM2Sensor.configureTargetColumn(columnBuilder, null);

        final Item item = columnBuilder.build();
        final Sensor sensor = item.getSensor();
        assertNotNull(sensor);
        assertEquals("orb_avhrr.m02", sensor.getName());
    }
}
