package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class ProductObservationReaderTest {

    private static final File AMSRE_TEST_FILE =
            new File("testdata/AMSR-E/152", "20100601-AMSRE-REMSS-L2P-amsr_l2b_v05_r42958.dat-v01.nc");
    private static DataFile dataFile;
    private static ObservationReader reader;

    @BeforeClass
    public static void init() throws IOException {
        dataFile = new DataFile();
        reader = new ProductObservationReader("");
        reader.init(AMSRE_TEST_FILE, dataFile);
    }

    @AfterClass
    public static void clean() {
        try {
            reader.close();
        } catch (IOException e) {
            // ignore
        }
    }

    @Test
    public void testGetNumRecords() {
        assertEquals(1, reader.getNumRecords());
    }

    @Test
    public void testGetTime() throws IOException, InvalidRangeException {
        assertTrue(reader.getTime(0) > TimeUtil.MILLISECONDS_1981);
    }

    @Test
    public void testReadObservation() throws IOException, InvalidRangeException {
        final Observation observation = reader.readObservation(0);
        assertSame(dataFile, observation.getDatafile());
        assertEquals(reader.getTime(0), observation.getTime().getTime());
        assertNotNull(observation.getLocation());
        assertTrue(observation.getLocation().getGeometry().checkConsistency());
    }
}
