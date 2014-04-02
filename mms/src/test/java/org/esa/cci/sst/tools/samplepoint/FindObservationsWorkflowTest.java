package org.esa.cci.sst.tools.samplepoint;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class FindObservationsWorkflowTest {

    @Test
    public void testCreateFromContextForPrimary() {
        final WorkflowContext workflowContext = new WorkflowContext();

        final ObservationFinder.Parameter parameter = FindObservationsWorkflow.createFromContextForPrimary(workflowContext);
        assertNotNull(parameter);
    }
}
