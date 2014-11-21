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
        return new MatchupData();
    }

    public static List<Matchup> restore(MatchupData matchupData) {
        return new ArrayList<>();
    }
}

/*
- Inject IDGenerator class to create unique IDs for Matchups
- generate serializable classes from the classes passed in

- connect sensor per Id with referenceObservation

- ReferenceObservation
-- SERIALIZE: create DataFile from filePath and sensorId fields
-- SERIALIZE: location to/from PGgeometry
 */