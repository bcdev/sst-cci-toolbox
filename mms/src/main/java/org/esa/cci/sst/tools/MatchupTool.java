/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.tools;

import com.bc.ceres.core.Assert;
import org.esa.cci.sst.data.*;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.tools.samplepoint.TimeRange;
import org.esa.cci.sst.util.ConfigUtil;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.text.MessageFormat;
import java.util.*;

/**
 * Tool to compute multi-sensor match-ups from the MMS database.
 */
public class MatchupTool extends BasicTool {

    private static final String SENSOR_OBSERVATION_QUERY =
            "select o"
                    + " from ReferenceObservation o"
                    + " where o.sensor = ?1"
                    + " and o.time >= ?2 and o.time < ?3"
                    + " order by o.time, o.id";

    private static final String SINGLE_SENSOR_OBSERVATION_QUERY =
            "select o"
                    + " from ReferenceObservation o"
                    + " where o.sensor = ?1"
                    + " and o.time >= ?2 and o.time < ?3"
                    + " and not exists (select m from Matchup m"
                    + "                 where m.refObs = o)"
                    + " and not exists (select c from Coincidence c"
                    + "                 where c.observation = o)"
                    + " order by o.time, o.id";

    private static final String MATCHUPS_QUERY =
            "select m"
                    + " from Matchup m inner join m.refObs o"
                    + " where o.time >= ?1 and o.time < ?2"
                    + " order by o.time, m.id";

    private static final String MATCHUPS_FOR_PRIMARY_SENSOR_ONLY_QUERY =
            "select m"
                    + " from Matchup m inner join m.refObs o"
                    + " where o.time >= ?1 and o.time < ?2"
                    + " and o.sensor = ?3"
                    + " order by o.time, m.id";

    private static final String SECONDARY_OBSERVATION_QUERY =
            "select o from ReferenceObservation o"
                    + " where o.sensor = ?1"
                    + " and o.time >= ?2 and o.time < ?3"
                    + " and not exists (select c from Coincidence c"
                    + "                 where c.observation = o)"
                    + " order by o.time, o.id";

    private static final String COINCIDING_OBSERVATION_QUERY =
            "select o.id"
                    + " from mm_observation o"
                    + " where o.sensor = ?1"
                    + " and o.time >= timestamp ?2 - interval '12:00:00' and o.time < timestamp ?2 + interval '12:00:00'"
                    + " and st_intersects(o.location, st_geomfromewkt(?3))"
                    + " order by abs(extract(epoch from o.time) - extract(epoch from timestamp ?2))";

    private static final String COINCIDING_CALLSIGN_QUERY =
            "select o.id"
                    + " from mm_observation o"
                    + " where o.sensor = ?1"
                    + " and o.time >= timestamp ?2 - interval '12:00:00' and o.time < timestamp ?2 + interval '12:00:00'"
                    + " and o.name = ?4"
                    + " order by abs(extract(epoch from o.time) - extract(epoch from timestamp ?2))";

    private static final String COINCIDING_GLOBALOBS_QUERY =
            "select o.id"
                    + " from mm_observation o"
                    + " where o.sensor = ?1"
                    + " and o.time >= timestamp ?2 - interval '12:00:00' and o.time < timestamp ?2 + interval '12:00:00'"
                    + " order by abs(extract(epoch from o.time) - extract(epoch from timestamp ?2))";

    private static final String COINCIDING_INSITUOBS_QUERY =
            "select o.id"
                    + " from mm_observation o"
                    + " where o.sensor = ?1"
                    + " and o.name = ?4"
                    + " and abs(extract(epoch from o.time) - extract(epoch from timestamp ?2)) <= o.timeRadius"
                    + " order by abs(extract(epoch from o.time) - extract(epoch from timestamp ?2))";

    private static final String DUPLICATES_QUERY = "update mm_observation o set referenceflag = 5 " +
            "where o.sensor=?1 " +
            "and o.time >= ?2 and o.time < ?3 " +
            "and o.dataset != 6 and o.dataset != 7 " +
            "and exists ( select p.id from mm_observation p " +
            "where p.sensor = o.sensor and p.name = o.name " +
            "and p.time > o.time - interval '00:02:00' and p.time < o.time + interval '00:02:00' " +
            "and (p.timeradius < o.timeradius or (p.timeradius = o.timeradius and p.id < o.id)) )";

    private static final String DUPLICATES_DELETE_QUERY = "delete from mm_observation o " +
            "where o.sensor=?1 " +
            "and o.time >= ?2 and o.time < ?3 " +
            "and o.dataset != 6 and o.dataset != 7 " +
            "and exists ( select p.id from mm_observation p " +
            "where p.sensor = o.sensor and p.name = o.name " +
            "and p.time > o.time - interval '00:02:00' and p.time < o.time + interval '00:02:00' " +
            "and (p.timeradius < o.timeradius or (p.timeradius = o.timeradius and p.id < o.id)) )";

    private static final int CHUNK_SIZE = 1024; //*16;

    private static final String ATSR_MD = "atsr_md";
    private static final String METOP = "metop";
    private static final String SEVIRI = "seviri";
    private static final String AVHRR_MD = "avhrr_md";
    private static final Map<Class<? extends Observation>, String> OBSERVATION_QUERY_MAP = new HashMap<>(12);

    private Sensor atsrSensor;
    private Sensor metopSensor;
    private Sensor seviriSensor;
    private Sensor avhrrSensor;

    private List<Matchup> matchupAccu = new ArrayList<>();
    private List<Coincidence> coincidenceAccu = new ArrayList<>();

    static {
        OBSERVATION_QUERY_MAP.put(ReferenceObservation.class, COINCIDING_OBSERVATION_QUERY);
        OBSERVATION_QUERY_MAP.put(RelatedObservation.class, COINCIDING_OBSERVATION_QUERY);
        OBSERVATION_QUERY_MAP.put(GlobalObservation.class, COINCIDING_GLOBALOBS_QUERY);
        OBSERVATION_QUERY_MAP.put(InsituObservation.class, COINCIDING_INSITUOBS_QUERY);
    }

    private TimeRange timeRange;

    public static void main(String[] args) {
        final MatchupTool tool = new MatchupTool();
        try {
            if (!tool.setCommandLineArgs(args)) {
                return;
            }
            tool.initialize();
            tool.run();
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Exception e) {
            tool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.UNKNOWN_ERROR));
        } finally {
            tool.getPersistenceManager().close();
        }
    }

    private MatchupTool() {
        super("matchup-tool.sh", "0.1");
    }

    @Override
    public void initialize() {
        super.initialize();
        final Storage storage = getStorage();
        atsrSensor = storage.getSensor(ATSR_MD);
        metopSensor = storage.getSensor(METOP);
        seviriSensor = storage.getSensor(SEVIRI);
        avhrrSensor = storage.getSensor(AVHRR_MD);
        setTimeRange();
    }

    private void run() {
        final Configuration config = getConfig();

        if (config.getBooleanValue("mms.matchup.cleanup")) {
            cleanup();
        } else if (config.getBooleanValue("mms.matchup.cleanupinterval")) {
            cleanupInterval();
        }

        if (config.getBooleanValue("mms.matchup.markduplicates")) {
            markDuplicates();
        } else if (config.getBooleanValue("mms.matchup.dropduplicates")) {
            dropDuplicates();
        }

        if (config.getBooleanValue("mms.matchup.atsr_md")) {
            findAtsrMultiSensorMatchups();
        }

        if (config.getBooleanValue("mms.matchup.metop")) {
            findMetopMultiSensorMatchups();
        }

        if (configurationIndexOf("atsr_md") != -1) {
            findSingleSensorMatchups(ATSR_MD, atsrSensor);
        }

        if (configurationIndexOf("metop") != -1) {
            findSingleSensorMatchups(METOP, metopSensor);
        }

        if (configurationIndexOf("seviri") != -1) {
            findSingleSensorMatchups(SEVIRI, seviriSensor);
        }

        if (configurationIndexOf("avhrr_md") != -1) {
            findSingleSensorMatchups(AVHRR_MD, avhrrSensor);
        }

        final String primarySensor = config.getStringValue(Configuration.KEY_MMS_MATCHUP_PRIMARY_SENSOR);
        if (primarySensor != null) {
            getLogger().info("Primary sensor is " + primarySensor);
        }
        findRelatedObservations(primarySensor);
    }

    /**
     * Sets reference flag of observations to 5
     * if there are other observations for same sensor and call sign and satellite orbit
     * with lower temporal distance of in-situ measurement
     */
    private void markDuplicates() {
        try {
            getPersistenceManager().transaction();
            Query query = getPersistenceManager().createNativeQuery(DUPLICATES_QUERY);
            query.setParameter(2, timeRange.getStartDate());
            query.setParameter(3, timeRange.getStopDate());

            long time = System.currentTimeMillis();
            query.setParameter(1, ATSR_MD);
            query.executeUpdate();
            getLogger().info(MessageFormat.format("{0} duplicates determined in {1} ms.", ATSR_MD,
                    System.currentTimeMillis() - time));

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            time = System.currentTimeMillis();
            query.setParameter(1, METOP);
            query.executeUpdate();
            getLogger().info(MessageFormat.format("{0} duplicates determined in {1} ms.", METOP,
                    System.currentTimeMillis() - time));

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            time = System.currentTimeMillis();
            query.setParameter(1, SEVIRI);
            query.executeUpdate();
            getLogger().info(MessageFormat.format("{0} duplicates determined in {1} ms.", SEVIRI,
                    System.currentTimeMillis() - time));

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            time = System.currentTimeMillis();
            query.setParameter(1, AVHRR_MD);
            query.executeUpdate();
            getLogger().info(MessageFormat.format("{0} duplicates determined in {1} ms.", AVHRR_MD,
                    System.currentTimeMillis() - time));

            getPersistenceManager().commit();
        } catch (Exception e) {
            getPersistenceManager().rollback();
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    private void dropDuplicates() {
        try {
            getPersistenceManager().transaction();
            Query query = getPersistenceManager().createNativeQuery(DUPLICATES_DELETE_QUERY);
            query.setParameter(2, timeRange.getStartDate());
            query.setParameter(3, timeRange.getStopDate());

            long time = System.currentTimeMillis();
            query.setParameter(1, ATSR_MD);
            query.executeUpdate();
            getLogger().info(MessageFormat.format("{0} duplicates dropped in {1} ms.", ATSR_MD,
                    System.currentTimeMillis() - time));

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            time = System.currentTimeMillis();
            query.setParameter(1, METOP);
            query.executeUpdate();
            getLogger().info(MessageFormat.format("{0} duplicates dropped in {1} ms.", METOP,
                    System.currentTimeMillis() - time));

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            time = System.currentTimeMillis();
            query.setParameter(1, SEVIRI);
            query.executeUpdate();
            getLogger().info(MessageFormat.format("{0} duplicates dropped in {1} ms.", SEVIRI,
                    System.currentTimeMillis() - time));

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            time = System.currentTimeMillis();
            query.setParameter(1, AVHRR_MD);
            query.executeUpdate();
            getLogger().info(MessageFormat.format("{0} duplicates dropped in {1} ms.", AVHRR_MD,
                    System.currentTimeMillis() - time));

            getPersistenceManager().commit();
        } catch (Exception e) {
            getPersistenceManager().rollback();
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    private void setTimeRange() {
        timeRange = ConfigUtil.getTimeRange(Configuration.KEY_MMS_MATCHUP_START_TIME,
                Configuration.KEY_MMS_MATCHUP_STOP_TIME,
                getConfig());
    }

    /**
     * Loops over (A)ATSR reference observations and inquires METOP and SEVIRI
     * fulfilling the coincidence criterion by a spatio-temporal database query.
     * Creates matchups for the temporally nearest coincidences. Does the same
     * for METOP as reference and SEVIRI as inquired coincidence.
     */

    private void findAtsrMultiSensorMatchups() {
        try {
            getPersistenceManager().transaction();

            int count = 0;
            long time = System.currentTimeMillis();

            final Query query = createIncrementalQuery(ATSR_MD, SENSOR_OBSERVATION_QUERY);
            for (int cursor = 0; ; ) {
                final List<ReferenceObservation> atsrObservations = query.setFirstResult(cursor).getResultList();
                if (atsrObservations.size() == 0) {
                    break;
                }
                cursor += atsrObservations.size();

                for (final ReferenceObservation atsrObservation : atsrObservations) {
                    //System.out.println(TimeUtil.formatCcsdsUtcMillisFormat(new Date()) + " " + atsrObservation.getId() + " ...");
                    Matchup matchup = null;

                    // determine corresponding metop observation if any
                    if (metopSensor != null) {
                        final Observation metopObservation = findCoincidingObservation(atsrObservation, metopSensor);
                        if (metopObservation != null) {
                            matchup = createMatchup(atsrObservation,
                                    atsrSensor.getPattern() | metopSensor.getPattern());
                            final Coincidence coincidence = createCoincidence(matchup, metopObservation);
                            coincidenceAccu.add(coincidence);
                        }
                    }

                    // determine corresponding seviri observation if any
                    if (seviriSensor != null) {
                        final Observation seviriObservation = findCoincidingObservation(atsrObservation, seviriSensor);
                        if (seviriObservation != null) {
                            if (matchup == null) {
                                matchup = createMatchup(atsrObservation,
                                        atsrSensor.getPattern() | seviriSensor.getPattern());
                            } else {
                                matchup.setPattern(matchup.getPattern() | seviriSensor.getPattern());
                            }
                            final Coincidence coincidence = createCoincidence(matchup, seviriObservation);
                            coincidenceAccu.add(coincidence);
                        }
                    }

                    // determine corresponding avhrr_md observation if any
                    if (avhrrSensor != null) {
                        final Observation avhrrObservation = findCoincidingCallsign(atsrObservation, avhrrSensor);
                        if (avhrrObservation != null) {
                            if (matchup == null) {
                                matchup = createMatchup(atsrObservation,
                                        atsrSensor.getPattern() | avhrrSensor.getPattern());
                            } else {
                                matchup.setPattern(matchup.getPattern() | avhrrSensor.getPattern());
                            }
                            final Coincidence coincidence = createCoincidence(matchup, avhrrObservation);
                            coincidenceAccu.add(coincidence);
                        }
                    }

                    ++count;
                    if (count % 1024 == 0) {
                        getPersistenceManager().commit();
                        getPersistenceManager().transaction();
                        getLogger().info(MessageFormat.format("{0} {1} processed in {2} ms.",
                                count,
                                atsrSensor.getName(),
                                System.currentTimeMillis() - time));
                        time = System.currentTimeMillis();
                    }
                }
            }

            getPersistenceManager().commit();
            getPersistenceManager().transaction();
            getLogger().info(MessageFormat.format("{0} {1} processed in {2} ms.",
                    count,
                    atsrSensor.getName(),
                    System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            for (Matchup m : matchupAccu) {
                getPersistenceManager().persist(m);
            }
            for (Coincidence c : coincidenceAccu) {
                getPersistenceManager().persist(c);
            }
            getLogger().info(MessageFormat.format("{0} matchups and {1} coincidences stored in {2} ms.",
                    matchupAccu.size(),
                    coincidenceAccu.size(),
                    System.currentTimeMillis() - time));
            matchupAccu.clear();
            coincidenceAccu.clear();

            getPersistenceManager().commit();
        } catch (Exception e) {
            getPersistenceManager().rollback();
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    /**
     * Loops over METOP reference observations and inquires SEVIRI
     * fulfilling the coincidence criterion by a spatio-temporal database query.
     * Creates matchups for the temporally nearest coincidences.
     * (currently, there is no temporal overlap of AVHRR and METOP.)
     */
    private void findMetopMultiSensorMatchups() {
        if (seviriSensor == null) {
            return;
        }
        try {
            getPersistenceManager().transaction();

            int count = 0;
            long time = System.currentTimeMillis();
            final Query query = createIncrementalQuery(METOP, SECONDARY_OBSERVATION_QUERY);
            for (int cursor = 0; ; ) {
                final List<ReferenceObservation> metopObservations = query.setFirstResult(cursor).getResultList();
                if (metopObservations.isEmpty()) {
                    break;
                }
                cursor += metopObservations.size();

                for (final ReferenceObservation metopObservation : metopObservations) {
                    // determine corresponding seviri observation if any
                    if (seviriSensor != null) {
                        final Observation seviriObservation = findCoincidingObservation(metopObservation, seviriSensor);
                        if (seviriObservation != null) {
                            final Matchup matchup = createMatchup(metopObservation,
                                    metopSensor.getPattern() | seviriSensor.getPattern());
                            final Coincidence coincidence = createCoincidence(matchup, seviriObservation);
                            coincidenceAccu.add(coincidence);
                        }
                    }
                    ++count;
                    if (count % 1024 == 0) {
                        getPersistenceManager().commit();
                        getPersistenceManager().transaction();
                        getLogger().info(MessageFormat.format("{0} {1} processed in {2} ms.",
                                count,
                                metopSensor.getName(),
                                System.currentTimeMillis() - time));
                        time = System.currentTimeMillis();
                    }
                }
            }

            getPersistenceManager().commit();
            getPersistenceManager().transaction();
            getLogger().info(MessageFormat.format("{0} {1} processed in {2} ms.",
                    count,
                    metopSensor.getName(),
                    System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            for (Matchup m : matchupAccu) {
                getPersistenceManager().persist(m);
            }
            for (Coincidence c : coincidenceAccu) {
                getPersistenceManager().persist(c);
            }
            getLogger().info(MessageFormat.format("{0} matchups and {1} coincidences stored in {2} ms.",
                    matchupAccu.size(),
                    coincidenceAccu.size(),
                    System.currentTimeMillis() - time));
            matchupAccu.clear();
            coincidenceAccu.clear();

            getPersistenceManager().commit();
        } catch (Exception e) {
            getPersistenceManager().rollback();
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    private void findSingleSensorMatchups(String sensorName, Sensor sensor) {
        try {
            getPersistenceManager().transaction();

            int count = 0;
            long time = System.currentTimeMillis();
            final Query query = createIncrementalQuery(sensorName, SINGLE_SENSOR_OBSERVATION_QUERY);
            for (int cursor = 0; ; ) {
                final List<ReferenceObservation> observations = query.setFirstResult(cursor).getResultList();
                if (observations.isEmpty()) {
                    break;
                }
                cursor += observations.size();

                for (final ReferenceObservation observation : observations) {
                    createMatchup(observation, sensor.getPattern());
                    ++count;
                    if (count % 1024 == 0) {
                        getPersistenceManager().commit();
                        getPersistenceManager().transaction();
                        getLogger().info(MessageFormat.format("{0} {1} processed in {2} ms.",
                                count,
                                sensor.getName(),
                                System.currentTimeMillis() - time));
                        time = System.currentTimeMillis();
                    }
                }
            }

            getPersistenceManager().commit();
            getPersistenceManager().transaction();
            getLogger().info(MessageFormat.format("{0} {1} processed in {2} ms.",
                    count,
                    sensor.getName(),
                    System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            for (Matchup m : matchupAccu) {
                getPersistenceManager().persist(m);
            }
            getLogger().info(MessageFormat.format("{0} matchups stored in {1} ms.",
                                                  matchupAccu.size(),
                                                  System.currentTimeMillis() - time));
            matchupAccu.clear();

            getPersistenceManager().commit();
        } catch (Exception e) {
            getPersistenceManager().rollback();
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    private void findRelatedObservations(String primarySensor) {
        try {
            getPersistenceManager().transaction();
            List<String> sensorNames = determineRelatedSensorNames();
            final Query query;
            if (primarySensor != null) {
                query = getPersistenceManager().createQuery(MATCHUPS_FOR_PRIMARY_SENSOR_ONLY_QUERY);
            } else {
                query = getPersistenceManager().createQuery(MATCHUPS_QUERY);
            }
            long time = System.currentTimeMillis();

            long chunkSizeMillis = 3600 * 1000;
            final long startTime = timeRange.getStartDate().getTime();
            final long stopTime = timeRange.getStopDate().getTime();
            long chunkStartTime = startTime;
            while (chunkStartTime < stopTime) {
                long chunkStopTime = chunkStartTime + chunkSizeMillis;
                if (chunkStopTime > stopTime) {
                    chunkStopTime = stopTime;
                }
                query.setParameter(1, new Date(chunkStartTime));
                query.setParameter(2, new Date(chunkStopTime));
                if (primarySensor != null) {
                    query.setParameter(3, primarySensor);
                }
                final List<Matchup> matchups = query.getResultList();

                for (String sensorName : sensorNames) {
                    final Sensor sensor = getStorage().getSensor(sensorName);
                    final Class<? extends Observation> observationClass = getObservationClass(sensor);
                    final String queryString = OBSERVATION_QUERY_MAP.get(observationClass);
                    for (final Matchup matchup : matchups) {
                        addCoincidence(matchup, sensorName, queryString, sensor.getPattern(), observationClass);
                    }
                    getLogger().info(MessageFormat.format("{0} {1} up to {2} processed in {3} ms.",
                            matchups.size(),
                            sensorName,
                            TimeUtil.formatCcsdsUtcFormat(new Date(chunkStopTime)),
                            System.currentTimeMillis() - time));
                    time = System.currentTimeMillis();
                }
                getPersistenceManager().commit();
                getPersistenceManager().transaction();
                chunkStartTime = chunkStopTime;
                if (matchups.size() > 2048) {
                    chunkSizeMillis = chunkSizeMillis / 2;
                } else if (matchups.size() < 512) {
                    chunkSizeMillis = chunkSizeMillis * 2;
                }
            }

            for (Coincidence c : coincidenceAccu) {
                getPersistenceManager().persist(c);
            }
            getLogger().info(MessageFormat.format("{0} coincidences stored in {1} ms.",
                    coincidenceAccu.size(),
                    System.currentTimeMillis() - time));
            coincidenceAccu.clear();

            getPersistenceManager().commit();
        } catch (Exception e) {
            getPersistenceManager().rollback();
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    private List<String> determineRelatedSensorNames() {
        List<String> sensorNames = new ArrayList<>();
        final Configuration config = getConfig();
        for (int i = 0; i < 100; i++) {
            final String sensorKey = String.format("mms.matchup.%d.sensor", i);
            final String sensorName = config.getStringValue(sensorKey, null);
            if (sensorName == null) {
                continue;
            }
            final Sensor sensor = getStorage().getSensor(sensorName);
            if (sensor == null) {
                continue;
            }
            final Class<? extends Observation> observationClass = getObservationClass(sensor);
            if (observationClass == ReferenceObservation.class) {
                continue;
            }
            final String queryString = OBSERVATION_QUERY_MAP.get(observationClass);
            if (queryString == null) {
                final String message = MessageFormat.format("No query for observation type ''{0}''",
                        observationClass.getSimpleName());
                throw new ToolException(message, ToolException.TOOL_CONFIGURATION_ERROR);
            }
            if (!sensorNames.contains(sensorName)) {
                sensorNames.add(sensorName);
            }
        }
        return sensorNames;
    }

    private int configurationIndexOf(String sensor) {
        final Configuration config = getConfig();
        for (int i = 0; i < 100; i++) {
            final String sensorKey = String.format("mms.matchup.%d.sensor", i);
            final String sensorName = config.getStringValue(sensorKey, null);
            if (sensor.equals(sensorName)) {
                return i;
            }
        }
        return -1;
    }

    private Query createIncrementalQuery(String sensorName, String queryString) {
        final Query query = getPersistenceManager().createQuery(queryString);
        query.setParameter(1, sensorName);
        query.setParameter(2, timeRange.getStartDate());
        query.setParameter(3, timeRange.getStopDate());
        query.setMaxResults(CHUNK_SIZE);
        return query;
    }

    @SuppressWarnings({"unchecked"})
    private static Class<? extends Observation> getObservationClass(Sensor sensor) {
        try {
            return (Class<? extends Observation>) Class.forName(
                    String.format("%s.%s", Sensor.class.getPackage().getName(), sensor.getObservationType()));
        } catch (ClassCastException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private void addCoincidence(Matchup matchup, String sensorName, String queryString,
                                long pattern, Class<? extends Observation> observationClass) {
        if (coincidenceAlreadyExists(matchup, sensorName)) {
            return;
        }
        final ReferenceObservation refObs = matchup.getRefObs();
        final Observation sensorObs = findCoincidingObservation(refObs, queryString, observationClass, sensorName);
        if (sensorObs != null) {
            final Coincidence coincidence = createCoincidence(matchup, sensorObs);
            coincidenceAccu.add(coincidence);
            matchup.setPattern(matchup.getPattern() | pattern);
        }
    }

    static boolean coincidenceAlreadyExists(Matchup matchup, String sensorName) {
        final List<Coincidence> coincidences = matchup.getCoincidences();
        for (Coincidence coincidence : coincidences) {
            if (coincidence.getObservation().getSensor().equals(sensorName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines temporally nearest common observation of a specific sensor
     * for a reference observation.
     *
     * @param refObs The reference observation.
     * @param sensor The sensor.
     * @return common observation of sensor with coincidence to reference observation
     * that is temporally closest to reference observation, null if no
     * observation of the specified sensor has a coincidence with the
     * reference observation
     */
    private Observation findCoincidingObservation(ReferenceObservation refObs, Sensor sensor) {
        final Class<? extends Observation> observationClass = getObservationClass(sensor);
        return findCoincidingObservation(refObs, COINCIDING_OBSERVATION_QUERY, observationClass, sensor.getName());
    }

    private Observation findCoincidingCallsign(ReferenceObservation refObs, Sensor sensor) {
        final Class<? extends Observation> observationClass = getObservationClass(sensor);
        return findCoincidingObservation(refObs, COINCIDING_CALLSIGN_QUERY, observationClass, sensor.getName());
    }

    private Observation findCoincidingObservation(ReferenceObservation refObs, String queryString,
                                                  Class observationClass, String sensorName) {
        // since binding a date to a parameter failed ...
        final String queryString2 = queryString.replaceAll("\\?2",
                "'" + TimeUtil.formatCcsdsUtcFormat(refObs.getTime()) + "'");
        final Query query = getPersistenceManager().createNativeQuery(queryString2, observationClass);
        query.setParameter(1, sensorName);
        //query.setParameter("time", refObs.getTime(), TemporalType.TIMESTAMP);
        query.setParameter(3, refObs.getPoint().toString());
        query.setParameter(4, refObs.getName());
        query.setMaxResults(1);
        try {
            @SuppressWarnings({"unchecked"})
            final List<? extends Observation> observations = query.getResultList();
            if (!observations.isEmpty()) {
                // select temporally nearest common observation
                return observations.get(0);
            } else {
                return null;
            }
        } catch (PersistenceException e) {
            if (e.getMessage().startsWith("ERROR: BOOM! Could not generate outside point!")) {
                getLogger().warning(
                        "skipping chunk up to " + sensorName + " for matchup " + refObs.getId() + ": " + e.getMessage());
                try {
                    //getPersistenceManager().rollback();
                    getPersistenceManager().commit();
                } catch (Exception _) {
                    // ignore
                }
                getPersistenceManager().transaction();
                return null;
            } else {
                throw e;
            }
        }
    }


    /**
     * Factory method to create matchup for a reference observation.
     *
     * @param referenceObservation the reference observation constituting the matchup
     * @param pattern
     * @return the new Matchup for the reference observation
     */
    private Matchup createMatchup(ReferenceObservation referenceObservation, long pattern) {
        final Matchup matchup = new Matchup();
        matchup.setId(referenceObservation.getId());
        matchup.setRefObs(referenceObservation);
        matchup.setPattern(pattern);
        matchupAccu.add(matchup);
        return matchup;
    }

    /**
     * Creates Coincidence between a matchup and a common observation,
     * determines temporal and spatial distance by a database query.
     *
     * @param matchup     the matchup with the reference observation
     * @param observation the common observation that has a coincidence with
     *                    the reference and is temporally closest to it
     * @return newly created Coincidence relating matchup and common observation
     */
    static Coincidence createCoincidence(Matchup matchup, Observation observation) {
        Assert.argument(observation instanceof Timeable, "!(observation instanceof Timeable)");
        final Date matchupTime = matchup.getRefObs().getTime();
        final Date observationTime = ((Timeable) observation).getTime();
        final double timeDifference = TimeUtil.getTimeDifferenceInSeconds(matchupTime, observationTime);
        final Coincidence coincidence = new Coincidence();

        coincidence.setMatchup(matchup);
        coincidence.setObservation(observation);
        coincidence.setTimeDifference(timeDifference);
        return coincidence;
    }

    private void cleanup() {
        getPersistenceManager().transaction();

        Query delete = getPersistenceManager().createQuery("delete from Coincidence c");
        delete.executeUpdate();
        delete = getPersistenceManager().createQuery("delete from Matchup m");
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

    private void cleanupInterval() {
        getPersistenceManager().transaction();

        final Date startDate = timeRange.getStartDate();
        final Date stopDate = timeRange.getStopDate();
        Query delete = getPersistenceManager().createNativeQuery(
                "delete from mm_coincidence c where exists ( select r.id from mm_observation r where c.matchup_id = r.id and r.time >= ?1 and r.time < ?2 )");
        delete.setParameter(1, startDate);
        delete.setParameter(2, stopDate);
        delete.executeUpdate();
        delete = getPersistenceManager().createNativeQuery(
                "delete from mm_matchup m where exists ( select r from mm_observation r where m.refobs_id = r.id and r.time >= ?1 and r.time < ?2 )");
        delete.setParameter(1, startDate);
        delete.setParameter(2, stopDate);
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

}
