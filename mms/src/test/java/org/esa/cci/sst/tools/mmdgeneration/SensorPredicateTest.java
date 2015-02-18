package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.Predicate;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ralf Quast
 */
public class SensorPredicateTest {

    @Test
    public void testSensorPredicates_withoutSensors() throws Exception {
        final Predicate predicate = new SensorPredicate(new String[]{});

        assertTrue(predicate.accept("matchup.id"));
        assertTrue(predicate.accept("aai.absorbing_aerosol_index"));
        assertTrue(predicate.accept("seaice.concentration"));
        assertTrue(predicate.accept("insitu.sea_surface_temperature"));

        assertTrue(predicate.accept("atsr.3.cloud_flag"));
        assertTrue(predicate.accept("atsr.2.cloud_flag"));
        assertTrue(predicate.accept("atsr.1.cloud_flag"));

        assertTrue(predicate.accept("avhrr.n12.cloud_flag"));
        assertTrue(predicate.accept("avhrr.n11.cloud_flag"));
    }

    @Test
    public void testSensorPredicates_withSensors() throws Exception {
        final Predicate predicate = new SensorPredicate(new String[]{"avhrr.n12", "avhrr.n11"});

        assertTrue(predicate.accept("matchup.id"));
        assertTrue(predicate.accept("aai.absorbing_aerosol_index"));
        assertTrue(predicate.accept("seaice.concentration"));
        assertTrue(predicate.accept("insitu.sea_surface_temperature"));

        assertFalse(predicate.accept("atsr.3.cloud_flag"));
        assertFalse(predicate.accept("atsr.2.cloud_flag"));
        assertFalse(predicate.accept("atsr.1.cloud_flag"));

        assertTrue(predicate.accept("avhrr.n12.cloud_flag"));
        assertTrue(predicate.accept("avhrr.n11.cloud_flag"));
    }
}
