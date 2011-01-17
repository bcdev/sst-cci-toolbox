package org.esa.cci.sst.reader;

import junit.framework.TestCase;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class AmsreObservationReaderTest extends TestCase {


    @Test
    public void testReadObservation() throws IOException, InvalidRangeException {
        final File testFile = new File("/mnt/hgfs/EOData/sst-cci-toolbox/mmd_test_month/AMSR-E/152/20100601-AMSRE-REMSS-L2P-amsr_l2b_v05_r42958.dat-v01.nc.gz");
        final AmsreObservationReader reader = new AmsreObservationReader();
        final DataFile dataFileEntry = new DataFile();
        reader.init(testFile, dataFileEntry);
        assertEquals(1, reader.getNumRecords());
        final long timeAtRec0 = reader.getTime(0);
        assertTrue(timeAtRec0 > TimeUtil.MILLISECONDS_1981);

        final Observation observation = reader.readObservation(0);
        assertNotNull(observation.getDatafile());
        assertSame(dataFileEntry, observation.getDatafile());
        assertEquals(new Date(timeAtRec0), observation.getTime());
        assertEquals(null, observation.getLocation());

    }
}
