package org.esa.cci.sst.tools.matchup;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MatchupIO {

    public static void write(MatchupData matchupData, OutputStream outputStream) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(outputStream, matchupData);
    }

    public static MatchupData read(ByteArrayInputStream inputStream) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputStream, MatchupData.class);
    }
}

/*
- Inject IDGenerator class to create unique IDs for Matchups
- internal ID generator to generate file-unique id's to resolve references later.
- generate serializable classes from the classes passed in

- add Sensor list
-- connect sensor per Id with referenceObservation

- ReferenceObservation
-- SERIALIZE: create DataFile from filePath and sensorId fields
-- SERIALIZE: location to/from PGgeometry
 */