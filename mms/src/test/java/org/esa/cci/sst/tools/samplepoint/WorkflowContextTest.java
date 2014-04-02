package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.tools.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
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
    public void testSetGetSearchTimePast() {
        final int searchTime_1 = 45;
        final int searchTime_2 = 1098876;

        workflowContext.setSearchTimePast(searchTime_1);
        assertEquals(searchTime_1, workflowContext.getSearchTimePast());

        workflowContext.setSearchTimePast(searchTime_2);
        assertEquals(searchTime_2, workflowContext.getSearchTimePast());
    }

    @Test
    public void testSetGetSearchTimeFuture() {
        final int searchTime_1 = 56;
        final int searchTime_2 = 7765;

        workflowContext.setSearchTimeFuture(searchTime_1);
        assertEquals(searchTime_1, workflowContext.getSearchTimeFuture());

        workflowContext.setSearchTimeFuture(searchTime_2);
        assertEquals(searchTime_2, workflowContext.getSearchTimeFuture());
    }

    @Test
    public void testSetGetSearchTimePast2() {
        final int searchTime_1 = 47;
        final int searchTime_2 = 1044876;

        workflowContext.setSearchTimePast2(searchTime_1);
        assertEquals(searchTime_1, workflowContext.getSearchTimePast2());

        workflowContext.setSearchTimePast2(searchTime_2);
        assertEquals(searchTime_2, workflowContext.getSearchTimePast2());
    }

    @Test
    public void testSetGetSearchTimeFuture2() {
        final int searchTime_1 = 57;
        final int searchTime_2 = 7722;

        workflowContext.setSearchTimeFuture2(searchTime_1);
        assertEquals(searchTime_1, workflowContext.getSearchTimeFuture2());

        workflowContext.setSearchTimeFuture2(searchTime_2);
        assertEquals(searchTime_2, workflowContext.getSearchTimeFuture2());
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

    @Test
    public void testSetGetArchiveRootDir() {
        final File rootDir = new File("/home/archive");

        workflowContext.setArchiveRootDir(rootDir);
        assertSameAbsolutePath(rootDir, workflowContext.getArchiveRootDir());
    }

    @Test
    public void testSetGetInsituSensorName() {
        final String name_1 = "instrument";
        final String name_2 = "a_Sensor";

        workflowContext.setInsituSensorName(name_1);
        assertEquals(name_1, workflowContext.getInsituSensorName());

        workflowContext.setInsituSensorName(name_2);
        assertEquals(name_2, workflowContext.getInsituSensorName());
    }

    @Test
    public void testSetGetInsituSensorPattern() {
        final long pattern_1 = 4000000000000000L;
        final long pattern_2 = 8000000000000000L;

        workflowContext.setInsituSensorPattern(pattern_1);
        assertEquals(pattern_1, workflowContext.getInsituSensorPattern());

        workflowContext.setInsituSensorPattern(pattern_2);
        assertEquals(pattern_2, workflowContext.getInsituSensorPattern());
    }

    @Test
    public void testSetGetSampleGeneratorName() {
        final String generatorName_1 = "willi";
        final String generatorName_2 = "charlotte";

        workflowContext.setSampleGeneratorName(generatorName_1);
        assertEquals(generatorName_1, workflowContext.getSampleGeneratorName());

        workflowContext.setSampleGeneratorName(generatorName_2);
        assertEquals(generatorName_2, workflowContext.getSampleGeneratorName());
    }

    @Test
    public void testSetGetSensorName2() {
         final String name_1 = "Charly";
         final String name_2 = "Rene";

        workflowContext.setSensorName2(name_1);
        assertEquals(name_1, workflowContext.getSensorName2());

        workflowContext.setSensorName2(name_2);
        assertEquals(name_2, workflowContext.getSensorName2());
    }

    private static void assertSameAbsolutePath(File rootDir, File archiveRootDir) {
        assertEquals(rootDir.getAbsolutePath(), archiveRootDir.getAbsolutePath());
    }
}
