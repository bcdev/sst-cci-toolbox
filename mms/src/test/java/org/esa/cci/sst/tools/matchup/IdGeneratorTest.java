package org.esa.cci.sst.tools.matchup;

import org.esa.cci.sst.tool.Configuration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IdGeneratorTest {

    @Test
    public void testNext() {
        final IdGenerator idGenerator = new IdGenerator(0, 0, 0, 0);

        assertEquals(0, idGenerator.next());
        assertEquals(1, idGenerator.next());
        assertEquals(2, idGenerator.next());
    }

    @Test
    public void testNextUnique() {
        final IdGenerator idGenerator = new IdGenerator(1998, 11, 6, 14);

        assertEquals(1998110614000000000L, idGenerator.nextUnique());
        assertEquals(1998110614000000001L, idGenerator.nextUnique());
        assertEquals(1998110614000000002L, idGenerator.nextUnique());
    }

    @Test
    public void testCreateFromConfig_oneSensor() {
        final Configuration configuration = new Configuration();
        configuration.put(Configuration.KEY_MMS_SAMPLING_START_TIME, "2010-06-01T00:00:00Z");
        configuration.put(Configuration.KEY_MMS_SAMPLING_STOP_TIME, "2010-07-01T00:00:00Z");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SENSOR, "orb_atsr.3");

        final IdGenerator idGenerator = IdGenerator.create(configuration);

        assertEquals(2010060900000000000L, idGenerator.nextUnique());
        assertEquals(2010060900000000001L, idGenerator.nextUnique());
    }

    @Test
    public void testCreateFromConfig_twoSensor() {
        final Configuration configuration = new Configuration();
        configuration.put(Configuration.KEY_MMS_SAMPLING_START_TIME, "2009-07-01T00:00:00Z");
        configuration.put(Configuration.KEY_MMS_SAMPLING_STOP_TIME, "2009-08-01T00:00:00Z");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SENSOR, "avhrr.n11, history");

        final IdGenerator idGenerator = IdGenerator.create(configuration);

        assertEquals(2009074003000000000L, idGenerator.nextUnique());
        assertEquals(2009074003000000001L, idGenerator.nextUnique());
    }
}
