package org.esa.cci.sst.tools;

import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.tools.samplepoint.ObservationFinder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuxDataToolTest {

    @Test
    public void testCreateQueryParameter() {
        final long time = 667734683;
        final int delta = 289;
        final String sensorName = "the_sensor";

        final ObservationFinder.Parameter parameter = AuxDataTool.createQueryParameter(time, delta, sensorName);
        assertNotNull(parameter);
        assertEquals(time, parameter.getStartTime());
        assertEquals(time, parameter.getStopTime());
        assertEquals(delta, parameter.getSearchTimeFuture());
        assertEquals(delta, parameter.getSearchTimePast());
        assertEquals(sensorName, parameter.getSensorName());
    }
}
