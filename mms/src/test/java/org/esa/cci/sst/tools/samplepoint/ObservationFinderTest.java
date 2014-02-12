package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.overlap.PolarOrbitingPolygon;
import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

public class ObservationFinderTest {

    private List<SamplingPoint> samples;

    @Before
    public void setUp() throws Exception {
        samples = new SobolSamplePointGenerator().createSamples(1000, 0, 0, 1000);
    }

    @Test
    @Ignore
    public void testFindObservations() throws Exception {
        boolean secondSensor = false;
        final long searchTimeDeltaMillis = 86400 * 1000;
        ObservationFinder.findObservations(samples, secondSensor, searchTimeDeltaMillis, new PolarOrbitingPolygon[0]);

        // TODO - continue here
    }
}
