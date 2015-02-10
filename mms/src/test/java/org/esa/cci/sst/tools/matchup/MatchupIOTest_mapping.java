package org.esa.cci.sst.tools.matchup;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.tool.ToolException;
import org.junit.Before;
import org.junit.Test;
import org.postgis.PGgeometry;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SuppressWarnings({"deprecation", "InstanceofInterfaces"})
public class MatchupIOTest_mapping {

    private IdGenerator idGenerator;
    private DetachHandler detachHandler;

    @Before
    public void setUp() {
        idGenerator = new IdGenerator(2010, 11, 16, 17);
        detachHandler = mock(DetachHandler.class);
    }

    @Test
    public void testMap_emptyList() {
        final List<Matchup> matchups = new ArrayList<>();

        final MatchupData mapped = MatchupIO.map(matchups, idGenerator, detachHandler);
        assertNotNull(mapped);
        assertTrue(mapped.getIso().isEmpty());
        assertTrue(mapped.getMu().isEmpty());
        assertTrue(mapped.getReo().isEmpty());
        assertTrue(mapped.getRlo().isEmpty());
        assertTrue(mapped.getSe().isEmpty());

        verifyNoMoreInteractions(detachHandler);
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
        sensor.setId(1);
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

        final MatchupData mapped = MatchupIO.map(matchups, idGenerator, detachHandler);
        assertNotNull(mapped);
        assertTrue(mapped.getIso().isEmpty());
        assertTrue(mapped.getRlo().isEmpty());

        final List<IO_Matchup> mappedMatchups = mapped.getMu();
        assertEquals(1, mappedMatchups.size());
        final IO_Matchup io_matchup = mappedMatchups.get(0);
        assertEquals(2010111617000000000L, io_matchup.getId());
        assertEquals(1, io_matchup.getPa());
        assertTrue(io_matchup.isIv());

        final List<IO_RefObservation> referenceObservations = mapped.getReo();
        assertEquals(1, referenceObservations.size());
        final IO_RefObservation io_refObs = referenceObservations.get(0);
        assertEquals(0, io_refObs.getId());
        assertEquals("2", io_refObs.getNa());
        assertEquals("3", io_refObs.getSe());
        assertEquals("4", io_refObs.getFp());
        assertEquals(1, io_refObs.getSi());
        assertEquals(8, io_refObs.getRn());
        assertEquals(9, io_refObs.getTi().getTime());
        assertEquals(10.1, io_refObs.getTr(), 1e-8);
        // @todo 2 tb/tb temporarily removed geolocation mapping 2014-12-17s
        //assertEquals("POLYGON((0 0,5 0,5 5,0 5,0 0))", io_refObs.getLocation());
        assertEquals("POINT(11 12)", io_refObs.getPt());
        assertEquals(13, io_refObs.getDs());
        assertEquals(14, io_refObs.getRf());

        final List<Sensor> sensors = mapped.getSe();
        assertEquals(1, sensors.size());
        final Sensor mappedSensor = sensors.get(0);
        assertEquals(1, mappedSensor.getId());
        assertEquals("5", mappedSensor.getName());
        assertEquals(6, sensor.getPattern());
        assertEquals("7", sensor.getObservationType());

        verify(detachHandler, times(0)).detach(matchup);
        verify(detachHandler, times(1)).detach(referenceObservation);
        verify(detachHandler, times(1)).detach(sensor);
        verifyNoMoreInteractions(detachHandler);
    }

    @Test
    public void testMap_oneMatchup_oneRelatedCoincidence() throws SQLException {
        final List<Matchup> matchups = new ArrayList<>();
        final Matchup matchup = createMatchupWithRefobsAndSensor();
        final ReferenceObservation matchupRefObs = matchup.getRefObs();

        final List<Coincidence> coincidences = new ArrayList<>();
        final Coincidence coincidence = new Coincidence();
        coincidence.setTimeDifference(127.127);
        coincidences.add(coincidence);

        final RelatedObservation relatedObservation = new RelatedObservation();
        relatedObservation.setName("128");
        relatedObservation.setSensor("129");
        relatedObservation.setId(198);
        final Sensor sensor = new Sensor();
        sensor.setName("related");
        sensor.setPattern(131);
        sensor.setObservationType("132");
        sensor.setId(2);
        final DataFile dataFile = new DataFile("130", sensor);
        relatedObservation.setDatafile(dataFile);
        relatedObservation.setRecordNo(133);
        relatedObservation.setTime(new Date(134));
        relatedObservation.setTimeRadius(135.135);
        relatedObservation.setLocation(new PGgeometry("POLYGON((10 10,11 10,11 11,10 11,10 10))"));
        coincidence.setObservation(relatedObservation);
        matchup.setCoincidences(coincidences);

        matchups.add(matchup);

        final MatchupData matchupData = MatchupIO.map(matchups, idGenerator, detachHandler);
        final List<IO_Matchup> io_matchups = matchupData.getMu();
        assertEquals(1, io_matchups.size());
        final IO_Matchup io_matchup = io_matchups.get(0);

        final List<IO_Coincidence> io_coincidences = io_matchup.getCi();
        assertEquals(1, io_coincidences.size());
        final IO_Coincidence io_coincidence = io_coincidences.get(0);
        assertEquals(127.127, io_coincidence.getTd(), 1e-8);
        assertEquals(198, io_coincidence.getOi());
        assertFalse(io_coincidence.isIs());

        final List<IO_Observation> relatedObservations = matchupData.getRlo();
        assertEquals(1, relatedObservations.size());
        final IO_Observation io_observation = relatedObservations.get(0);
        assertEquals(198, io_observation.getId());
        assertEquals("128", io_observation.getNa());
        assertEquals("129", io_observation.getSe());
        assertEquals("130", io_observation.getFp());
        assertEquals(2, io_observation.getSi());
        assertEquals(133, io_observation.getRn());
        assertEquals(134, io_observation.getTi().getTime());
        assertEquals(135.135, io_observation.getTr(), 1e-8);
        // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
        //assertEquals("POLYGON((10 10,11 10,11 11,10 11,10 10))", io_observation.getLocation());

        final List<Sensor> sensors = matchupData.getSe();
        assertEquals(2, sensors.size());    // first results from the referenceObservation
        final Sensor io_sensor = sensors.get(1);
        assertEquals(2, io_sensor.getId());

        verify(detachHandler, times(0)).detach(matchup);
        verify(detachHandler, times(1)).detach(matchupRefObs);
        verify(detachHandler, times(1)).detach(matchupRefObs.getDatafile().getSensor());
        verify(detachHandler, times(1)).detach(sensor);
        verify(detachHandler, times(0)).detach(relatedObservation);
        verify(detachHandler, times(0)).detach(coincidence);
        verifyNoMoreInteractions(detachHandler);
    }

    @Test
    public void testMap_oneMatchup_oneInsituCoincidence() throws SQLException {
        final List<Matchup> matchups = new ArrayList<>();
        final Matchup matchup = createMatchupWithRefobsAndSensor();
        final ReferenceObservation matchupRefObs = matchup.getRefObs();

        final List<Coincidence> coincidences = new ArrayList<>();
        final Coincidence coincidence = new Coincidence();
        coincidence.setTimeDifference(227.227);
        coincidences.add(coincidence);

        final InsituObservation insituObservation = new InsituObservation();
        insituObservation.setName("228");
        insituObservation.setSensor("229");
        insituObservation.setId(207);
        final Sensor sensor = new Sensor();
        sensor.setName("insitu");
        sensor.setPattern(231);
        sensor.setObservationType("232");
        sensor.setId(2);
        final DataFile dataFile = new DataFile("230", sensor);
        insituObservation.setDatafile(dataFile);
        insituObservation.setRecordNo(233);
        insituObservation.setTime(new Date(234));
        insituObservation.setTimeRadius(235.235);
        insituObservation.setLocation(new PGgeometry("POLYGON((10 10,11 10,11 11,10 11,10 10))"));
        coincidence.setObservation(insituObservation);
        matchup.setCoincidences(coincidences);

        matchups.add(matchup);

        final MatchupData matchupData = MatchupIO.map(matchups, idGenerator, detachHandler);
        final List<IO_Matchup> io_matchups = matchupData.getMu();
        assertEquals(1, io_matchups.size());
        final IO_Matchup io_matchup = io_matchups.get(0);

        final List<IO_Coincidence> io_coincidences = io_matchup.getCi();
        assertEquals(1, io_coincidences.size());
        final IO_Coincidence io_coincidence = io_coincidences.get(0);
        assertEquals(227.227, io_coincidence.getTd(), 1e-8);
        assertEquals(207, io_coincidence.getOi());
        assertTrue(io_coincidence.isIs());

        final List<IO_Observation> insituObservations = matchupData.getIso();
        assertEquals(1, insituObservations.size());
        final IO_Observation io_observation = insituObservations.get(0);
        assertEquals(207, io_observation.getId());
        assertEquals("228", io_observation.getNa());
        assertEquals("229", io_observation.getSe());
        assertEquals("230", io_observation.getFp());
        assertEquals(2, io_observation.getSi());
        assertEquals(233, io_observation.getRn());
        assertEquals(234, io_observation.getTi().getTime());
        assertEquals(235.235, io_observation.getTr(), 1e-8);
        // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
//        assertEquals("POLYGON((10 10,11 10,11 11,10 11,10 10))", io_observation.getLocation());

        final List<Sensor> sensors = matchupData.getSe();
        assertEquals(2, sensors.size());    // first results from the referenceObservation
        final Sensor io_sensor = sensors.get(1);
        assertEquals(2, io_sensor.getId());

        verify(detachHandler, times(0)).detach(matchup);
        verify(detachHandler, times(1)).detach(matchupRefObs);
        verify(detachHandler, times(1)).detach(matchupRefObs.getDatafile().getSensor());
        verify(detachHandler, times(1)).detach(sensor);
        verify(detachHandler, times(0)).detach(insituObservation);
        verify(detachHandler, times(0)).detach(coincidence);
        verifyNoMoreInteractions(detachHandler);
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
        io_matchup.setPa(3);
        io_matchup.setRi(4);
        io_matchup.setIv(false);
        matchupData.add(io_matchup);

        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(4);
        io_refObs.setNa("5");
        io_refObs.setSe("6");
        io_refObs.setFp("7");
        io_refObs.setSi(8);
        io_refObs.setRn(12);
        io_refObs.setTi(new Date(13));
        io_refObs.setTr(14.14);
        io_refObs.setLo("POLYGON((3 3,3 5,5 5,5 3,3 3))");
        io_refObs.setPt("POINT(15 16)");
        io_refObs.setDs((byte) 17);
        io_refObs.setRf((byte) 18);
        matchupData.add(io_refObs);

        final Sensor sensor = new Sensor();
        sensor.setId(8);
        sensor.setName("9");
        sensor.setPattern(10);
        sensor.setObservationType("11");
        matchupData.add(sensor);

        final List<Matchup> matchups = MatchupIO.restore(matchupData);
        assertEquals(1, matchups.size());
        final Matchup matchup = matchups.get(0);
        assertEquals(2, matchup.getId());
        assertEquals(3, matchup.getPattern());
        assertFalse(matchup.isInvalid());

        final ReferenceObservation refObs = matchup.getRefObs();
        assertNotNull(refObs);
        assertEquals(4, refObs.getId());
        assertEquals("5", refObs.getName());
        assertEquals("6", refObs.getSensor());

        final DataFile datafile = refObs.getDatafile();
        assertNotNull(datafile);
        assertEquals("7", datafile.getPath());
        final Sensor resultSensor = datafile.getSensor();
        assertNotNull(resultSensor);
        assertEquals(8, resultSensor.getId());
        assertEquals("9", resultSensor.getName());
        assertEquals(10, resultSensor.getPattern());
        assertEquals("11", resultSensor.getObservationType());

        assertEquals(12, refObs.getRecordNo());
        assertEquals(13, refObs.getTime().getTime());
        assertEquals(14.14, refObs.getTimeRadius(), 1e-8);
        // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
        // assertEquals("POLYGON((3 3,3 5,5 5,5 3,3 3))", refObs.getLocation().getValue());
        assertEquals("POINT(15 16)", refObs.getPoint().getValue());
        assertEquals(17, refObs.getDataset());
        assertEquals(18, refObs.getReferenceFlag());
    }

    @Test
    public void testRestore_oneMatchup_invalidRefObsId() {
        final MatchupData matchupData = new MatchupData();
        final IO_Matchup io_matchup = new IO_Matchup();
        io_matchup.setRi(16);
        matchupData.add(io_matchup);

        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(17);
        matchupData.add(io_refObs);

        try {
            MatchupIO.restore(matchupData);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    @Test
    public void testRestore_oneMatchup_invalidSensorId() {
        final MatchupData matchupData = new MatchupData();
        final IO_Matchup io_matchup = new IO_Matchup();
        io_matchup.setRi(16);
        matchupData.add(io_matchup);

        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(16);
        io_refObs.setLo("POLYGON((3 3,3 5,5 5,5 3,3 3))");
        io_refObs.setPt("POINT(15 16)");
        io_refObs.setSi(-99);
        matchupData.add(io_refObs);

        final Sensor sensor = new Sensor();
        sensor.setId(75);
        matchupData.add(sensor);

        try {
            MatchupIO.restore(matchupData);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
//    @Test
//    public void testRestore_oneMatchup_invalidLocationString() {
//        final MatchupData matchupData = new MatchupData();
//        final IO_Matchup io_matchup = new IO_Matchup();
//        io_matchup.setRefObsId(16);
//        matchupData.add(io_matchup);
//
//        final IO_RefObservation io_refObs = new IO_RefObservation();
//        io_refObs.setId(16);
//        io_refObs.setLocation("in the Baltic Sea");
//        io_refObs.setPoint("POINT(15 16)");
//        io_refObs.setSensorId(75);
//        matchupData.add(io_refObs);
//
//        final Sensor sensor = new Sensor();
//        sensor.setId(75);
//        matchupData.add(sensor);
//
//        try {
//            MatchupIO.restore(matchupData);
//            fail("ToolException expected");
//        } catch (ToolException expected){
//        }
//    }

    @Test
    public void testRestore_oneMatchup_invalidPointString() {
        final MatchupData matchupData = new MatchupData();
        final IO_Matchup io_matchup = new IO_Matchup();
        io_matchup.setRi(16);
        matchupData.add(io_matchup);

        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(16);
        io_refObs.setLo("POLYGON((3 3,3 5,5 5,5 3,3 3))");
        io_refObs.setPt("Big Ben");
        io_refObs.setSi(75);
        matchupData.add(io_refObs);

        final Sensor sensor = new Sensor();
        sensor.setId(75);
        matchupData.add(sensor);

        try {
            MatchupIO.restore(matchupData);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    @Test
    public void testRestore_oneMatchup_oneRelatedCoincidence() throws SQLException {
        final MatchupData matchupData = new MatchupData();
        final IO_Matchup io_matchup = new IO_Matchup();
        io_matchup.setRi(12);
        matchupData.add(io_matchup);

        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(12);
        io_refObs.setSi(13);
        io_refObs.setLo("POLYGON((0 0,1 0,1 1,0 1,0 0))");
        io_refObs.setPt("POINT(11 13)");
        matchupData.add(io_refObs);

        Sensor sensor = new Sensor();
        sensor.setId(13);
        matchupData.add(sensor);

        sensor = new Sensor();
        sensor.setId(14);
        sensor.setName("related");
        matchupData.add(sensor);

        final IO_Coincidence io_coincidence = new IO_Coincidence();
        io_coincidence.setOi(15);
        io_coincidence.setTd(16.16);
        io_coincidence.setIs(false);
        io_matchup.add(io_coincidence);

        final IO_Observation io_observation = new IO_Observation();
        io_observation.setId(15);
        io_observation.setNa("16");
        io_observation.setSe("17");
        io_observation.setFp("18");
        io_observation.setSi(14);
        io_observation.setTi(new Date(19));
        io_observation.setTr(20.2);
        io_observation.setLo("POLYGON((2 2,2 3,3 3,3 2,2 2))");
        io_observation.setRn(21);
        matchupData.addRelated(io_observation);

        final List<Matchup> matchups = MatchupIO.restore(matchupData);
        assertEquals(1, matchups.size());

        final Matchup matchup = matchups.get(0);
        final List<Coincidence> coincidences = matchup.getCoincidences();
        assertEquals(1, coincidences.size());
        final Coincidence coincidence = coincidences.get(0);
        assertEquals(16.16, coincidence.getTimeDifference(), 1e-8);
        final Observation observation = coincidence.getObservation();
        assertTrue(observation instanceof RelatedObservation);
        assertEquals("16", observation.getName());
        assertEquals("17", observation.getSensor());

        final DataFile datafile = observation.getDatafile();
        assertNotNull(datafile);
        assertEquals("18", datafile.getPath());
        final Sensor restoredSensor = datafile.getSensor();
        assertNotNull(restoredSensor);
        assertEquals(14, sensor.getId());
        assertEquals("related", sensor.getName());

        assertEquals(19, ((RelatedObservation) observation).getTime().getTime());
        assertEquals(20.2, ((RelatedObservation) observation).getTimeRadius(), 1e-8);
        // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
//        assertEquals("POLYGON((2 2,2 3,3 3,3 2,2 2))", ((RelatedObservation) observation).getLocation().getValue());
        assertEquals(21, observation.getRecordNo());
    }

    @Test
    public void testRestore_oneMatchup_invalidRelatedObservationId() {
        final MatchupData matchupData = new MatchupData();
        final IO_Matchup io_matchup = new IO_Matchup();
        io_matchup.setRi(12);
        matchupData.add(io_matchup);

        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(12);
        io_refObs.setSi(13);
        io_refObs.setLo("POLYGON((0 0,1 0,1 1,0 1,0 0))");
        io_refObs.setPt("POINT(11 13)");
        matchupData.add(io_refObs);

        Sensor sensor = new Sensor();
        sensor.setId(13);
        matchupData.add(sensor);

        final IO_Coincidence io_coincidence = new IO_Coincidence();
        io_coincidence.setOi(-99);
        io_matchup.add(io_coincidence);

        final IO_Observation io_observation = new IO_Observation();
        io_observation.setId(15);
        matchupData.addRelated(io_observation);

        try {
            MatchupIO.restore(matchupData);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    @Test
    public void testRestore_oneMatchup_oneInsituCoincidence() throws SQLException {
        final MatchupData matchupData = new MatchupData();
        final IO_Matchup io_matchup = new IO_Matchup();
        io_matchup.setRi(114);
        matchupData.add(io_matchup);

        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(114);
        io_refObs.setSi(19);
        io_refObs.setLo("POLYGON((0 0,1 0,1 1,0 1,0 0))");
        io_refObs.setPt("POINT(11 13)");
        matchupData.add(io_refObs);

        Sensor sensor = new Sensor();
        sensor.setId(19);
        matchupData.add(sensor);

        sensor = new Sensor();
        sensor.setId(24);
        sensor.setName("insitu");
        matchupData.add(sensor);

        final IO_Coincidence io_coincidence = new IO_Coincidence();
        io_coincidence.setOi(20);
        io_coincidence.setTd(21.21);
        io_coincidence.setIs(true);
        io_matchup.add(io_coincidence);

        final IO_Observation io_observation = new IO_Observation();
        io_observation.setId(20);
        io_observation.setNa("21");
        io_observation.setSe("22");
        io_observation.setFp("23");
        io_observation.setSi(24);
        io_observation.setTi(new Date(25));
        io_observation.setTr(26.26);
        io_observation.setLo("POLYGON((2 2,2 3,3 3,3 2,2 2))");
        io_observation.setRn(27);
        matchupData.addInsitu(io_observation);

        final List<Matchup> matchups = MatchupIO.restore(matchupData);
        assertEquals(1, matchups.size());

        final Matchup matchup = matchups.get(0);
        final List<Coincidence> coincidences = matchup.getCoincidences();
        assertEquals(1, coincidences.size());

        final Coincidence coincidence = coincidences.get(0);
        assertEquals(21.21, coincidence.getTimeDifference(), 1e-8);

        final Observation observation = coincidence.getObservation();
        assertTrue(observation instanceof InsituObservation);
        assertEquals("21", observation.getName());
        assertEquals("22", observation.getSensor());

        final DataFile datafile = observation.getDatafile();
        assertNotNull(datafile);
        assertEquals("23", datafile.getPath());
        final Sensor restoredSensor = datafile.getSensor();
        assertNotNull(restoredSensor);
        assertEquals(24, sensor.getId());
        assertEquals("insitu", sensor.getName());

        assertEquals(25, ((InsituObservation) observation).getTime().getTime());
        assertEquals(26.26, ((InsituObservation) observation).getTimeRadius(), 1e-8);
        // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
        //assertEquals("POLYGON((2 2,2 3,3 3,3 2,2 2))", ((InsituObservation) observation).getLocation().getValue());
        assertEquals(27, observation.getRecordNo());
    }

    @Test
    public void testRestore_oneMatchup_invalidInsituObservationId() {
        final MatchupData matchupData = new MatchupData();
        final IO_Matchup io_matchup = new IO_Matchup();
        io_matchup.setRi(12);
        matchupData.add(io_matchup);

        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(12);
        io_refObs.setSi(13);
        io_refObs.setLo("POLYGON((0 0,1 0,1 1,0 1,0 0))");
        io_refObs.setPt("POINT(11 13)");
        matchupData.add(io_refObs);

        Sensor sensor = new Sensor();
        sensor.setId(13);
        matchupData.add(sensor);

        final IO_Coincidence io_coincidence = new IO_Coincidence();
        io_coincidence.setOi(-99);
        io_coincidence.setIs(true);
        io_matchup.add(io_coincidence);

        final IO_Observation io_observation = new IO_Observation();
        io_observation.setId(15);
        matchupData.addInsitu(io_observation);

        try {
            MatchupIO.restore(matchupData);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    private Matchup createMatchupWithRefobsAndSensor() throws SQLException {
        final Matchup matchup = new Matchup();
        final ReferenceObservation refObs = new ReferenceObservation();
        final DataFile datafile = new DataFile();
        final Sensor sensor = new Sensor();
        sensor.setName("default");
        datafile.setSensor(sensor);
        refObs.setDatafile(datafile);
        refObs.setLocation(new PGgeometry("POLYGON(0 0,1 0,1 1,0 1,0 0))"));
        refObs.setPoint(new PGgeometry("POINT(19 -11)"));
        matchup.setRefObs(refObs);
        return matchup;
    }
}
