package org.esa.cci.sst.tools.samplepoint;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FindObservationsWorkflowTest {

    @Test
    public void testCreateFromContextForPrimary() {
        final WorkflowContext workflowContext = new WorkflowContext();
        workflowContext.setStartTime(1);
        workflowContext.setStopTime(2);
        workflowContext.setSearchTimePast(3);
        workflowContext.setSearchTimeFuture(4);
        workflowContext.setSensorName("sensi");

        final ObservationFinder.Parameter parameter = FindObservationsWorkflow.createFromContextForPrimary(workflowContext);
        assertNotNull(parameter);
        assertEquals(1, parameter.getStartTime());
        assertEquals(2, parameter.getStopTime());
        assertEquals(3, parameter.getSearchTimePast());
        assertEquals(4, parameter.getSearchTimeFuture());
        assertEquals("sensi", parameter.getSensorName());
    }

    @Test
    public void testCreateFromContextForSecondary() {
        final WorkflowContext workflowContext = new WorkflowContext();
        workflowContext.setStartTime(5);
        workflowContext.setStopTime(6);
        workflowContext.setSearchTimePast(7);
        workflowContext.setSearchTimeFuture(8);
        workflowContext.setSensorName2("thermometer");

        final ObservationFinder.Parameter parameter = FindObservationsWorkflow.createFromContextForSecondary(workflowContext);
        assertNotNull(parameter);
        assertEquals(5, parameter.getStartTime());
        assertEquals(6, parameter.getStopTime());
        assertEquals(7, parameter.getSearchTimePast());
        assertEquals(8, parameter.getSearchTimeFuture());
        assertEquals("thermometer", parameter.getSensorName());
    }
}
