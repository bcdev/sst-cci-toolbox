package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.tools.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class WorkflowContextTest {

    private WorkflowContext workflowContext;

    @Before
    public void setUp() {
        workflowContext = new WorkflowContext();
    }

    @Test
    public void testSetGetStartTime() {
        final long start_1 = 44578;
        final long start_2 = 7331269;

        workflowContext.setStartTime(start_1);
        assertEquals(start_1, workflowContext.getStartTime());

        workflowContext.setStartTime(start_2);
        assertEquals(start_2, workflowContext.getStartTime());
    }

    @Test
    public void testSetGetStopTime() {
        final long stop_1 = 85442;
        final long stop_2 = 99552;

        workflowContext.setStopTime(stop_1);
        assertEquals(stop_1, workflowContext.getStopTime());

        workflowContext.setStopTime(stop_2);
        assertEquals(stop_2, workflowContext.getStopTime());
    }

    @Test
    public void testSetGetHalfRevisitTime() {
        final int halfTime_1 = 45;
        final int halfTime_2 = 1098876;

        workflowContext.setHalfRevisitTime(halfTime_1);
        assertEquals(halfTime_1, workflowContext.getHalfRevisitTime());

        workflowContext.setHalfRevisitTime(halfTime_2);
        assertEquals(halfTime_2, workflowContext.getHalfRevisitTime());
    }

    @Test
    public void testSetGetLogger() {
        final Logger logger = Logger.getAnonymousLogger();

        workflowContext.setLogger(logger);
        assertSame(logger, workflowContext.getLogger());
    }

    @Test
    public void testSetGetConfig() {
        final Configuration configuration = new Configuration();

        workflowContext.setConfig(configuration);
        assertSame(configuration, workflowContext.getConfig());
    }

    @Test
    public void testSetGetSensorName() {
        final String sensorName = "odometer";

        workflowContext.setSensorName(sensorName);
        assertEquals(sensorName, workflowContext.getSensorName());
    }

    @Test
    public void testSetGetSampleCount() {
        final int count_1 = 776;
        final int count_2 = 87665;

        workflowContext.setSampleCount(count_1);
        assertEquals(count_1, workflowContext.getSampleCount());

        workflowContext.setSampleCount(count_2);
        assertEquals(count_2, workflowContext.getSampleCount());
    }

    @Test
    public void testSetGetSampleSkip() {
        final int skip_1 = 99;
        final int skip_2 = 75622;

        workflowContext.setSampleSkip(skip_1);
        assertEquals(skip_1, workflowContext.getSampleSkip());

        workflowContext.setSampleSkip(skip_2);
        assertEquals(skip_2, workflowContext.getSampleSkip());
    }
}
