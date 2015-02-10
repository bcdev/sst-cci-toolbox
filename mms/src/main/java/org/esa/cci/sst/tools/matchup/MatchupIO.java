package org.esa.cci.sst.tools.matchup;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.postgis.PGgeometry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("deprecation")
public class MatchupIO {

    public static void write(List<Matchup> matchups, OutputStream outputStream, Configuration configuration,
                             PersistenceManager persistenceManager) throws IOException {
        final IdGenerator idGenerator = IdGenerator.create(configuration);
        final DetachHandler detachHandler = DetachHandlerFactory.create(configuration, persistenceManager);

        final MatchupData matchupData = map(matchups, idGenerator, detachHandler);
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
        final Set<Integer> observationIdSet = new HashSet<>();

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
                    if (!observationIdSet.contains(observationId)) {
                        final IO_Observation io_observation = createIO_Observation(matchupData, observationId,
                                                                                   insituObservation, detachHandler);
                        observationIdSet.add(observationId);
                        matchupData.addInsitu(io_observation);
                    }
                    io_coincidence.setIs(true);
                } else if (observation instanceof GlobalObservation) {
                    final GlobalObservation globalObservation = (GlobalObservation) observation;
                    if (!observationIdSet.contains(observationId)) {
                        final IO_Observation io_observation = createIO_Observation(matchupData, observationId,
                                                                                   globalObservation, detachHandler);
                        observationIdSet.add(observationId);
                        matchupData.addGlobal(io_observation);
                    }
                    io_coincidence.setGl(true);
                } else {
                    final RelatedObservation relatedObservation = (RelatedObservation) observation;
                    if (!observationIdSet.contains(observationId)) {
                        final IO_Observation io_observation = createIO_Observation(matchupData, observationId,
                                                                                   relatedObservation, detachHandler);
                        observationIdSet.add(observationId);
                        matchupData.addRelated(io_observation);
                    }
                }

                io_coincidence.setOi(observationId);
                io_coincidence.setTd(coincidence.getTimeDifference());
                io_matchup.add(io_coincidence);
            }
        }
        return matchupData;
    }

    // package access for testing only tb 2014-11-26
    static List<Matchup> restore(MatchupData matchupData) {
        final List<Matchup> resultList = new ArrayList<>();

        final List<IO_Matchup> matchups = matchupData.getMu();
        for (final IO_Matchup io_matchup : matchups) {
            final Matchup matchup = createMatchup(io_matchup);

            final ReferenceObservation refObs = getRefObs(io_matchup.getRi(), matchupData);
            matchup.setRefObs(refObs);

            final List<IO_Coincidence> io_coincidences = io_matchup.getCi();
            final List<Coincidence> coincidences = new ArrayList<>();
            for (final IO_Coincidence io_coincidence : io_coincidences) {
                final Coincidence coincidence = new Coincidence();
                coincidence.setTimeDifference(io_coincidence.getTd());
                Observation observation;
                if (io_coincidence.isIs()) {
                    observation = getInsituObservation(io_coincidence.getOi(), matchupData);
                } else if (io_coincidence.isGl()) {
                    observation = getGlobalObservation(io_coincidence.getOi(), matchupData);
                } else {
                    observation = getRelatedObservation(io_coincidence.getOi(), matchupData);
                }
                coincidence.setObservation(observation);
                coincidences.add(coincidence);
            }
            matchup.setCoincidences(coincidences);

            resultList.add(matchup);
        }
        return resultList;
    }

    private static IO_Observation createIO_Observation(MatchupData matchupData, int observationId,
                                                       RelatedObservation relatedObservation,
                                                       DetachHandler detachHandler) {
        final IO_Observation io_observation = new IO_Observation();
        io_observation.setId(observationId);
        io_observation.setNa(relatedObservation.getName());
        io_observation.setSe(relatedObservation.getSensor());
        final DataFile datafile = relatedObservation.getDatafile();
        io_observation.setFp(datafile.getPath());
        final int sensorId = addSensor(datafile.getSensor(), matchupData, detachHandler);
        io_observation.setSi(sensorId);
        io_observation.setRn(relatedObservation.getRecordNo());
        io_observation.setTi(relatedObservation.getTime());
        io_observation.setTr(relatedObservation.getTimeRadius());
        // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
        //io_observation.setLocation(relatedObservation.getLocation().getValue());

        return io_observation;
    }

    private static IO_Observation createIO_Observation(MatchupData matchupData, int observationId,
                                                       GlobalObservation globalObservation,
                                                       DetachHandler detachHandler) {
        final IO_Observation io_observation = new IO_Observation();
        io_observation.setId(observationId);
        io_observation.setNa(globalObservation.getName());
        io_observation.setSe(globalObservation.getSensor());
        final DataFile datafile = globalObservation.getDatafile();
        io_observation.setFp(datafile.getPath());
        final int sensorId = addSensor(datafile.getSensor(), matchupData, detachHandler);
        io_observation.setSi(sensorId);
        io_observation.setRn(globalObservation.getRecordNo());
        io_observation.setTi(globalObservation.getTime());
        io_observation.setTr(0);

        return io_observation;
    }

    private static Observation getRelatedObservation(int observationId, MatchupData matchupData) {
        final List<IO_Observation> relatedObservations = matchupData.getRlo();
        for (final IO_Observation observation : relatedObservations) {
            if (observation.getId() == observationId) {
                final RelatedObservation relatedObservation = new RelatedObservation();
                relatedObservation.setName(observation.getNa());
                relatedObservation.setSensor(observation.getSe());

                final DataFile dataFile = new DataFile();
                dataFile.setPath(observation.getFp());
                final Sensor sensor = getSensor(observation.getSi(), matchupData);
                dataFile.setSensor(sensor);
                relatedObservation.setDatafile(dataFile);

                relatedObservation.setTime(observation.getTi());
                relatedObservation.setTimeRadius(observation.getTr());

                // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
//                final PGgeometry location = createGeometry(observation.getLocation());
//                relatedObservation.setLocation(location);

                relatedObservation.setRecordNo(observation.getRn());
                return relatedObservation;
            }
        }
        throw new ToolException("RelatedObservation with id '" + observationId + "'not found",
                                ToolException.TOOL_INTERNAL_ERROR);
    }

    private static Observation getInsituObservation(int observationId, MatchupData matchupData) {
        final List<IO_Observation> insituObservations = matchupData.getIso();
        for (final IO_Observation observation : insituObservations) {
            if (observation.getId() == observationId) {
                final InsituObservation insituObservation = new InsituObservation();
                insituObservation.setName(observation.getNa());
                insituObservation.setSensor(observation.getSe());

                final DataFile dataFile = new DataFile();
                dataFile.setPath(observation.getFp());
                final Sensor sensor = getSensor(observation.getSi(), matchupData);
                dataFile.setSensor(sensor);
                insituObservation.setDatafile(dataFile);

                insituObservation.setTime(observation.getTi());
                insituObservation.setTimeRadius(observation.getTr());

                // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
//                final PGgeometry location = createGeometry(observation.getLocation());
//                relatedObservation.setLocation(location);

                insituObservation.setRecordNo(observation.getRn());
                return insituObservation;
            }
        }
        throw new ToolException("InsituObservation with id '" + observationId + "'not found",
                                ToolException.TOOL_INTERNAL_ERROR);
    }

    private static Observation getGlobalObservation(int observationId, MatchupData matchupData) {
        final List<IO_Observation> globalObservations = matchupData.getGlo();
        for (final IO_Observation observation : globalObservations) {
            if (observation.getId() == observationId) {
                final GlobalObservation globalObservation = new GlobalObservation();
                globalObservation.setName(observation.getNa());
                globalObservation.setSensor(observation.getSe());

                final DataFile dataFile = new DataFile();
                dataFile.setPath(observation.getFp());

                final Sensor sensor = getSensor(observation.getSi(), matchupData);
                dataFile.setSensor(sensor);

                globalObservation.setDatafile(dataFile);
                globalObservation.setTime(observation.getTi());
                globalObservation.setRecordNo(observation.getRn());

                return globalObservation;
            }
        }
        throw new ToolException("GlobalObservation with id '" + observationId + "'not found",
                                ToolException.TOOL_INTERNAL_ERROR);
    }

    private static ReferenceObservation getRefObs(int refObsId, MatchupData matchupData) {
        final List<IO_RefObservation> ioRefObsList = matchupData.getReo();
        for (final IO_RefObservation io_refobs : ioRefObsList) {
            if (io_refobs.getId() == refObsId) {
                final ReferenceObservation result = new ReferenceObservation();
                result.setId(refObsId);
                result.setName(io_refobs.getNa());
                result.setSensor(io_refobs.getSe());

                final DataFile dataFile = new DataFile();
                dataFile.setPath(io_refobs.getFp());
                final Sensor sensor = getSensor(io_refobs.getSi(), matchupData);
                dataFile.setSensor(sensor);
                result.setDatafile(dataFile);

                result.setRecordNo(io_refobs.getRn());
                result.setTime(io_refobs.getTi());
                result.setTimeRadius(io_refobs.getTr());

                // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
                // result.setLocation(createGeometry(io_refobs.getLocation()));
                result.setPoint(createGeometry(io_refobs.getPt()));

                result.setDataset(io_refobs.getDs());
                result.setReferenceFlag(io_refobs.getRf());

                return result;
            }
        }
        throw new ToolException("ReferenceObservation with id '" + refObsId + "'not found",
                                ToolException.TOOL_INTERNAL_ERROR);
    }

    private static Sensor getSensor(int sensorId, MatchupData matchupData) {
        final List<Sensor> sensors = matchupData.getSe();
        for (final Sensor io_sensor : sensors) {
            if (io_sensor.getId() == sensorId) {
                return io_sensor;
            }
        }
        throw new ToolException("Sensor with id '" + sensorId + "'not found", ToolException.TOOL_INTERNAL_ERROR);
    }

    private static IO_RefObservation createIO_RefObs(IdGenerator idGenerator, MatchupData matchupData, Matchup matchup,
                                                     DetachHandler detachHandler) {
        final ReferenceObservation refObs = matchup.getRefObs();
        final IO_RefObservation io_refObs = new IO_RefObservation();
        io_refObs.setId(idGenerator.next());
        io_refObs.setNa(refObs.getName());
        io_refObs.setSe(refObs.getSensor());
        final DataFile datafile = refObs.getDatafile();
        io_refObs.setFp(datafile.getPath());

        final int sensorId = addSensor(datafile.getSensor(), matchupData, detachHandler);
        io_refObs.setSi(sensorId);

        io_refObs.setRn(refObs.getRecordNo());
        io_refObs.setTi(refObs.getTime());
        io_refObs.setTr(refObs.getTimeRadius());
        // @todo 2 tb/tb temporarily removed due to memory issues 2014-12-17
        //io_refObs.setLocation(refObs.getLocation().getValue());
        io_refObs.setPt(refObs.getPoint().getValue());
        io_refObs.setDs(refObs.getDataset());
        io_refObs.setRf(refObs.getReferenceFlag());

        detachHandler.detach(refObs);
        matchup.setRefObs(null);

        return io_refObs;
    }

    // package access for testing only tb 2014-11-24
    static int addSensor(Sensor sensor, MatchupData matchupData, DetachHandler detachHandler) {
        final List<Sensor> sensorList = matchupData.getSe();
        for (final Sensor storedSensor : sensorList) {
            if (storedSensor.getName().equals(sensor.getName())) {
                return storedSensor.getId();
            }
        }

        matchupData.add(sensor);
        detachHandler.detach(sensor);

        return sensor.getId();
    }

    private static Matchup createMatchup(IO_Matchup io_matchup) {
        final Matchup matchup = new Matchup();
        matchup.setId(io_matchup.getId());
        matchup.setPattern(io_matchup.getPa());
        matchup.setInvalid(io_matchup.isIv());
        return matchup;
    }

    private static IO_Matchup createIO_Matchup(Matchup matchup, IdGenerator idGenerator) {
        final IO_Matchup io_matchup = new IO_Matchup();
        io_matchup.setId(idGenerator.nextUnique());
        io_matchup.setPa(matchup.getPattern());
        io_matchup.setIv(matchup.isInvalid());
        return io_matchup;
    }

    private static PGgeometry createGeometry(String wkt) {
        try {
            return new PGgeometry(wkt);
        } catch (SQLException e) {
            throw new ToolException("Error reading matchup-file, invalid WKT: " + wkt, e,
                                    ToolException.TOOL_INTERNAL_ERROR);
        }
    }
}

