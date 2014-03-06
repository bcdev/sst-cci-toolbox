package org.esa.cci.sst.tools;

import org.esa.cci.sst.tools.samplepoint.WorkflowContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SamplingPointGenerationToolTest {

    @Test
    public void testAssignFromConfig() {
        final WorkflowContext workflowContext = new WorkflowContext();

        final Configuration configuration = new Configuration();
        configuration.put(Configuration.KEY_MMS_SAMPLING_START_TIME, "2007-01-01T00:00:00Z");
        configuration.put(Configuration.KEY_MMS_SAMPLING_STOP_TIME, "2007-01-01T12:00:00Z");
        configuration.put(Configuration.KEY_MMS_SAMPLING_HALF_REVISIT_TIME, "517");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SENSOR, "Mathilde");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SKIP, "518");
        configuration.put(Configuration.KEY_MMS_SAMPLING_COUNT, "519");

        SamplingPointGenerationTool.assignFromConfig(workflowContext, configuration);

        assertEquals(1167609600000L, workflowContext.getStartTime());
        assertEquals(1167652800000L, workflowContext.getStopTime());
        assertEquals(517, workflowContext.getHalfRevisitTime());
        assertEquals("Mathilde", workflowContext.getSensorName());
        assertEquals(518, workflowContext.getSampleSkip());
        assertEquals(519, workflowContext.getSampleCount());
    }
}
