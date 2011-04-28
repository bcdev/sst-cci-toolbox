package org.esa.cci.sst.tools;

import com.bc.ceres.core.Assert;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.Timed;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.esa.cci.sst.tools.SensorType.*;

/**
 * Tool to compute multi-sensor match-ups from the MMS database.
 */
public class MatchupTool extends MmsTool {

    private static final String SENSOR_OBSERVATION_QUERY =
            "select o"
            + " from ReferenceObservation o"
            + " where o.sensor = ?1"
            + " order by o.time";

    private static final String SINGLE_SENSOR_OBSERVATION_QUERY =
            "select o"
            + " from ReferenceObservation o"
            + " where o.sensor = ?1"
            + " and not exists (select m from Matchup m"
            + "                 where m.refObs = o)"
            + " and not exists (select c from Coincidence c"
            + "                 where c.observation = o)"
            + " order by o.time";

    private static final String ALL_MATCHUPS_QUERY =
            "select m"
            + " from Matchup m"
            + " order by m.id";

    private static final String SECONDARY_OBSERVATION_QUERY =
            "select o from ReferenceObservation o"
            + " where o.sensor = ?1"
            + " and not exists (select c from Coincidence c"
            + "                 where c.observation = o)"
            + " order by o.time";

    private static final String COINCIDING_OBSERVATION_QUERY =
            "select o.id"
            + " from mm_observation o, mm_observation oref"
            + " where oref.id = ?"
            + " and o.sensor = ?"
            + " and o.time >= oref.time - '12:00:00' and o.time < oref.time + '12:00:00'"
            + " and st_intersects(o.location, oref.point)"
            + " order by abs(extract(epoch from o.time) - extract(epoch from oref.time))";

    private static final String COINCIDING_GLOBALOBS_QUERY =
            "select o.id"
            + " from mm_observation o, mm_observation oref"
            + " where oref.id = ?"
            + " and o.sensor = ?"
            + " and o.time >= oref.time - '12:00:00' and o.time < oref.time + '12:00:00'"
            + " order by abs(extract(epoch from o.time) - extract(epoch from oref.time))";

    private static final String COINCIDING_INSITUOBS_QUERY =
            "select o.id"
            + " from mm_observation o, mm_observation oref"
            + " where oref.id = ?"
            + " and o.sensor = ?"
            + " and o.name = oref.name"
            + " and abs(extract(epoch from o.time) - extract(epoch from oref.time)) <= o.timeRadius"
            + " order by abs(extract(epoch from o.time) - extract(epoch from oref.time))";

    private static final String DISTANCE_QUERY =
            "select abs(extract(epoch from o1.time) - extract(epoch from o2.time)), st_distance(o1.point,o2.location)"
            + " from mm_observation o1, mm_observation o2"
            + " where o1.id = ?"
            + " and o2.id = ?";

    private static final String TEMPORAL_DISTANCE_QUERY =
            "select abs(extract(epoch from o1.time) - extract(epoch from o2.time))"
            + " from mm_observation o1, mm_observation o2"
            + " where o1.id = ?"
            + " and o2.id = ?";

    private static final Map<SensorType, String> QUERY_MAP = new HashMap<SensorType, String>(12);

    static {
        QUERY_MAP.put(ATSR, COINCIDING_OBSERVATION_QUERY);
        QUERY_MAP.put(AVHRR, COINCIDING_OBSERVATION_QUERY);
        QUERY_MAP.put(AMSRE, COINCIDING_OBSERVATION_QUERY);
        QUERY_MAP.put(TMI, COINCIDING_OBSERVATION_QUERY);
        QUERY_MAP.put(SEAICE, COINCIDING_OBSERVATION_QUERY);
        QUERY_MAP.put(AAI, COINCIDING_GLOBALOBS_QUERY);
        QUERY_MAP.put(HISTORY, COINCIDING_INSITUOBS_QUERY);
    }

    public static void main(String[] args) {
        // comment out the following two lines in order to activate the tool
        //System.out.println("The matchup tool is deactivated in order to preserve the state of the database.");
        //System.exit(0);
        final MatchupTool tool = new MatchupTool();
        try {
            final boolean performWork = tool.setCommandLineArgs(args);
            if (!performWork) {
                return;
            }
            tool.initialize();
            tool.cleanup();
            tool.findMultiSensorMatchups();
            tool.findSingleSensorMatchups();
        } catch (ToolException e) {
            tool.getErrorHandler().handleError(e, e.getMessage(), e.getExitCode());
        } catch (Throwable t) {
            tool.getErrorHandler().handleError(t, t.getMessage(), 1);
        }
    }

    /**
     * JPA persistence entity manager
     */
    public MatchupTool() {
        super("mmsmatchup.sh", "0.1");
    }


    /**
     * Loops over (A)ATSR reference observations and inquires METOP and SEVIRI
     * fulfilling the coincidence criterion by a spatio-temporal database query.
     * Creates matchups for the temporally nearest coincidences. Does the same
     * for METOP as reference and SEVIRI as inquired coincidence.
     *
     * @throws ToolException when an error has occurred.
     */
    public void findMultiSensorMatchups() throws ToolException {
        try {
            // open database
            getPersistenceManager().transaction();

            // loop over aatsr observations
            int count = 0;
            long time = System.currentTimeMillis();
            final List<ReferenceObservation> atsrObservations =
                    inquireReferenceObservations(SENSOR_OBSERVATION_QUERY, ATSR_MD);
            for (final ReferenceObservation atsrObservation : atsrObservations) {
                Matchup matchup = null;

                // determine corresponding metop observation if any
                final Observation metopObservation = findCoincidingObservation(atsrObservation, METOP);
                if (metopObservation != null) {
                    matchup = createMatchup(atsrObservation);
                    getPersistenceManager().persist(matchup);
                    Coincidence metopCoincidence = createCoincidence(matchup, metopObservation);
                    getPersistenceManager().persist(metopCoincidence);
                    matchup.setPattern(ATSR_MD.getPattern() | METOP.getPattern());
                }

                // determine corresponding seviri observation if any
                final Observation seviriObservation = findCoincidingObservation(atsrObservation, SEVIRI);
                if (seviriObservation != null) {
                    if (matchup == null) {
                        matchup = createMatchup(atsrObservation);
                        getPersistenceManager().persist(matchup);
                        matchup.setPattern(ATSR_MD.getPattern());
                    }
                    Coincidence seviriCoincidence = createCoincidence(matchup, seviriObservation);
                    getPersistenceManager().persist(seviriCoincidence);
                    matchup.setPattern(matchup.getPattern() | SEVIRI.getPattern());
                }
                if (matchup != null) {
                    findRelatedObservations(matchup);
                }

                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    getLogger().info(MessageFormat.format("{0}/{1} {2} processed in {3} ms.",
                                                          count,
                                                          atsrObservations.size(),
                                                          ATSR_MD.getSensor(),
                                                          System.currentTimeMillis() - time));
                    time = System.currentTimeMillis();
                }
            }

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            // loop over metop observations not yet included in aatsr coincidences
            count = 0;
            time = System.currentTimeMillis();
            final List<ReferenceObservation> metopObservations =
                    inquireReferenceObservations(SECONDARY_OBSERVATION_QUERY, METOP);
            for (final ReferenceObservation metopObservation : metopObservations) {
                // determine corresponding seviri observation if any
                final Observation seviriObservation = findCoincidingObservation(metopObservation, SEVIRI);
                if (seviriObservation != null) {
                    final Matchup matchup = createMatchup(metopObservation);
                    getPersistenceManager().persist(matchup);
                    final Coincidence seviriCoincidence = createCoincidence(matchup, seviriObservation);
                    getPersistenceManager().persist(seviriCoincidence);
                    matchup.setPattern(METOP.getPattern() | SEVIRI.getPattern());
                    findRelatedObservations(matchup);
                }

                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    getLogger().info(MessageFormat.format("{0}/{1} {2} processed in {3} ms.",
                                                          count,
                                                          metopObservations.size(),
                                                          METOP.getSensor(),
                                                          System.currentTimeMillis() - time));
                    time = System.currentTimeMillis();
                }
            }

            // make changes in database
            getPersistenceManager().commit();
        } catch (Exception e) {
            // do not make any change in case of errors
            getPersistenceManager().rollback();
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    public void findSingleSensorMatchups() throws ToolException {
        try {
            // open database
            getPersistenceManager().transaction();

            // loop over aatsr observations
            int count = 0;
            long time = System.currentTimeMillis();
            final List<ReferenceObservation> atsrObservations = inquireReferenceObservations(
                    SINGLE_SENSOR_OBSERVATION_QUERY, ATSR_MD);
            for (final ReferenceObservation aatsrObservation : atsrObservations) {
                Matchup matchup = createMatchup(aatsrObservation);
                getPersistenceManager().persist(matchup);
                matchup.setPattern(ATSR_MD.getPattern());
                findRelatedObservations(matchup);
                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    getLogger().info(MessageFormat.format("{0}/{1} {2} processed in {3} ms.",
                                                          count,
                                                          atsrObservations.size(),
                                                          ATSR_MD.getSensor(),
                                                          System.currentTimeMillis() - time));
                    time = System.currentTimeMillis();
                }
            }

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            // loop over metop observations not yet included in aatsr coincidences
            count = 0;
            time = System.currentTimeMillis();
            final List<ReferenceObservation> metopObservations = inquireReferenceObservations(
                    SINGLE_SENSOR_OBSERVATION_QUERY, METOP);
            for (final ReferenceObservation metopObservation : metopObservations) {
                Matchup matchup = createMatchup(metopObservation);
                getPersistenceManager().persist(matchup);
                matchup.setPattern(METOP.getPattern());
                findRelatedObservations(matchup);
                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    getLogger().info(MessageFormat.format("{0}/{1} {2} processed in {3} ms.",
                                                          count,
                                                          metopObservations.size(),
                                                          METOP.getSensor(),
                                                          System.currentTimeMillis() - time));
                    time = System.currentTimeMillis();
                }
            }

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            count = 0;
            time = System.currentTimeMillis();
            final List<ReferenceObservation> seviriObservations = inquireReferenceObservations(
                    SINGLE_SENSOR_OBSERVATION_QUERY, SEVIRI);
            for (final ReferenceObservation seviriObservation : seviriObservations) {
                final Matchup matchup = createMatchup(seviriObservation);
                getPersistenceManager().persist(matchup);
                matchup.setPattern(SEVIRI.getPattern());
                findRelatedObservations(matchup);
                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    getLogger().info(MessageFormat.format("{0}/{1} {2} processed in {3} ms.",
                                                          count,
                                                          seviriObservations.size(),
                                                          SEVIRI.getSensor(),
                                                          System.currentTimeMillis() - time));
                    time = System.currentTimeMillis();
                }
            }

            // make changes in database
            getPersistenceManager().commit();
        } catch (Exception e) {
            // do not make any change in case of errors
            getPersistenceManager().rollback();
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    private void findRelatedObservations(Matchup matchup) throws ToolException {
        final Properties configuration = getConfiguration();
        for (int i = 0; i < 100; i++) {
            final String sensorTypeName = configuration.getProperty(
                    String.format("mms.test.inputSets.%d.sensorType", i));
            final String sensorName = configuration.getProperty(
                    String.format("mms.test.inputSets.%d.sensor", i));
            if (sensorTypeName == null || sensorName == null) {
                continue;
            }
            if (!SensorType.isSensorType(sensorTypeName)) {
                throw new ToolException(MessageFormat.format("Unknown sensor type ''{0}''.", sensorTypeName),
                                        ToolException.TOOL_CONFIGURATION_ERROR);
            }
            final SensorType sensorType = SensorType.valueOfIgnoreCase(sensorTypeName);
            final String queryString = QUERY_MAP.get(sensorType);
            if (queryString != null) {
                addCoincidence(matchup, sensorName, queryString, sensorType);
            }
        }
    }

    private void addCoincidence(Matchup matchup, String sensorName, String queryString, SensorType sensorType) {
        final ReferenceObservation refObs = matchup.getRefObs();
        final Query query = createObservationQuery(queryString, sensorType.getObservationClass());
        final Observation sensorObs = findCoincidingObservation(refObs, query, sensorName);
        if (sensorObs != null) {
            final Coincidence coincidence = createCoincidence(matchup, sensorObs);
            getPersistenceManager().persist(coincidence);
            matchup.setPattern(matchup.getPattern() | sensorType.getPattern());
        }
    }

    /**
     * Inquires observations of a certain sensor.
     *
     * @param queryString The JPA query with one numbered variable.
     * @param sensorType  The sensor type.
     *
     * @return list of observations of this sensor that fulfils the query
     */
    @SuppressWarnings({"unchecked"})
    private List<ReferenceObservation> inquireReferenceObservations(String queryString, SensorType sensorType) {
        final Query query = getPersistenceManager().createQuery(queryString);
        query.setParameter(1, sensorType.getSensor());
        return query.getResultList();
    }

    /**
     * Determines temporally nearest common observation of a specific sensor
     * for a reference observation.
     *
     * @param refObs     The reference observation.
     * @param sensorType The sensor type.
     *
     * @return common observation of sensor with coincidence to reference observation
     *         that is temporally closest to reference observation, null if no
     *         observation of the specified sensor has a coincidence with the
     *         reference observation
     */
    private Observation findCoincidingObservation(ReferenceObservation refObs, SensorType sensorType) {
        final Query query = createObservationQuery(COINCIDING_OBSERVATION_QUERY, sensorType.getObservationClass());
        return findCoincidingObservation(refObs, query, sensorType.getSensor());
    }

    private Query createObservationQuery(String queryString, Class<? extends Observation> resultClass) {
        return getPersistenceManager().createNativeQuery(queryString, resultClass);
    }

    private Observation findCoincidingObservation(ReferenceObservation refObs, Query query, String sensor) {
        query.setParameter(1, refObs.getId());
        query.setParameter(2, sensor);
        @SuppressWarnings({"unchecked"})
        final List<? extends Observation> observations = query.getResultList();
        if (observations.size() > 0) {
            // select temporally nearest common observation
            return observations.get(0);
        } else {
            return null;
        }
    }


    /**
     * Factory method to create matchup for a reference observation.
     *
     * @param referenceObservation the reference observation constituting the matchup
     *
     * @return the new Matchup for the reference observation
     */
    private Matchup createMatchup(ReferenceObservation referenceObservation) {
        final Matchup matchup = new Matchup();
        matchup.setRefObs(referenceObservation);
        return matchup;
    }

    /**
     * Creates Coincidence between a matchup and a common observation,
     * determines temporal and spatial distance by a database query.
     *
     * @param matchup     the matchup with the reference observation
     * @param observation the common observation that has a coincidence with
     *                    the reference and is temporally closest to it
     *
     * @return newly created Coincidence relating matchup and common observation
     */
    private Coincidence createCoincidence(Matchup matchup, Observation observation) {
        Assert.argument(observation instanceof Timed, "!(observation instanceof Timed)");
        final double timeDifference = TimeUtil.computeTimeDelta(matchup, (Timed)observation);
        final Coincidence coincidence = new Coincidence();
        coincidence.setMatchup(matchup);
        coincidence.setObservation(observation);
        coincidence.setTimeDifference(timeDifference);

        return coincidence;
    }

    private void cleanup() throws ToolException {
        getPersistenceManager().transaction();

        // clear coincidences as they are computed from scratch
        Query delete = getPersistenceManager().createQuery("delete from Coincidence c");
        delete.executeUpdate();
        delete = getPersistenceManager().createQuery("delete from Matchup m");
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

}
