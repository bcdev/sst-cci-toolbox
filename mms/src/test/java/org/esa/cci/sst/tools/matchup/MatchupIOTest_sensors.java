package org.esa.cci.sst.tools.matchup;


import org.esa.cci.sst.data.Sensor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@SuppressWarnings("deprecation")
public class MatchupIOTest_sensors {

    private MatchupData matchupData;
    private DetachHandler detachHandler;

    @Before
    public void setUp(){
        matchupData = new MatchupData();
        detachHandler = mock(DetachHandler.class);
    }

    @Test
    public void testAddSameSensor_existingSensorReferenceIsUsed() {
        final Sensor sensor = createSensor();

        MatchupIO.addSensor(sensor, matchupData, detachHandler);

        final int id = MatchupIO.addSensor(sensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(1, matchupData.getSensors().size());
    }

    @Test
    public void testAddSensor_nameIsSignificantToDifferentiate() {
        Sensor sensor = createSensor();

        int id = MatchupIO.addSensor(sensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(1, matchupData.getSensors().size());

        sensor = createSensor();
        sensor.setName("different");
        id = MatchupIO.addSensor(sensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(2, matchupData.getSensors().size());
    }

    @Test
    public void testAddSensor_patternIsSignificantToDifferentiate() {
        Sensor sensor = createSensor();

        int id = MatchupIO.addSensor(sensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(1, matchupData.getSensors().size());

        sensor = createSensor();
        sensor.setPattern(-99);
        id = MatchupIO.addSensor(sensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(2, matchupData.getSensors().size());
    }

    @Test
    public void testAddSensor_observationTypeIsSignificantToDifferentiate() {
        Sensor sensor = createSensor();

        int id = MatchupIO.addSensor(sensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(1, matchupData.getSensors().size());

        sensor = createSensor();
        sensor.setObservationType("different");
        id = MatchupIO.addSensor(sensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(2, matchupData.getSensors().size());
    }

    private Sensor createSensor() {
        final Sensor sensor = new Sensor();
        sensor.setName("bla");
        sensor.setPattern(9);
        sensor.setObservationType("blub");
        sensor.setId(1);
        return sensor;
    }
}
