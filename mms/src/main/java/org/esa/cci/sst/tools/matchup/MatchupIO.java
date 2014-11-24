package org.esa.cci.sst.tools.matchup;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.cci.sst.data.Matchup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
            final IO_Matchup io_matchup = createIO_Matchup(idGenerator, matchup);
            matchupData.add(io_matchup);
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

    private static Matchup createMatchup(IO_Matchup io_matchup) {
        final Matchup matchup = new Matchup();
        matchup.setId(io_matchup.getId());
        matchup.setPattern(io_matchup.getPattern());
        matchup.setInvalid(io_matchup.isInvalid());
        return matchup;
    }

    private static IO_Matchup createIO_Matchup(IdGenerator idGenerator, Matchup matchup) {
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