package org.esa.cci.sst.tools.matchup;


import org.esa.cci.sst.data.Sensor;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class MatchupIOTest_readWrite {

    private static final String EMPTY_FILE = "{\"referenceObservations\":[],\"relatedObservations\":[],\"insituObservations\":[],\"sensors\":[],\"matchups\":[]}";
    private static final String ONE_REF_OBS_FILE = "{\"referenceObservations\":[{\"id\":12,\"name\":\"13\",\"sensor\":\"14\",\"filePath\":\"15\",\"sensorId\":16,\"time\":17,\"timeRadius\":18.18,\"location\":\"19\",\"recordNo\":23,\"point\":\"20\",\"dataset\":21,\"referenceFlag\":22}],\"relatedObservations\":[],\"insituObservations\":[],\"sensors\":[],\"matchups\":[]}";
    private static final String ONE_REL_OBS_FILE = "{\"referenceObservations\":[],\"relatedObservations\":[{\"id\":23,\"name\":\"24\",\"sensor\":\"25\",\"filePath\":\"26\",\"sensorId\":27,\"time\":28,\"timeRadius\":29.29,\"location\":\"30\",\"recordNo\":31}],\"insituObservations\":[],\"sensors\":[],\"matchups\":[]}";
    private static final String ONE_INSITU_OBS_FILE = "{\"referenceObservations\":[],\"relatedObservations\":[],\"insituObservations\":[{\"id\":31,\"name\":\"32\",\"sensor\":\"33\",\"filePath\":\"34\",\"sensorId\":35,\"time\":36,\"timeRadius\":37.37,\"location\":\"38\",\"recordNo\":39}],\"sensors\":[],\"matchups\":[]}";
    private static final String ONE_SENSOR_FILE = "{\"referenceObservations\":[],\"relatedObservations\":[],\"insituObservations\":[],\"sensors\":[{\"id\":39,\"name\":\"40\",\"pattern\":41,\"observationType\":\"42\"}],\"matchups\":[]}";
    private static final String ONE_MATCHUP_FILE = "{\"referenceObservations\":[],\"relatedObservations\":[],\"insituObservations\":[],\"sensors\":[],\"matchups\":[{\"id\":43,\"refObsId\":44,\"coincidences\":[{\"id\":45,\"timeDifference\":46.46,\"observationId\":47,\"insitu\":false},{\"id\":48,\"timeDifference\":49.49,\"observationId\":50,\"insitu\":true}],\"pattern\":51,\"invalid\":false}]}";

    @Test
    public void testWriteEmptyMatchupData() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MatchupData matchupData = new MatchupData();

        MatchupIO.writeMapped(matchupData, outputStream);

        assertEquals(EMPTY_FILE, outputStream.toString());
    }

    @Test
    public void testReadEmptyStream() throws IOException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(EMPTY_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.readMapped(inputStream);
        assertNotNull(matchupData);

        final List<IO_RefObservation> referenceObservations = matchupData.getReferenceObservations();
        assertNotNull(referenceObservations);
        assertTrue(referenceObservations.isEmpty());

        final List<IO_Observation> insituObservations = matchupData.getInsituObservations();
        assertNotNull(insituObservations);
        assertTrue(insituObservations.isEmpty());

        final List<IO_Observation> relatedObservations = matchupData.getRelatedObservations();
        assertNotNull(relatedObservations);
        assertTrue(relatedObservations.isEmpty());

        final List<IO_Matchup> matchups = matchupData.getMatchups();
        assertNotNull(matchups);
        assertTrue(matchups.isEmpty());

        final List<Sensor> sensors = matchupData.getSensors();
        assertNotNull(sensors);
        assertTrue(sensors.isEmpty());
    }

    @Test
    public void testWriteMatchupData_oneReferenceObservation() throws IOException, SQLException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MatchupData matchupData = new MatchupData();
        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(12);
        io_refObs.setName("13");
        io_refObs.setSensor("14");
        io_refObs.setFilePath("15");
        io_refObs.setSensorId(16);
        io_refObs.setTime(new Date(17));
        io_refObs.setTimeRadius(18.18);
        io_refObs.setLocation("19");
        io_refObs.setPoint("20");
        io_refObs.setDataset((byte) 21);
        io_refObs.setReferenceFlag((byte) 22);
        io_refObs.setRecordNo(23);
        matchupData.add(io_refObs);

        MatchupIO.writeMapped(matchupData, outputStream);

        assertEquals(ONE_REF_OBS_FILE, outputStream.toString());
    }

    @Test
    public void testReadMatchupData_oneReferenceObservation() throws IOException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ONE_REF_OBS_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.readMapped(inputStream);
        assertNotNull(matchupData);

        final List<IO_RefObservation> referenceObservations = matchupData.getReferenceObservations();
        assertEquals(1, referenceObservations.size());
        final IO_RefObservation io_refObs = referenceObservations.get(0);
        assertEquals(12, io_refObs.getId());
        assertEquals("13", io_refObs.getName());
        assertEquals("14", io_refObs.getSensor());
        assertEquals("15", io_refObs.getFilePath());
        assertEquals(16, io_refObs.getSensorId());
        assertEquals(17, io_refObs.getTime().getTime());
        assertEquals(18.18, io_refObs.getTimeRadius(), 1e-8);
        assertEquals("19", io_refObs.getLocation());
        assertEquals("20", io_refObs.getPoint());
        assertEquals(21, io_refObs.getDataset());
        assertEquals(22, io_refObs.getReferenceFlag());
    }

    @Test
    public void testWriteMatchupData_oneRelatedObservation() throws IOException, SQLException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MatchupData matchupData = new MatchupData();
        final IO_Observation related = new IO_Observation();
        related.setId(23);
        related.setName("24");
        related.setSensor("25");
        related.setFilePath("26");
        related.setSensorId(27);
        related.setTime(new Date(28));
        related.setTimeRadius(29.29);
        related.setLocation("30");
        related.setRecordNo(31);
        matchupData.addRelated(related);

        MatchupIO.writeMapped(matchupData, outputStream);

        assertEquals(ONE_REL_OBS_FILE, outputStream.toString());
    }

    @Test
    public void testReadMatchupData_oneRelatedObservation() throws IOException, SQLException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ONE_REL_OBS_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.readMapped(inputStream);
        assertNotNull(matchupData);

        final List<IO_Observation> relatedObservations = matchupData.getRelatedObservations();
        assertEquals(1, relatedObservations.size());
        final IO_Observation related = relatedObservations.get(0);
        assertEquals(23, related.getId());
        assertEquals("24", related.getName());
        assertEquals("25", related.getSensor());
        assertEquals("26", related.getFilePath());
        assertEquals(27, related.getSensorId());
        assertEquals(28, related.getTime().getTime());
        assertEquals(29.29, related.getTimeRadius(), 1e-8);
        assertEquals("30", related.getLocation());
    }

    @Test
    public void testWriteMatchupData_oneInsituObservation() throws IOException, SQLException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MatchupData matchupData = new MatchupData();
        final IO_Observation insitu = new IO_Observation();
        insitu.setId(31);
        insitu.setName("32");
        insitu.setSensor("33");
        insitu.setFilePath("34");
        insitu.setSensorId(35);
        insitu.setTime(new Date(36));
        insitu.setTimeRadius(37.37);
        insitu.setLocation("38");
        insitu.setRecordNo(39);
        matchupData.addInsitu(insitu);

        MatchupIO.writeMapped(matchupData, outputStream);

        assertEquals(ONE_INSITU_OBS_FILE, outputStream.toString());
    }

    @Test
    public void testReadMatchupData_oneInsituObservation() throws IOException, SQLException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ONE_INSITU_OBS_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.readMapped(inputStream);
        assertNotNull(matchupData);

        final List<IO_Observation> insituObservations = matchupData.getInsituObservations();
        assertEquals(1, insituObservations.size());
        final IO_Observation insitu = insituObservations.get(0);
        assertEquals(31, insitu.getId());
        assertEquals("32", insitu.getName());
        assertEquals("33", insitu.getSensor());
        assertEquals("34", insitu.getFilePath());
        assertEquals(35, insitu.getSensorId());
        assertEquals(36, insitu.getTime().getTime());
        assertEquals(37.37, insitu.getTimeRadius(), 1e-8);
        assertEquals("38", insitu.getLocation());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWrite_oneSensor() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MatchupData matchupData = new MatchupData();

        final Sensor sensor = new Sensor();
        sensor.setId(39);
        sensor.setName("40");
        sensor.setPattern(41);
        sensor.setObservationType("42");
        matchupData.add(sensor);

        MatchupIO.writeMapped(matchupData, outputStream);

        assertEquals(ONE_SENSOR_FILE, outputStream.toString());
    }

    @Test
    public void testRead_oneSensor() throws IOException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ONE_SENSOR_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.readMapped(inputStream);
        assertNotNull(matchupData);

        final List<Sensor> sensors = matchupData.getSensors();
        assertEquals(1, sensors.size());
        final Sensor sensor = sensors.get(0);
        assertEquals(39, sensor.getId());
        assertEquals("40", sensor.getName());
        assertEquals(41, sensor.getPattern());
        assertEquals("42", sensor.getObservationType());
    }

    @Test
    public void testWrite_oneMatchupWithTwoCoincidences() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MatchupData matchupData = new MatchupData();

        final IO_Matchup matchup = new IO_Matchup();
        matchup.setId(43);
        matchup.setRefObsId(44);
        matchup.setPattern(51);
        matchup.setInvalid(false);
        final IO_Coincidence coincidence_1 = new IO_Coincidence();
        coincidence_1.setId(45);
        coincidence_1.setTimeDifference(46.46);
        coincidence_1.setObservationId(47);
        coincidence_1.setInsitu(false);
        matchup.add(coincidence_1);
        final IO_Coincidence coincidence_2 = new IO_Coincidence();
        coincidence_2.setId(48);
        coincidence_2.setTimeDifference(49.49);
        coincidence_2.setObservationId(50);
        coincidence_2.setInsitu(true);
        matchup.add(coincidence_2);
        matchupData.add(matchup);

        MatchupIO.writeMapped(matchupData, outputStream);

        assertEquals(ONE_MATCHUP_FILE, outputStream.toString());
    }

    @Test
    public void testRead_oneMatchupWithTwoCoincidences() throws IOException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ONE_MATCHUP_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.readMapped(inputStream);
        assertNotNull(matchupData);

        final List<IO_Matchup> matchups = matchupData.getMatchups();
        assertEquals(1, matchups.size());
        final IO_Matchup matchup = matchups.get(0);
        assertEquals(43, matchup.getId());
        assertEquals(44, matchup.getRefObsId());
        assertEquals(51, matchup.getPattern());
        assertFalse(matchup.isInvalid());

        final List<IO_Coincidence> coincidences = matchup.getCoincidences();
        assertEquals(2, coincidences.size());
        IO_Coincidence coincidence = coincidences.get(0);
        assertEquals(45, coincidence.getId());
        assertEquals(46.46, coincidence.getTimeDifference(), 1e-8);
        assertEquals(47, coincidence.getObservationId());
        assertFalse(coincidence.isInsitu());

        coincidence = coincidences.get(1);
        assertEquals(48, coincidence.getId());
        assertEquals(49.49, coincidence.getTimeDifference(), 1e-8);
        assertEquals(50, coincidence.getObservationId());
        assertTrue(coincidence.isInsitu());
    }
}
