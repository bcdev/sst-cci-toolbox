package org.esa.cci.sst.tools.matchup;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.Sensor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class MatchupIO {

    // package access for testing only tb 2014-11-21
    static void write(MatchupData matchupData, OutputStream outputStream) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(outputStream, matchupData);
    }

    // package access for testing only tb 2014-11-21
    static MatchupData read(ByteArrayInputStream inputStream) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputStream, MatchupData.class);
    }

    public static MatchupData map(List<Matchup> matchups, IdGenerator idGenerator) {
        final MatchupData matchupData = new MatchupData();

        for (final Matchup matchup : matchups) {
            final IO_Matchup io_matchup = createIO_Matchup(matchup, idGenerator);
            matchupData.add(io_matchup);

            final IO_RefObservation io_refObs = createIO_RefObs(idGenerator, matchupData, matchup);
            matchupData.add(io_refObs);
        }
        return matchupData;
    }

    public static List<Matchup> restore(MatchupData matchupData) {
        final ArrayList<Matchup> resultList = new ArrayList<>();

        final List<IO_Matchup> matchups = matchupData.getMatchups();
        for (final IO_Matchup io_matchup : matchups) {
            final Matchup matchup = createMatchup(io_matchup);
            resultList.add(matchup);
        }
        return resultList;
    }

    private static IO_RefObservation createIO_RefObs(IdGenerator idGenerator, MatchupData matchupData, Matchup matchup) {
        final ReferenceObservation refObs = matchup.getRefObs();
        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(idGenerator.next());
        io_refObs.setName(refObs.getName());
        io_refObs.setSensor(refObs.getSensor());
        final DataFile datafile = refObs.getDatafile();
        io_refObs.setFilePath(datafile.getPath());

        final int sensorId = addSensor(datafile.getSensor(), matchupData, idGenerator);
        io_refObs.setSensorId(sensorId);

        io_refObs.setRecordNo(refObs.getRecordNo());
        io_refObs.setTime(refObs.getTime());
        io_refObs.setTimeRadius(refObs.getTimeRadius());
        io_refObs.setLocation(refObs.getLocation().getValue());
        io_refObs.setPoint(refObs.getPoint().getValue());
        io_refObs.setDataset(refObs.getDataset());
        io_refObs.setReferenceFlag(refObs.getReferenceFlag());
        return io_refObs;
    }

    // package access for testing only tb 2014-11-24
    static int addSensor(Sensor sensor, MatchupData matchupData, IdGenerator idGenerator) {
        final List<Sensor> sensorList = matchupData.getSensors();
        for (final Sensor storedSensor : sensorList) {
            if (storedSensor.getName().equals(sensor.getName())
            && storedSensor.getPattern() == sensor.getPattern()
            && storedSensor.getObservationType().equals(sensor.getObservationType())) {
                return storedSensor.getId();
            }
        }

        sensor.setId(idGenerator.next());
        matchupData.add(sensor);

        return sensor.getId();
    }

    private static Matchup createMatchup(IO_Matchup io_matchup) {
        final Matchup matchup = new Matchup();
        matchup.setId(io_matchup.getId());
        matchup.setPattern(io_matchup.getPattern());
        matchup.setInvalid(io_matchup.isInvalid());
        return matchup;
    }

    private static IO_Matchup createIO_Matchup(Matchup matchup, IdGenerator idGenerator) {
        final IO_Matchup io_matchup = new IO_Matchup();
        io_matchup.setId(idGenerator.nextUnique());
        io_matchup.setPattern(matchup.getPattern());
        io_matchup.setInvalid(matchup.isInvalid());
        return io_matchup;
    }
}

/*


- connect sensor per Id with referenceObservation

- ReferenceObservation
-- SERIALIZE: create DataFile from filePath and sensorId fields
-- SERIALIZE: location to/from PGgeometry
 */