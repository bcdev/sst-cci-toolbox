package org.esa.cci.sst.tools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArchiveUtilsTest {

    @Test
    public void testCreateCleanFilePath() {
        assertEquals("/archive/root/clean/avhrr_orb.n10,history/2008/avhrr_orb.n10,history-clean-2008-08.json",
                ArchiveUtils.createCleanFilePath("/archive/root", new String[]{"avhrr_orb.n10", "history"}, 2008, 8));


        assertEquals("/archive/root/clean/orb_avhrr.n18/1995/orb_avhrr.n18-clean-1995-11.json",
                ArchiveUtils.createCleanFilePath("/archive/root", new String[]{"orb_avhrr.n18"}, 1995, 11));

        assertEquals("/archive/root/clean/amsre,avhrr.m01,history/2006/amsre,avhrr.m01,history-clean-2006-02.json",
                ArchiveUtils.createCleanFilePath("/archive/root", new String[]{"amsre", "avhrr.m01", "history"}, 2006, 2));
    }

    @Test
    public void testCreateCleanEnvFilePath() {
        assertEquals("/archive/root/clean-env/avhrr_orb.n10,history/2008/avhrr_orb.n10,history-clean-env-2008-08.json",
                ArchiveUtils.createCleanEnvFilePath("/archive/root", new String[]{"avhrr_orb.n10", "history"}, 2008, 8));


        assertEquals("/archive/root/clean-env/orb_avhrr.n18/1995/orb_avhrr.n18-clean-env-1995-11.json",
                ArchiveUtils.createCleanEnvFilePath("/archive/root", new String[]{"orb_avhrr.n18"}, 1995, 11));

        assertEquals("/archive/root/clean-env/amsre,avhrr.m01,history/2006/amsre,avhrr.m01,history-clean-env-2006-02.json",
                ArchiveUtils.createCleanEnvFilePath("/archive/root", new String[]{"amsre", "avhrr.m01", "history"}, 2006, 2));
    }

    @Test
    public void testCreateArchiveWildcardPath() {
         assertEquals("/archive/root/clean-env/amsre*/2008/amsre*-clean-env-2008-07.json",
                 ArchiveUtils.createWildcardPath("/archive/root", "amsre", "clean-env", 2008, 7));
    }

    @Test
    public void testCreateSensorNamesArray_OneSensor() throws Exception {
        final String[] sensorNames = ArchiveUtils.createSensorNamesArray("ralf", null, null);

        assertEquals(1, sensorNames.length);
        assertEquals("ralf", sensorNames[0]);
    }

    @Test
    public void testCreateSensorNamesArray_OneSensorWithInsitu() throws Exception {
        final String[] sensorNames = ArchiveUtils.createSensorNamesArray("ralf", null, "prisemut");

        assertEquals(2, sensorNames.length);
        assertEquals("ralf", sensorNames[0]);
        assertEquals("history", sensorNames[1]);
    }

    @Test
    public void testCreateSensorNamesArray_TwoSensors() throws Exception {
        final String[] sensorNames = ArchiveUtils.createSensorNamesArray("ralf", "tom", null);

        assertEquals(2, sensorNames.length);
        assertEquals("ralf", sensorNames[0]);
        assertEquals("tom", sensorNames[1]);
    }

    @Test
    public void testCreateSensorNamesArray_TwoSensorsWithInsitu() throws Exception {
        final String[] sensorNames = ArchiveUtils.createSensorNamesArray("ralf", "tom", "prisemut");

        assertEquals(3, sensorNames.length);
        assertEquals("ralf", sensorNames[0]);
        assertEquals("tom", sensorNames[1]);
        assertEquals("history", sensorNames[2]);
    }
}
