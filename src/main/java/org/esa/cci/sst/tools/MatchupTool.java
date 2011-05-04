package org.esa.cci.sst.tools;

import com.bc.ceres.core.Assert;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.data.Timeable;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Tool to compute multi-sensor match-ups from the MMS database.
 */
public class MatchupTool extends BasicTool {

    private static final String SENSOR_OBSERVATION_QUERY =
            "select o"
            + " from ReferenceObservation o"
            + " where o.sensor = ?1"
            + " and o.time >= ?2 and o.time < ?3"
            + " order by o.time";

    private static final String SINGLE_SENSOR_OBSERVATION_QUERY =
            "select o"
            + " from ReferenceObservation o"
            + " where o.sensor = ?1"
            + " and o.time >= ?2 and o.time < ?3"
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
            + " and o.time >= ?2 and o.time < ?3"
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


    private static final String ATSR_MD = "atsr_md";
    private static final String METOP = "metop";
    private static final String SEVIRI = "seviri";

    private static final Map<Class<? extends Observation>, String> OBSERVATION_QUERY_MAP =
            new HashMap<Class<? extends Observation>, String>(12);

    private Sensor atsrSensor;
    private Sensor metopSensor;
    private Sensor seviriSensor;

    static {
        OBSERVATION_QUERY_MAP.put(ReferenceObservation.class, COINCIDING_OBSERVATION_QUERY);
        OBSERVATION_QUERY_MAP.put(RelatedObservation.class, COINCIDING_OBSERVATION_QUERY);
        OBSERVATION_QUERY_MAP.put(GlobalObservation.class, COINCIDING_GLOBALOBS_QUERY);
        OBSERVATION_QUERY_MAP.put(InsituObservation.class, COINCIDING_INSITUOBS_QUERY);
    }

    public static void main(String[] args) {
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
            tool.getErrorHandler().terminate(e);
        } catch (Throwable t) {
            tool.getErrorHandler().terminate(new ToolException(t.getMessage(), t, ToolException.UNKNOWN_ERROR));
        }
    }

    /**
     * JPA persistence entity manager
     */
    public MatchupTool() {
        super("mmsmatchup.sh", "0.1");
    }

    @Override
    public void initialize() {
        super.initialize();
        atsrSensor = getSensor(ATSR_MD);
        metopSensor = getSensor(METOP);
        seviriSensor = getSensor(SEVIRI);
    }

    /**
     * Loops over (A)ATSR reference observations and inquires METOP and SEVIRI
     * fulfilling the coincidence criterion by a spatio-temporal database query.
     * Creates matchups for the temporally nearest coincidences. Does the same
     * for METOP as reference and SEVIRI as inquired coincidence.
     */
    public void findMultiSensorMatchups() {
        try {
            // open database
            getPersistenceManager().transaction();

            // loop over aatsr observations
            int count = 0;
            long time = System.currentTimeMillis();
            final List<ReferenceObservation> atsrObservations =
                    getReferenceObservations(SENSOR_OBSERVATION_QUERY, ATSR_MD);

            for (final ReferenceObservation atsrObservation : atsrObservations) {
                Matchup matchup = null;

                // determine corresponding metop observation if any
                if (metopSensor != null) {
                    final Observation metopObservation = findCoincidingObservation(atsrObservation, metopSensor);
                    if (metopObservation != null) {
                        matchup = createMatchup(atsrObservation);
                        getPersistenceManager().persist(matchup);
                        Coincidence metopCoincidence = createCoincidence(matchup, metopObservation);
                        getPersistenceManager().persist(metopCoincidence);
                        matchup.setPattern(atsrSensor.getPattern() | metopSensor.getPattern());
                    }
                }

                // determine corresponding seviri observation if any
                if (seviriSensor != null) {
                    final Observation seviriObservation = findCoincidingObservation(atsrObservation, seviriSensor);
                    if (seviriObservation != null) {
                        if (matchup == null) {
                            matchup = createMatchup(atsrObservation);
                            getPersistenceManager().persist(matchup);
                            matchup.setPattern(atsrSensor.getPattern());
                        }
                        Coincidence seviriCoincidence = createCoincidence(matchup, seviriObservation);
                        getPersistenceManager().persist(seviriCoincidence);
                        matchup.setPattern(matchup.getPattern() | seviriSensor.getPattern());
                    }
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
                                                          atsrSensor.getName(),
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
                    getReferenceObservations(SECONDARY_OBSERVATION_QUERY, METOP);
            if (seviriSensor != null) {
                for (final ReferenceObservation metopObservation : metopObservations) {
                    // determine corresponding seviri observation if any
                    final Observation seviriObservation = findCoincidingObservation(metopObservation, seviriSensor);
                    if (seviriObservation != null) {
                        final Matchup matchup = createMatchup(metopObservation);
                        getPersistenceManager().persist(matchup);
                        final Coincidence seviriCoincidence = createCoincidence(matchup, seviriObservation);
                        getPersistenceManager().persist(seviriCoincidence);
                        matchup.setPattern(metopSensor.getPattern() | seviriSensor.getPattern());
                        findRelatedObservations(matchup);
                    }

                    ++count;
                    if (count % 1024 == 0) {
                        getPersistenceManager().commit();
                        getPersistenceManager().transaction();
                        getLogger().info(MessageFormat.format("{0}/{1} {2} processed in {3} ms.",
                                                              count,
                                                              metopObservations.size(),
                                                              metopSensor.getName(),
                                                              System.currentTimeMillis() - time));
                        time = System.currentTimeMillis();
                    }
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

    public void findSingleSensorMatchups() {
        try {
            // open database
            getPersistenceManager().transaction();

            // loop over aatsr observations
            int count = 0;
            long time = System.currentTimeMillis();
            final List<ReferenceObservation> atsrObservations =
                    getReferenceObservations(SINGLE_SENSOR_OBSERVATION_QUERY, ATSR_MD);

            for (final ReferenceObservation aatsrObservation : atsrObservations) {
                Matchup matchup = createMatchup(aatsrObservation);
                getPersistenceManager().persist(matchup);
                matchup.setPattern(atsrSensor.getPattern());
                findRelatedObservations(matchup);
                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    getLogger().info(MessageFormat.format("{0}/{1} {2} processed in {3} ms.",
                                                          count,
                                                          atsrObservations.size(),
                                                          atsrSensor.getName(),
                                                          System.currentTimeMillis() - time));
                    time = System.currentTimeMillis();
                }
            }

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            // loop over metop observations not yet included in aatsr coincidences
            count = 0;
            time = System.currentTimeMillis();
            final List<ReferenceObservation> metopObservations = getReferenceObservations(
                    SINGLE_SENSOR_OBSERVATION_QUERY, METOP);
            for (final ReferenceObservation metopObservation : metopObservations) {
                Matchup matchup = createMatchup(metopObservation);
                getPersistenceManager().persist(matchup);
                matchup.setPattern(metopSensor.getPattern());
                findRelatedObservations(matchup);
                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    getLogger().info(MessageFormat.format("{0}/{1} {2} processed in {3} ms.",
                                                          count,
                                                          metopObservations.size(),
                                                          metopSensor.getName(),
                                                          System.currentTimeMillis() - time));
                    time = System.currentTimeMillis();
                }
            }

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            count = 0;
            time = System.currentTimeMillis();
            final List<ReferenceObservation> seviriObservations = getReferenceObservations(
                    SINGLE_SENSOR_OBSERVATION_QUERY, SEVIRI);
            for (final ReferenceObservation seviriObservation : seviriObservations) {
                final Matchup matchup = createMatchup(seviriObservation);
                getPersistenceManager().persist(matchup);
                matchup.setPattern(seviriSensor.getPattern());
                findRelatedObservations(matchup);
                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    getLogger().info(MessageFormat.format("{0}/{1} {2} processed in {3} ms.",
                                                          count,
                                                          seviriObservations.size(),
                                                          seviriSensor.getName(),
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

    private void findRelatedObservations(Matchup matchup) {
        final Properties configuration = getConfiguration();
        for (int i = 0; i < 100; i++) {
            final String sensorName = configuration.getProperty(String.format("mms.source.%d.sensor", i));
            if (sensorName == null) {
                continue;
            }
            final Sensor sensor = getSensor(sensorName);
            if (sensor != null) {
                final Class<? extends Observation> observationClass = getObservationClass(sensor);
                if (observationClass != ReferenceObservation.class) {
                    final String queryString = OBSERVATION_QUERY_MAP.get(observationClass);
                    if (queryString == null) {
                        final String message = MessageFormat.format("No query for observation type ''{0}''",
                                                                    observationClass.getSimpleName());
                        throw new ToolException(message, ToolException.TOOL_CONFIGURATION_ERROR);
                    }
                    addCoincidence(matchup, sensorName, queryString, sensor.getPattern(), observationClass);
                }
            }
        }
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
        final ReferenceObservation refObs = matchup.getRefObs();
        final Query query = createObservationQuery(queryString, observationClass);
        final Observation sensorObs = findCoincidingObservation(refObs, query, sensorName);
        if (sensorObs != null) {
            final Coincidence coincidence = createCoincidence(matchup, sensorObs);
            getPersistenceManager().persist(coincidence);
            matchup.setPattern(matchup.getPattern() | pattern);
        }
    }

    /**
     * Inquires observations of a certain sensor.
     *
     * @param queryString The JPA query with one numbered variable.
     * @param sensorName  The sensor name.
     *
     * @return list of observations of this sensor that fulfils the query
     */
    @SuppressWarnings({"unchecked"})
    private List<ReferenceObservation> getReferenceObservations(String queryString, String sensorName) {
        final Query query = getPersistenceManager().createQuery(queryString);
        query.setParameter(1, sensorName);
        query.setParameter(2, getSourceStartTime());
        query.setParameter(3, getSourceStopTime());

        return query.getResultList();
    }

    /**
     * Determines temporally nearest common observation of a specific sensor
     * for a reference observation.
     *
     * @param refObs The reference observation.
     * @param sensor The sensor.
     *
     * @return common observation of sensor with coincidence to reference observation
     *         that is temporally closest to reference observation, null if no
     *         observation of the specified sensor has a coincidence with the
     *         reference observation
     */
    private Observation findCoincidingObservation(ReferenceObservation refObs, Sensor sensor) {
        final Class<? extends Observation> observationClass = getObservationClass(sensor);
        final Query query = createObservationQuery(COINCIDING_OBSERVATION_QUERY, observationClass);
        return findCoincidingObservation(refObs, query, sensor.getName());
    }

    private Query createObservationQuery(String queryString, Class<? extends Observation> resultClass) {
        return getPersistenceManager().createNativeQuery(queryString, resultClass);
    }

    private Observation findCoincidingObservation(ReferenceObservation refObs, Query query, String sensorName) {
        query.setParameter(1, refObs.getId());
        query.setParameter(2, sensorName);
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
        Assert.argument(observation instanceof Timeable, "!(observation instanceof Timeable)");
        final double timeDifference = TimeUtil.timeDifferenceInSeconds(matchup, (Timeable) observation);
        final Coincidence coincidence = new Coincidence();
        coincidence.setMatchup(matchup);
        coincidence.setObservation(observation);
        coincidence.setTimeDifference(timeDifference);

        return coincidence;
    }

    private void cleanup() {
        getPersistenceManager().transaction();

        // clear coincidences as they are computed from scratch
        Query delete = getPersistenceManager().createQuery("delete from Coincidence c");
        delete.executeUpdate();
        delete = getPersistenceManager().createQuery("delete from Matchup m");
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

}
