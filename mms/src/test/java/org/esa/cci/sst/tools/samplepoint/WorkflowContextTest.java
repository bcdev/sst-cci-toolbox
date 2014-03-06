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
}
