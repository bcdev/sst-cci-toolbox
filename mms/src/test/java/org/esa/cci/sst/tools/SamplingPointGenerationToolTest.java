package org.esa.cci.sst.tools;

import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tools.samplepoint.GenerateInsituPointsWorkflow;
import org.esa.cci.sst.tools.samplepoint.GenerateSobolPointsWorkflow;
import org.esa.cci.sst.tools.samplepoint.Workflow;
import org.esa.cci.sst.tools.samplepoint.WorkflowContext;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamplingPointGenerationToolTest {

    private WorkflowContext workflowContext;

    @Before
    public void setUp(){
        workflowContext = new WorkflowContext();
    }

    @Test
    public void testAssignFromConfig() {
        final Configuration configuration = new Configuration();
        configuration.put(Configuration.KEY_MMS_SAMPLING_START_TIME, "2007-01-01T00:00:00Z");
        configuration.put(Configuration.KEY_MMS_SAMPLING_STOP_TIME, "2007-01-01T12:00:00Z");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SEARCH_TIME_PAST, "517");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SEARCH_TIME_FUTURE, "523");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SENSOR, "Mathilde,Hieronimus");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SKIP, "518");
        configuration.put(Configuration.KEY_MMS_SAMPLING_COUNT, "519");
        configuration.put(Configuration.KEY_MMS_ARCHIVE_ROOT, "/path/archive");
        configuration.put(Configuration.KEY_MMS_SAMPLING_INSITU_SENSOR, "thermometer");
        configuration.put("mms.pattern.thermometer", "4000000000000000");
        configuration.put(Configuration.KEY_MMS_SAMPLING_GENERATOR, "Cool_One");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SEARCH_TIME_PAST_2, "1517");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SEARCH_TIME_FUTURE_2, "1523");

        SamplingPointGenerationTool.assignFromConfig(workflowContext, configuration);

        assertEquals(1167609600000L, workflowContext.getStartTime());
        assertEquals(1167609599000L, workflowContext.getStopTime());
        assertEquals(517, workflowContext.getSearchTimePast());
        assertEquals(523, workflowContext.getSearchTimeFuture());
        assertEquals("Mathilde", workflowContext.getSensorName1());
        assertEquals(518, workflowContext.getSampleSkip());
        assertEquals(519, workflowContext.getSampleCount());
        final File archiveRootDir = workflowContext.getArchiveRootDir();
        assertNotNull(archiveRootDir);
        assertEquals(File.separator + "path" + File.separator + "archive", archiveRootDir.getPath());
        assertEquals("thermometer", workflowContext.getInsituSensorName());
        assertEquals(4611686018427387904L, workflowContext.getInsituSensorPattern());
        assertEquals("Cool_One", workflowContext.getSampleGeneratorName());
        assertEquals("Hieronimus", workflowContext.getSensorName2());
        assertEquals(1517, workflowContext.getSearchTimePast2());
        assertEquals(1523, workflowContext.getSearchTimeFuture2());
    }

    @Test
    public void testAssignFromConfig_sensorName2IsNullWhenNotPresentInConfig() {
        final Configuration configuration = new Configuration();
        configuration.put(Configuration.KEY_MMS_SAMPLING_START_TIME, "2008-02-02T00:00:00Z");
        configuration.put(Configuration.KEY_MMS_SAMPLING_STOP_TIME, "2008-02-03T12:00:00Z");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SEARCH_TIME_PAST, "619");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SEARCH_TIME_FUTURE, "719");
        configuration.put(Configuration.KEY_MMS_SAMPLING_SENSOR, "Herrmann");
        configuration.put(Configuration.KEY_MMS_SAMPLING_COUNT, "520");
        configuration.put(Configuration.KEY_MMS_ARCHIVE_ROOT, "/path/archive/root");
        configuration.put(Configuration.KEY_MMS_SAMPLING_GENERATOR, "Yes");

        SamplingPointGenerationTool.assignFromConfig(workflowContext, configuration);
        assertNull(workflowContext.getSensorName2());
    }

    @Test
    public void testCreatePointGeneratorWorkflow() {
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
        workflowContext.setSampleGeneratorName("STRANGE_NAME");

        try {
            SamplingPointGenerationTool.createPointGeneratorWorkflow(workflowContext);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Invalid generator name: STRANGE_NAME", expected.getMessage());
        }
    }
}
