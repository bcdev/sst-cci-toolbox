package org.esa.cci.sst.tools.matchup;


import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class MatchupIOTest {

    private static final String EMPTY_FILE = "{\"referenceObservations\":[]}";
    private static final String ONE_REF_OBS_FILE = "{\"referenceObservations\":[{\"id\":12,\"name\":\"13\",\"sensor\":\"14\",\"filePath\":\"15\",\"sensorId\":16,\"time\":17,\"timeRadius\":18.18,\"location\":\"19\",\"point\":\"20\",\"dataset\":21,\"referenceFlag\":22}]}";

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

        final List<IO_RefObs> referenceObservations = matchupData.getReferenceObservations();
        assertNotNull(referenceObservations);
        assertTrue(referenceObservations.isEmpty());
    }

    @Test
    public void testWriteMatchupData_oneReferenceObservation() throws IOException, SQLException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MatchupData matchupData = new MatchupData();
        final IO_RefObs io_refObs = new IO_RefObs();
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

        final List<IO_RefObs> referenceObservations = matchupData.getReferenceObservations();
        assertEquals(1, referenceObservations.size());
        final IO_RefObs io_refObs = referenceObservations.get(0);
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
}
