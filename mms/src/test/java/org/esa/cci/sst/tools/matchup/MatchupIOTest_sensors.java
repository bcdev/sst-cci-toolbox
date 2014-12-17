package org.esa.cci.sst.tools.matchup;


import org.esa.cci.sst.data.Sensor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("deprecation")
public class MatchupIOTest_sensors {

    private IdGenerator idGenerator;
    private MatchupData matchupData;

    @Before
    public void setUp(){
        idGenerator = new IdGenerator(2, 3, 4, 5);
        idGenerator.next(); // to avoid having 0 as first id
        matchupData = new MatchupData();
    }

    @Test
    public void testAddSameSensor_existingSensorReferenceIsUsed() {
        final Sensor sensor = createSensor();

        MatchupIO.addSensor(sensor, matchupData);

        final int id = MatchupIO.addSensor(sensor, matchupData);
        assertEquals(1, id);
        assertEquals(1, matchupData.getSensors().size());
    }

    @Test
    public void testAddSensor_nameIsSignificantToDifferentiate() {
        Sensor sensor = createSensor();

        int id = MatchupIO.addSensor(sensor, matchupData);
        assertEquals(1, id);
        assertEquals(1, matchupData.getSensors().size());

        sensor = createSensor();
        sensor.setName("different");
        id = MatchupIO.addSensor(sensor, matchupData);
        assertEquals(1, id);
        assertEquals(2, matchupData.getSensors().size());
    }

    @Test
    public void testAddSensor_patternIsSignificantToDifferentiate() {
        Sensor sensor = createSensor();

        int id = MatchupIO.addSensor(sensor, matchupData);
        assertEquals(1, id);
        assertEquals(1, matchupData.getSensors().size());

        sensor = createSensor();
        sensor.setPattern(-99);
        id = MatchupIO.addSensor(sensor, matchupData);
        assertEquals(1, id);
        assertEquals(2, matchupData.getSensors().size());
    }

    @Test
    public void testAddSensor_observationTypeIsSignificantToDifferentiate() {
        Sensor sensor = createSensor();

        int id = MatchupIO.addSensor(sensor, matchupData);
        assertEquals(1, id);
        assertEquals(1, matchupData.getSensors().size());

        sensor = createSensor();
        sensor.setObservationType("different");
        id = MatchupIO.addSensor(sensor, matchupData);
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
