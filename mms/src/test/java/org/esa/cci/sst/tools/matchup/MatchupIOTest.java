package org.esa.cci.sst.tools.matchup;


import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class MatchupIOTest {

    private static final String EMPTY_FILE = "{\"referenceObservations\":[],\"relatedObservations\":[],\"insituObservations\":[]}";
    private static final String ONE_REF_OBS_FILE = "{\"referenceObservations\":[{\"id\":12,\"name\":\"13\",\"sensor\":\"14\",\"filePath\":\"15\",\"sensorId\":16,\"time\":17,\"timeRadius\":18.18,\"location\":\"19\",\"point\":\"20\",\"dataset\":21,\"referenceFlag\":22}],\"relatedObservations\":[],\"insituObservations\":[]}";
    private static final String ONE_REL_OBS_FILE = "{\"referenceObservations\":[],\"relatedObservations\":[{\"id\":23,\"name\":\"24\",\"sensor\":\"25\",\"filePath\":\"26\",\"sensorId\":27,\"time\":28,\"timeRadius\":29.29,\"location\":\"30\"}],\"insituObservations\":[]}";
    private static final String ONE_INSITU_OBS_FILE = "{\"referenceObservations\":[],\"relatedObservations\":[],\"insituObservations\":[{\"id\":31,\"name\":\"32\",\"sensor\":\"33\",\"filePath\":\"34\",\"sensorId\":35,\"time\":36,\"timeRadius\":37.37,\"location\":\"38\"}]}";

    @Test
    public void testWriteEmptyMatchupData() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MatchupData matchupData = new MatchupData();

        MatchupIO.write(matchupData, outputStream);

        assertEquals(EMPTY_FILE, outputStream.toString());
    }

    @Test
    public void testReadEmptyStream() throws IOException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(EMPTY_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.read(inputStream);
        assertNotNull(matchupData);

        final List<IO_RefObservation> referenceObservations = matchupData.getReferenceObservations();
        assertNotNull(referenceObservations);
        assertTrue(referenceObservations.isEmpty());
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
        matchupData.add(io_refObs);

        MatchupIO.write(matchupData, outputStream);

        assertEquals(ONE_REF_OBS_FILE, outputStream.toString());
    }

    @Test
    public void testReadMatchupData_oneReferenceObservation() throws IOException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ONE_REF_OBS_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.read(inputStream);
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
        matchupData.addRelated(related);

        MatchupIO.write(matchupData, outputStream);

        assertEquals(ONE_REL_OBS_FILE, outputStream.toString());
    }

    @Test
    public void testReadMatchupData_oneRelatedObservation() throws IOException, SQLException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ONE_REL_OBS_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.read(inputStream);
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
        matchupData.addInsitu(insitu);

        MatchupIO.write(matchupData, outputStream);

        assertEquals(ONE_INSITU_OBS_FILE, outputStream.toString());
    }

    @Test
    public void testReadMatchupData_oneInsituObservation() throws IOException, SQLException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ONE_INSITU_OBS_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.read(inputStream);
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
}
