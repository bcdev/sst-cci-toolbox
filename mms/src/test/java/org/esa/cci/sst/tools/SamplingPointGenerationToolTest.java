package org.esa.cci.sst.tools;

import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.tools.samplepoint.GenerateInsituPointsWorkflow;
import org.esa.cci.sst.tools.samplepoint.GenerateSobolPointsWorkflow;
import org.esa.cci.sst.tools.samplepoint.Workflow;
import org.esa.cci.sst.tools.samplepoint.WorkflowContext;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        configuration.put(Configuration.KEY_ARCHIVE_ROOTDIR, "/path/archive");
        configuration.put("mms.source.45.sensor", "thermometer");
        configuration.put("mms.pattern.history", "4000000000000000");
        configuration.put(Configuration.KEY_MMS_SAMPLING_GENERATOR, "Cool_One");

        SamplingPointGenerationTool.assignFromConfig(workflowContext, configuration);

        assertEquals(1167609600000L, workflowContext.getStartTime());
        assertEquals(1167652800000L, workflowContext.getStopTime());
        assertEquals(517, workflowContext.getHalfRevisitTime());
        assertEquals("Mathilde", workflowContext.getSensorName());
        assertEquals(518, workflowContext.getSampleSkip());
        assertEquals(519, workflowContext.getSampleCount());
        final File archiveRootDir = workflowContext.getArchiveRootDir();
        assertNotNull(archiveRootDir);
        assertEquals(File.separator + "path" + File.separator + "archive", archiveRootDir.getPath());
        assertEquals("thermometer", workflowContext.getInsituSensorName());
        assertEquals(4611686018427387904L, workflowContext.getInsituSensorPattern());
        assertEquals("Cool_One", workflowContext.getSampleGeneratorName());
    }

    @Test
    public void testCreatePointGeneratorWorkflow() {
        final WorkflowContext workflowContext = new WorkflowContext();
        workflowContext.setInsituSensorName("wtf");
        final Storage storage = mock(Storage.class);
        final PersistenceManager persistenceManager = mock(PersistenceManager.class);
        when(persistenceManager.getStorage()).thenReturn(storage);
        workflowContext.setPersistenceManager(persistenceManager);

        workflowContext.setSampleGeneratorName("SOBOL");
        Workflow workflow = SamplingPointGenerationTool.createPointGeneratorWorkflow(workflowContext);
        assertTrue(workflow instanceof GenerateSobolPointsWorkflow);

        workflowContext.setSampleGeneratorName("INSITU");
        workflow = SamplingPointGenerationTool.createPointGeneratorWorkflow(workflowContext);
        assertTrue(workflow instanceof GenerateInsituPointsWorkflow);
    }

    @Test
    public void testCreatePointGeneratorWorkflow_invalidParameter() {
        final WorkflowContext workflowContext = new WorkflowContext();
        workflowContext.setSampleGeneratorName("STRANGE_NAME");

        try {
            SamplingPointGenerationTool.createPointGeneratorWorkflow(workflowContext);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Invalid generatorname: STRANGE_NAME", expected.getMessage());
        }
    }
}
