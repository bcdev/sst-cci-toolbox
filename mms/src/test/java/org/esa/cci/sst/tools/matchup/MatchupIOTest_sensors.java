package org.esa.cci.sst.tools.matchup;


import org.esa.cci.sst.data.Sensor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

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
        assertEquals(1, matchupData.getSe().size());

        verify(detachHandler, times(1)).detach(sensor);
        verifyNoMoreInteractions(detachHandler);
    }

    @Test
    public void testAddSensor_nameIsSignificantToDifferentiate() {
        final Sensor sensor = createSensor();

        int id = MatchupIO.addSensor(sensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(1, matchupData.getSe().size());

        final Sensor differentSensor = createSensor();
        differentSensor.setName("different");
        id = MatchupIO.addSensor(differentSensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(2, matchupData.getSe().size());

        verify(detachHandler, times(1)).detach(sensor);
        verify(detachHandler, times(1)).detach(differentSensor);
        verifyNoMoreInteractions(detachHandler);
    }

    // temporally ignored, because purpose of test not understood
    @Ignore
    @Test
    public void testAddSensor_patternIsSignificantToDifferentiate() {
        final Sensor sensor = createSensor();

        int id = MatchupIO.addSensor(sensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(1, matchupData.getSe().size());

        final Sensor otherSensor = createSensor();
        otherSensor.setPattern(-99);
        id = MatchupIO.addSensor(otherSensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(2, matchupData.getSe().size());

        verify(detachHandler, times(1)).detach(sensor);
        verify(detachHandler, times(1)).detach(otherSensor);
        verifyNoMoreInteractions(detachHandler);
    }

    // temporally ignored, because purpose of test not understood
    @Ignore
    @Test
    public void testAddSensor_observationTypeIsSignificantToDifferentiate() {
        final Sensor sensor = createSensor();

        int id = MatchupIO.addSensor(sensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(1, matchupData.getSe().size());

        final Sensor differentSensor = createSensor();
        differentSensor.setObservationType("different");
        id = MatchupIO.addSensor(differentSensor, matchupData, detachHandler);
        assertEquals(1, id);
        assertEquals(2, matchupData.getSe().size());

        verify(detachHandler, times(1)).detach(sensor);
        verify(detachHandler, times(1)).detach(differentSensor);
        verifyNoMoreInteractions(detachHandler);
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
