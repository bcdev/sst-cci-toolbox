package org.esa.cci.sst.tools.matchup;

import org.esa.beam.util.io.FileUtils;
import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.TestHelper;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.util.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.postgis.PGgeometry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

@RunWith(IoTestRunner.class)
public class MatchupIOIntegrationTest {

    @Test
    public void testReadFromFile() throws IOException {
        final String resourcePath = TestHelper.getResourcePath(MatchupIOIntegrationTest.class, "test_matchups.json");
        final FileInputStream inputStream = new FileInputStream(new File(resourcePath));

        final List<Matchup> matchups = MatchupIO.read(inputStream);
        assertNotNull(matchups);
        assertEquals(1, matchups.size());

        final Matchup matchup = matchups.get(0);
        assertEquals(2005060908000000001L, matchup.getId());
        assertEquals(28, matchup.getPattern());
        assertFalse(matchup.isInvalid());

        final ReferenceObservation refObs = matchup.getRefObs();
        assertNotNull(refObs);
        assertEquals(12, refObs.getId());
        assertEquals("ref_obs_1", refObs.getName());
        assertEquals("orb_atsr.3", refObs.getSensor());
        assertEquals("/archive/sensor/atsr.3/the_file", refObs.getDatafile().getPath());
        assertEquals(1417010817173L, refObs.getTime().getTime());
        assertEquals(12000.0, refObs.getTimeRadius(), 1e-8);
        // todo 2 tb/tb temporarily deactivated due to memory issues 2015-02-09
        assertNull(refObs.getLocation());
        assertEquals(101, refObs.getRecordNo());
        assertEquals("POINT(10.5 10.5)", refObs.getPoint().toString());
        assertEquals(11, refObs.getDataset());
        assertEquals(8, refObs.getReferenceFlag());
    }

    @Test
    public void testWriteToFile() {
        // @todo 2 tb/tb implement 2014-11-26
    }

    @SuppressWarnings("deprecation")
    @Ignore
    @Test
    public void testWriteToFile_manyMatchups() throws IOException, SQLException {
        final int numMatchups = 3160307;

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final List<Matchup> matchups = new ArrayList<>();
        for (int i = 0; i < numMatchups; i++) {
            final Matchup matchup = new Matchup();

            final ReferenceObservation refObs = new ReferenceObservation();
            final DataFile dataFile = new DataFile();
            final Sensor sensor = new Sensor();
            sensor.setName("sensor_test");
            dataFile.setSensor(sensor);
            refObs.setDatafile(dataFile);
            refObs.setPoint(new PGgeometry("POINT(12 45)"));

            matchup.setRefObs(refObs);

            matchups.add(matchup);
        }

        stopWatch.stop();
        System.out.println("assemble data structures " + ((double)stopWatch.getElapsedMillis()) / 1000.0 + " sec");

        final File targetDirectory = new File("test_out");
        try {
            if (!targetDirectory.mkdirs()) {
                fail("unable to create target directory");
            }

            final File file = new File(targetDirectory, "test_data.json");
            if (!file.createNewFile()) {
                fail("unable to create target file");
            }

            final Properties properties = new Properties();
            properties.setProperty(Configuration.KEY_MMS_SAMPLING_START_TIME, "2010-06-01T00:00:00Z");
            properties.setProperty(Configuration.KEY_MMS_SAMPLING_STOP_TIME, "2010-07-01T00:00:00Z");
            properties.setProperty(Configuration.KEY_MMS_SAMPLING_SENSOR, "avhrr.n18");
            final Configuration configuration = new Configuration();
            configuration.add(properties);

            stopWatch.start();
            final FileOutputStream outputStream = new FileOutputStream(file);
            MatchupIO.write(matchups, outputStream, configuration, null);
            outputStream.close();

            stopWatch.stop();

            System.out.println("write data to disk " + ((double)stopWatch.getElapsedMillis()) / 1000.0 + " sec");
        } finally {
            if (!FileUtils.deleteTree(targetDirectory)) {
                fail("unable to delete test directory");
            }
        }

    }
}
