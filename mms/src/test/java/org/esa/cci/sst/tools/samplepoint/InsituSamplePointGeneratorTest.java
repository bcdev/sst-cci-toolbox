package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InsituSamplePointGeneratorTest {

    @Test
    public void testGenerate_noTimeConstraints() throws URISyntaxException {
        final URL testArchiveUrl = InsituSamplePointGeneratorTest.class.getResource("../../reader/insitu_0_WMOID_71569_20030117_20030131.nc");
        final URI uri = testArchiveUrl.toURI();
        final File archiveDir = new File(uri).getParentFile();

        final InsituSamplePointGenerator generator = new InsituSamplePointGenerator(archiveDir);
        final List<SamplingPoint> inSituPoints = generator.generate();
        assertNotNull(inSituPoints);
        assertEquals(223 + 3 + 1285, inSituPoints.size());
    }
}
