package org.esa.cci.sst.tools;

import org.esa.cci.sst.common.InsituDatasetId;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Test;
import org.postgis.PGgeometry;
import org.postgis.Point;

import static org.junit.Assert.*;

public class MatchupGeneratorTest {

    @Test
    public void testCreateSensorShortName() {
         assertEquals("his_avhrr.n13", MatchupGenerator.createSensorShortName("history", "avhrr.n13"));
         assertEquals("ins_atsr.2", MatchupGenerator.createSensorShortName("insitu", "atsr.2"));
    }

    @Test
    public void testCreateSensorShortName_throwsOnNonStandardPrimarySensorname() {
        try {
            MatchupGenerator.createSensorShortName("history", "avhrr.full_resolution");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCreateReferenceObservation_sobolPoint() {
        final SamplingPoint samplingPoint = new SamplingPoint();
        samplingPoint.setIndex(77845);
        samplingPoint.setReferenceLon(22.2);
        samplingPoint.setReferenceLat(33.3);
        samplingPoint.setReferenceTime(776636483L);
        samplingPoint.setInsituDatasetId(InsituDatasetId.dummy_diurnal_variability);
        final DataFile datafile = new DataFile();

        final ReferenceObservation referenceObservation = MatchupGenerator.createReferenceObservation("Sobol", samplingPoint, datafile);
        assertNotNull(referenceObservation);
        assertEquals("77845", referenceObservation.getName());
        assertEquals("Sobol", referenceObservation.getSensor());

        final PGgeometry location = referenceObservation.getLocation();
        final PGgeometry point = referenceObservation.getPoint();
        assertSame(location, point);
        final Point locationPoint = location.getGeometry().getPoint(0);
        assertEquals(22.2, locationPoint.getX(), 1e-8);
        assertEquals(33.3, locationPoint.getY(), 1e-8);

        assertEquals(776636483L, referenceObservation.getTime().getTime());
        assertEquals(0.0, referenceObservation.getTimeRadius(), 1e-8);

        assertSame(datafile, referenceObservation.getDatafile());
        assertEquals(0, referenceObservation.getRecordNo());
        assertEquals(InsituDatasetId.dummy_diurnal_variability.getValue(), referenceObservation.getDataset());
        assertEquals(Constants.MATCHUP_REFERENCE_FLAG_UNDEFINED, referenceObservation.getReferenceFlag());
    }

    @Test
    public void testCreateReferenceObservation_insituPoint() {
        final SamplingPoint samplingPoint = new SamplingPoint(1, 2, 76636483L, Double.NaN);
        samplingPoint.setIndex(87845);
        samplingPoint.setReferenceLon(32.2);
        samplingPoint.setReferenceLat(43.3);
        samplingPoint.setReferenceTime(876636483L);
        samplingPoint.setInsituDatasetId(InsituDatasetId.radiometer);
        final DataFile datafile = new DataFile();

        final ReferenceObservation referenceObservation = MatchupGenerator.createReferenceObservation("Bottle", samplingPoint, datafile);
        assertNotNull(referenceObservation);
        assertEquals("87845", referenceObservation.getName());
        assertEquals("Bottle", referenceObservation.getSensor());

        final PGgeometry location = referenceObservation.getLocation();
        final PGgeometry point = referenceObservation.getPoint();
        assertSame(location, point);
        final Point locationPoint = location.getGeometry().getPoint(0);
        assertEquals(32.2, locationPoint.getX(), 1e-8);
        assertEquals(43.3, locationPoint.getY(), 1e-8);

        assertEquals(876636483L, referenceObservation.getTime().getTime());
        assertEquals(800000.0, referenceObservation.getTimeRadius(), 1e-8);

        assertSame(datafile, referenceObservation.getDatafile());
        assertEquals(0, referenceObservation.getRecordNo());
        assertEquals(InsituDatasetId.radiometer.getValue(), referenceObservation.getDataset());
        assertEquals(Constants.MATCHUP_REFERENCE_FLAG_UNDEFINED, referenceObservation.getReferenceFlag());
    }
}
