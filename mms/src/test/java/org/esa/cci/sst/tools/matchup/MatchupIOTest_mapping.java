package org.esa.cci.sst.tools.matchup;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.Sensor;
import org.junit.Before;
import org.junit.Test;
import org.postgis.PGgeometry;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@SuppressWarnings("deprecation")
public class MatchupIOTest_mapping {

    private IdGenerator idGenerator;

    @Before
    public void setUp() {
        idGenerator = new IdGenerator(2010, 11, 16, 17);
    }

    @Test
    public void testMap_emptyList() {
        final List<Matchup> matchups = new ArrayList<>();

        final MatchupData mapped = MatchupIO.map(matchups, idGenerator);
        assertNotNull(mapped);
        assertTrue(mapped.getInsituObservations().isEmpty());
        assertTrue(mapped.getMatchups().isEmpty());
        assertTrue(mapped.getReferenceObservations().isEmpty());
        assertTrue(mapped.getRelatedObservations().isEmpty());
        assertTrue(mapped.getSensors().isEmpty());
    }

    @Test
    public void testMap_oneMatchup_oneRefObs() throws SQLException {
        final List<Matchup> matchups = new ArrayList<>();
        final Matchup matchup = new Matchup();
        matchup.setPattern(1L);
        matchup.setInvalid(true);
        final ReferenceObservation referenceObservation = new ReferenceObservation();
        referenceObservation.setName("2");
        referenceObservation.setSensor("3");
        final DataFile dataFile = new DataFile();
        dataFile.setPath("4");
        final Sensor sensor = new Sensor();
        sensor.setName("5");
        sensor.setPattern(6);
        sensor.setObservationType("7");
        dataFile.setSensor(sensor);
        referenceObservation.setDatafile(dataFile);
        referenceObservation.setRecordNo(8);
        referenceObservation.setTime(new Date(9));
        referenceObservation.setTimeRadius(10.1);
        referenceObservation.setLocation(new PGgeometry("POLYGON((0 0,5 0, 5 5, 0 5,0 0))"));
        referenceObservation.setPoint(new PGgeometry("POINT(11 12)"));
        referenceObservation.setDataset((byte) 13);
        referenceObservation.setReferenceFlag((byte) 14);
        matchup.setRefObs(referenceObservation);
        matchups.add(matchup);

        final MatchupData mapped = MatchupIO.map(matchups, idGenerator);
        assertNotNull(mapped);
        assertTrue(mapped.getInsituObservations().isEmpty());
        assertTrue(mapped.getRelatedObservations().isEmpty());

        final List<IO_Matchup> mappedMatchups = mapped.getMatchups();
        assertEquals(1, mappedMatchups.size());
        final IO_Matchup io_matchup = mappedMatchups.get(0);
        assertEquals(2010111617000000000L, io_matchup.getId());
        assertEquals(1, io_matchup.getPattern());
        assertTrue(io_matchup.isInvalid());

        final List<IO_RefObservation> referenceObservations = mapped.getReferenceObservations();
        assertEquals(1, referenceObservations.size());
        final IO_RefObservation io_refObs = referenceObservations.get(0);
        assertEquals(0, io_refObs.getId());
        assertEquals("2", io_refObs.getName());
        assertEquals("3", io_refObs.getSensor());
        assertEquals("4", io_refObs.getFilePath());
        assertEquals(1, io_refObs.getSensorId());
        assertEquals(8, io_refObs.getRecordNo());
        assertEquals(9, io_refObs.getTime().getTime());
        assertEquals(10.1, io_refObs.getTimeRadius(), 1e-8);
        assertEquals("POLYGON((0 0,5 0,5 5,0 5,0 0))", io_refObs.getLocation());
        assertEquals("POINT(11 12)", io_refObs.getPoint());
        assertEquals(13, io_refObs.getDataset());
        assertEquals(14, io_refObs.getReferenceFlag());

        final List<Sensor> sensors = mapped.getSensors();
        assertEquals(1, sensors.size());
        final Sensor mappedSensor = sensors.get(0);
        assertEquals(1, mappedSensor.getId());
        assertEquals("5", mappedSensor.getName());
        assertEquals(6, sensor.getPattern());
        assertEquals("7", sensor.getObservationType());
    }

    @Test
    public void testRestore_emptyObject() {
        final MatchupData matchupData = new MatchupData();

        final List<Matchup> matchups = MatchupIO.restore(matchupData);
        assertTrue(matchups.isEmpty());
    }

    @Test
    public void testRestore_oneMatchup_oneRefObs() {
        final MatchupData matchupData = new MatchupData();
        final IO_Matchup io_matchup = new IO_Matchup();
        io_matchup.setId(2);
        io_matchup.setPattern(3);
        io_matchup.setInvalid(false);
        matchupData.add(io_matchup);

        final List<Matchup> matchups = MatchupIO.restore(matchupData);
        assertEquals(1, matchups.size());
        final Matchup matchup = matchups.get(0);
        assertEquals(2, matchup.getId());
        assertEquals(3, matchup.getPattern());
        assertFalse(matchup.isInvalid());
    }

    // @todo test that two refObs using the same sensor refer to the same object
    // @todo test that two different sensors are both added
}
