package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.data.Sensor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GenerateInsituPointsWorkflowTest {

    @Test
    public void testGenerateSensor() {
        final Sensor sensor = GenerateInsituPointsWorkflow.createSensor("Sarah", 8877646L);
        assertNotNull(sensor);
        assertEquals("Sarah", sensor.getName());
        assertEquals(8877646L, sensor.getPattern());
        assertEquals("InsituObservation", sensor.getObservationType());
    }
}
