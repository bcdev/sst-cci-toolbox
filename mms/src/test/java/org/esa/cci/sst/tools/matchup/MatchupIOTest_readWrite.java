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

    private static final String EMPTY_FILE = "{\"reo\":[],\"rlo\":[],\"iso\":[],\"glo\":[],\"se\":[],\"mu\":[]}";
    private static final String ONE_REF_OBS_FILE = "{\"reo\":[{\"id\":12,\"na\":\"13\",\"se\":\"14\",\"fp\":\"15\",\"si\":16,\"ti\":17,\"tr\":18.18,\"lo\":\"19\",\"rn\":23,\"pt\":\"20\",\"ds\":21,\"rf\":22}],\"rlo\":[],\"iso\":[],\"glo\":[],\"se\":[],\"mu\":[]}";
    private static final String ONE_REL_OBS_FILE = "{\"reo\":[],\"rlo\":[{\"id\":23,\"na\":\"24\",\"se\":\"25\",\"fp\":\"26\",\"si\":27,\"ti\":28,\"tr\":29.29,\"lo\":\"30\",\"rn\":31}],\"iso\":[],\"glo\":[],\"se\":[],\"mu\":[]}";
    private static final String ONE_INSITU_OBS_FILE = "{\"reo\":[],\"rlo\":[],\"iso\":[{\"id\":31,\"na\":\"32\",\"se\":\"33\",\"fp\":\"34\",\"si\":35,\"ti\":36,\"tr\":37.37,\"lo\":\"38\",\"rn\":39}],\"glo\":[],\"se\":[],\"mu\":[]}";
    private static final String ONE_GLOBAL_OBS_FILE = "{\"reo\":[],\"rlo\":[],\"iso\":[],\"glo\":[{\"id\":31,\"na\":\"32\",\"se\":\"33\",\"fp\":\"34\",\"si\":35,\"ti\":36,\"tr\":0.0,\"lo\":null,\"rn\":39}],\"se\":[],\"mu\":[]}";
    private static final String ONE_SENSOR_FILE = "{\"reo\":[],\"rlo\":[],\"iso\":[],\"glo\":[],\"se\":[{\"id\":39,\"name\":\"40\",\"pattern\":41,\"observationType\":\"42\"}],\"mu\":[]}";
    private static final String ONE_MATCHUP_FILE = "{\"reo\":[],\"rlo\":[],\"iso\":[],\"glo\":[],\"se\":[],\"mu\":[{\"id\":43,\"ri\":44,\"ci\":[{\"id\":45,\"td\":46.46,\"oi\":47,\"is\":false,\"gl\":false},{\"id\":48,\"td\":49.49,\"oi\":50,\"is\":true,\"gl\":false}],\"pa\":51,\"iv\":false}]}";

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

        final List<IO_RefObservation> referenceObservations = matchupData.getReo();
        assertNotNull(referenceObservations);
        assertTrue(referenceObservations.isEmpty());

        final List<IO_Observation> insituObservations = matchupData.getIso();
        assertNotNull(insituObservations);
        assertTrue(insituObservations.isEmpty());

        final List<IO_Observation> relatedObservations = matchupData.getRlo();
        assertNotNull(relatedObservations);
        assertTrue(relatedObservations.isEmpty());

        final List<IO_Matchup> matchups = matchupData.getMu();
        assertNotNull(matchups);
        assertTrue(matchups.isEmpty());

        final List<Sensor> sensors = matchupData.getSe();
        assertNotNull(sensors);
        assertTrue(sensors.isEmpty());
    }

    @Test
    public void testWriteMatchupData_oneReferenceObservation() throws IOException, SQLException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MatchupData matchupData = new MatchupData();
        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(12);
        io_refObs.setNa("13");
        io_refObs.setSe("14");
        io_refObs.setFp("15");
        io_refObs.setSi(16);
        io_refObs.setTi(new Date(17));
        io_refObs.setTr(18.18);
        io_refObs.setLo("19");
        io_refObs.setPt("20");
        io_refObs.setDs((byte) 21);
        io_refObs.setRf((byte) 22);
        io_refObs.setRn(23);
        matchupData.add(io_refObs);

        MatchupIO.writeMapped(matchupData, outputStream);

        assertEquals(ONE_REF_OBS_FILE, outputStream.toString());
    }

    @Test
    public void testReadMatchupData_oneReferenceObservation() throws IOException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ONE_REF_OBS_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.readMapped(inputStream);
        assertNotNull(matchupData);

        final List<IO_RefObservation> referenceObservations = matchupData.getReo();
        assertEquals(1, referenceObservations.size());
        final IO_RefObservation io_refObs = referenceObservations.get(0);
        assertEquals(12, io_refObs.getId());
        assertEquals("13", io_refObs.getNa());
        assertEquals("14", io_refObs.getSe());
        assertEquals("15", io_refObs.getFp());
        assertEquals(16, io_refObs.getSi());
        assertEquals(17, io_refObs.getTi().getTime());
        assertEquals(18.18, io_refObs.getTr(), 1e-8);
        assertEquals("19", io_refObs.getLo());
        assertEquals("20", io_refObs.getPt());
        assertEquals(21, io_refObs.getDs());
        assertEquals(22, io_refObs.getRf());
    }

    @Test
    public void testWriteMatchupData_oneRelatedObservation() throws IOException, SQLException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MatchupData matchupData = new MatchupData();
        final IO_Observation related = new IO_Observation();
        related.setId(23);
        related.setNa("24");
        related.setSe("25");
        related.setFp("26");
        related.setSi(27);
        related.setTi(new Date(28));
        related.setTr(29.29);
        related.setLo("30");
        related.setRn(31);
        matchupData.addRelated(related);

        MatchupIO.writeMapped(matchupData, outputStream);

        assertEquals(ONE_REL_OBS_FILE, outputStream.toString());
    }

    @Test
    public void testReadMatchupData_oneRelatedObservation() throws IOException, SQLException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ONE_REL_OBS_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.readMapped(inputStream);
        assertNotNull(matchupData);

        final List<IO_Observation> relatedObservations = matchupData.getRlo();
        assertEquals(1, relatedObservations.size());
        final IO_Observation related = relatedObservations.get(0);
        assertEquals(23, related.getId());
        assertEquals("24", related.getNa());
        assertEquals("25", related.getSe());
        assertEquals("26", related.getFp());
        assertEquals(27, related.getSi());
        assertEquals(28, related.getTi().getTime());
        assertEquals(29.29, related.getTr(), 1e-8);
        assertEquals("30", related.getLo());
    }

    @Test
    public void testWriteMatchupData_oneInsituObservation() throws IOException, SQLException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MatchupData matchupData = new MatchupData();
        final IO_Observation insitu = new IO_Observation();
        insitu.setId(31);
        insitu.setNa("32");
        insitu.setSe("33");
        insitu.setFp("34");
        insitu.setSi(35);
        insitu.setTi(new Date(36));
        insitu.setTr(37.37);
        insitu.setLo("38");
        insitu.setRn(39);
        matchupData.addInsitu(insitu);

        MatchupIO.writeMapped(matchupData, outputStream);

        assertEquals(ONE_INSITU_OBS_FILE, outputStream.toString());
    }

    @Test
    public void testWriteMatchupData_oneGlobalObservation() throws IOException, SQLException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MatchupData matchupData = new MatchupData();
        final IO_Observation global = new IO_Observation();
        global.setId(31);
        global.setNa("32");
        global.setSe("33");
        global.setFp("34");
        global.setSi(35);
        global.setTi(new Date(36));
        global.setRn(39);
        matchupData.addGlobal(global);

        MatchupIO.writeMapped(matchupData, outputStream);

        assertEquals(ONE_GLOBAL_OBS_FILE, outputStream.toString());
    }

    @Test
    public void testReadMatchupData_oneInsituObservation() throws IOException, SQLException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ONE_INSITU_OBS_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.readMapped(inputStream);
        assertNotNull(matchupData);

        final List<IO_Observation> insituObservations = matchupData.getIso();
        assertEquals(1, insituObservations.size());
        final IO_Observation insitu = insituObservations.get(0);
        assertEquals(31, insitu.getId());
        assertEquals("32", insitu.getNa());
        assertEquals("33", insitu.getSe());
        assertEquals("34", insitu.getFp());
        assertEquals(35, insitu.getSi());
        assertEquals(36, insitu.getTi().getTime());
        assertEquals(37.37, insitu.getTr(), 1e-8);
        assertEquals("38", insitu.getLo());
    }

    @Test
    public void testReadMatchupData_oneGlobalObservation() throws IOException, SQLException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ONE_GLOBAL_OBS_FILE.getBytes());

        final MatchupData matchupData = MatchupIO.readMapped(inputStream);
        assertNotNull(matchupData);

        final List<IO_Observation> globalObservations = matchupData.getGlo();
        assertEquals(1, globalObservations.size());
        final IO_Observation global = globalObservations.get(0);
        assertEquals(31, global.getId());
        assertEquals("32", global.getNa());
        assertEquals("33", global.getSe());
        assertEquals("34", global.getFp());
        assertEquals(35, global.getSi());
        assertEquals(36, global.getTi().getTime());
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

        final List<Sensor> sensors = matchupData.getSe();
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
        matchup.setRi(44);
        matchup.setPa(51);
        matchup.setIv(false);
        final IO_Coincidence coincidence_1 = new IO_Coincidence();
        coincidence_1.setId(45);
        coincidence_1.setTd(46.46);
        coincidence_1.setOi(47);
        coincidence_1.setIs(false);
        matchup.add(coincidence_1);
        final IO_Coincidence coincidence_2 = new IO_Coincidence();
        coincidence_2.setId(48);
        coincidence_2.setTd(49.49);
        coincidence_2.setOi(50);
        coincidence_2.setIs(true);
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

        final List<IO_Matchup> matchups = matchupData.getMu();
        assertEquals(1, matchups.size());
        final IO_Matchup matchup = matchups.get(0);
        assertEquals(43, matchup.getId());
        assertEquals(44, matchup.getRi());
        assertEquals(51, matchup.getPa());
        assertFalse(matchup.isIv());

        final List<IO_Coincidence> coincidences = matchup.getCi();
        assertEquals(2, coincidences.size());
        IO_Coincidence coincidence = coincidences.get(0);
        assertEquals(45, coincidence.getId());
        assertEquals(46.46, coincidence.getTd(), 1e-8);
        assertEquals(47, coincidence.getOi());
        assertFalse(coincidence.isIs());

        coincidence = coincidences.get(1);
        assertEquals(48, coincidence.getId());
        assertEquals(49.49, coincidence.getTd(), 1e-8);
        assertEquals(50, coincidence.getOi());
        assertTrue(coincidence.isIs());
    }
}
