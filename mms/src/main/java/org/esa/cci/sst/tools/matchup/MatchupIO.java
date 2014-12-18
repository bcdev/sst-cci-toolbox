package org.esa.cci.sst.tools.matchup;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.cci.sst.data.*;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.postgis.PGgeometry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class MatchupIO {

    public static void write(List<Matchup> matchups, OutputStream outputStream, Configuration configuration) throws IOException {
        final IdGenerator idGenerator = IdGenerator.create(configuration);

        // @todo 1 tb/tb implement factory for detachhadler, read config parameter, invent means to ingest peristenceManager 2014-12-18
        final MatchupData matchupData = map(matchups, idGenerator, new NoDetachHandler());
        writeMapped(matchupData, outputStream);
    }

    public static List<Matchup> read(InputStream inputStream) throws IOException {
        final MatchupData matchupData = readMapped(inputStream);

        return restore(matchupData);
    }

    // package access for testing only tb 2014-11-21
    static void writeMapped(MatchupData matchupData, OutputStream outputStream) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(outputStream, matchupData);
    }

    // package access for testing only tb 2014-11-21
    static MatchupData readMapped(InputStream inputStream) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputStream, MatchupData.class);
    }

    @SuppressWarnings("InstanceofInterfaces")
    // package access for testing only tb 2014-11-26
    static MatchupData map(List<Matchup> matchups, IdGenerator idGenerator, DetachHandler detachHandler) {
        final MatchupData matchupData = new MatchupData();

        for (final Matchup matchup : matchups) {
            final IO_Matchup io_matchup = createIO_Matchup(matchup, idGenerator);
            matchupData.add(io_matchup);

            final IO_RefObservation io_refObs = createIO_RefObs(idGenerator, matchupData, matchup, detachHandler);
            matchupData.add(io_refObs);

            final List<Coincidence> coincidences = matchup.getCoincidences();
            for (final Coincidence coincidence : coincidences) {
                final IO_Coincidence io_coincidence = new IO_Coincidence();

                final Observation observation = coincidence.getObservation();
                final int observationId = observation.getId();
                if (observation instanceof InsituObservation) {
                    final InsituObservation insituObservation = (InsituObservation) observation;
                    final IO_Observation io_observation = createIO_Observation(matchupData, observationId, insituObservation);
                    matchupData.addInsitu(io_observation);
                    io_coincidence.setInsitu(true);
                } else {
                    final RelatedObservation relatedObservation = (RelatedObservation) observation;
                    final IO_Observation io_observation = createIO_Observation(matchupData, observationId, relatedObservation);
                    matchupData.addRelated(io_observation);
                }
                io_coincidence.setObservationId(observationId);
                io_coincidence.setTimeDifference(coincidence.getTimeDifference());
                io_matchup.add(io_coincidence);
            }

            detachHandler.detach(matchup);
        }
        return matchupData;
    }

    // package access for testing only tb 2014-11-26
    static List<Matchup> restore(MatchupData matchupData) {
        final List<Matchup> resultList = new ArrayList<>();

        final List<IO_Matchup> matchups = matchupData.getMatchups();
        for (final IO_Matchup io_matchup : matchups) {
            final Matchup matchup = createMatchup(io_matchup);

            final ReferenceObservation refoObs = getRefObs(io_matchup.getRefObsId(), matchupData);
            matchup.setRefObs(refoObs);

            final List<IO_Coincidence> io_coincidences = io_matchup.getCoincidences();
            final List<Coincidence> coincidences = new ArrayList<>();
            for (final IO_Coincidence io_coincidence : io_coincidences) {
                final Coincidence coincidence = new Coincidence();
                coincidence.setTimeDifference(io_coincidence.getTimeDifference());
                Observation observation;
                if (io_coincidence.isInsitu()) {
                    observation = getInsituObservation(io_coincidence.getObservationId(), matchupData);
                } else {
                    observation = getRelatedObservation(io_coincidence.getObservationId(), matchupData);
                }
                coincidence.setObservation(observation);
                coincidences.add(coincidence);
            }
            matchup.setCoincidences(coincidences);

            resultList.add(matchup);
        }
        return resultList;
    }

    private static IO_Observation createIO_Observation(MatchupData matchupData, int observationId, RelatedObservation relatedObservation) {
        final IO_Observation io_observation = new IO_Observation();
        io_observation.setId(observationId);
        io_observation.setName(relatedObservation.getName());
        io_observation.setSensor(relatedObservation.getSensor());
        final DataFile datafile = relatedObservation.getDatafile();
        io_observation.setFilePath(datafile.getPath());
        final int sensorId = addSensor(datafile.getSensor(), matchupData);
        io_observation.setSensorId(sensorId);
        io_observation.setRecordNo(relatedObservation.getRecordNo());
        io_observation.setTime(relatedObservation.getTime());
        io_observation.setTimeRadius(relatedObservation.getTimeRadius());
        // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
        //io_observation.setLocation(relatedObservation.getLocation().getValue());
        return io_observation;
    }

    private static Observation getRelatedObservation(int observationId, MatchupData matchupData) {
        final List<IO_Observation> relatedObservations = matchupData.getRelatedObservations();
        for (final IO_Observation observation : relatedObservations) {
            if (observation.getId() == observationId) {
                final RelatedObservation relatedObservation = new RelatedObservation();
                relatedObservation.setName(observation.getName());
                relatedObservation.setSensor(observation.getSensor());

                final DataFile dataFile = new DataFile();
                dataFile.setPath(observation.getFilePath());
                final Sensor sensor = getSensor(observation.getSensorId(), matchupData);
                dataFile.setSensor(sensor);
                relatedObservation.setDatafile(dataFile);

                relatedObservation.setTime(observation.getTime());
                relatedObservation.setTimeRadius(observation.getTimeRadius());

                // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
//                final PGgeometry location = createGeometry(observation.getLocation());
//                relatedObservation.setLocation(location);

                relatedObservation.setRecordNo(observation.getRecordNo());
                return relatedObservation;
            }
        }
        throw new ToolException("RelatedObservation with id '" + observationId + "'not found", ToolException.TOOL_INTERNAL_ERROR);
    }

    private static Observation getInsituObservation(int observationId, MatchupData matchupData) {
        final List<IO_Observation> insituObservations = matchupData.getInsituObservations();
        for (final IO_Observation observation : insituObservations) {
            if (observation.getId() == observationId) {
                final InsituObservation relatedObservation = new InsituObservation();
                relatedObservation.setName(observation.getName());
                relatedObservation.setSensor(observation.getSensor());

                final DataFile dataFile = new DataFile();
                dataFile.setPath(observation.getFilePath());
                final Sensor sensor = getSensor(observation.getSensorId(), matchupData);
                dataFile.setSensor(sensor);
                relatedObservation.setDatafile(dataFile);

                relatedObservation.setTime(observation.getTime());
                relatedObservation.setTimeRadius(observation.getTimeRadius());

                // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
//                final PGgeometry location = createGeometry(observation.getLocation());
//                relatedObservation.setLocation(location);

                relatedObservation.setRecordNo(observation.getRecordNo());
                return relatedObservation;
            }
        }
        throw new ToolException("RelatedObservation with id '" + observationId + "'not found", ToolException.TOOL_INTERNAL_ERROR);
    }

    private static ReferenceObservation getRefObs(int refObsId, MatchupData matchupData) {
        final List<IO_RefObservation> ioRefObsList = matchupData.getReferenceObservations();
        for (final IO_RefObservation io_refobs : ioRefObsList) {
            if (io_refobs.getId() == refObsId) {
                final ReferenceObservation result = new ReferenceObservation();
                result.setId(refObsId);
                result.setName(io_refobs.getName());
                result.setSensor(io_refobs.getSensor());

                final DataFile dataFile = new DataFile();
                dataFile.setPath(io_refobs.getFilePath());
                final Sensor sensor = getSensor(io_refobs.getSensorId(), matchupData);
                dataFile.setSensor(sensor);
                result.setDatafile(dataFile);

                result.setRecordNo(io_refobs.getRecordNo());
                result.setTime(io_refobs.getTime());
                result.setTimeRadius(io_refobs.getTimeRadius());

                // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
                // result.setLocation(createGeometry(io_refobs.getLocation()));
                result.setPoint(createGeometry(io_refobs.getPoint()));

                result.setDataset(io_refobs.getDataset());
                result.setReferenceFlag(io_refobs.getReferenceFlag());

                return result;
            }
        }
        throw new ToolException("ReferenceObservation with id '" + refObsId + "'not found", ToolException.TOOL_INTERNAL_ERROR);
    }

    private static Sensor getSensor(int sensorId, MatchupData matchupData) {
        final List<Sensor> sensors = matchupData.getSensors();
        for (final Sensor io_sensor : sensors) {
            if (io_sensor.getId() == sensorId) {
                return io_sensor;
            }
        }
        throw new ToolException("Sensor with id '" + sensorId + "'not found", ToolException.TOOL_INTERNAL_ERROR);
    }

    private static IO_RefObservation createIO_RefObs(IdGenerator idGenerator, MatchupData matchupData, Matchup matchup, DetachHandler detachHandler) {
        final ReferenceObservation refObs = matchup.getRefObs();
        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(idGenerator.next());
        io_refObs.setName(refObs.getName());
        io_refObs.setSensor(refObs.getSensor());
        final DataFile datafile = refObs.getDatafile();
        io_refObs.setFilePath(datafile.getPath());

        final int sensorId = addSensor(datafile.getSensor(), matchupData);
        io_refObs.setSensorId(sensorId);

        io_refObs.setRecordNo(refObs.getRecordNo());
        io_refObs.setTime(refObs.getTime());
        io_refObs.setTimeRadius(refObs.getTimeRadius());
        // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
        //io_refObs.setLocation(refObs.getLocation().getValue());
        io_refObs.setPoint(refObs.getPoint().getValue());
        io_refObs.setDataset(refObs.getDataset());
        io_refObs.setReferenceFlag(refObs.getReferenceFlag());

        detachHandler.detach(refObs);
        return io_refObs;
    }

    // package access for testing only tb 2014-11-24
    static int addSensor(Sensor sensor, MatchupData matchupData) {
        final List<Sensor> sensorList = matchupData.getSensors();
        for (final Sensor storedSensor : sensorList) {
            if (storedSensor.getName().equals(sensor.getName())
                    && storedSensor.getPattern() == sensor.getPattern()
                    && storedSensor.getObservationType().equals(sensor.getObservationType())) {
                return storedSensor.getId();
            }
        }

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

    private static PGgeometry createGeometry(String wkt) {
        try {
            return new PGgeometry(wkt);
        } catch (SQLException e) {
            throw new ToolException("Error reading matchup-file, invalid WKT: " + wkt, e, ToolException.TOOL_INTERNAL_ERROR);
        }
    }
}

