package org.esa.cci.sst;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.RelatedObservation;

import javax.persistence.Query;
import java.util.List;

import static org.esa.cci.sst.SensorName.*;

/**
 * Tool to compute multi-sensor matchups the MMS database.
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

    private static final String CORRESPONDING_OBSERVATION_QUERY =
            "select o.id"
            + " from mm_observation o, mm_observation oref"
            + " where oref.id = ?"
            + " and o.sensor = ?"
            + " and o.time >= oref.time - '12:00:00' and o.time < oref.time + '12:00:00'"
            + " and st_intersects(o.location,oref.point)"
            + " order by abs(extract(epoch from o.time) - extract(epoch from oref.time))";

    private static final String CORRESPONDING_GLOBALOBS_QUERY =
            "select o.id"
            + " from mm_observation o, mm_observation oref"
            + " where oref.id = ?"
            + " and o.sensor = ?"
            + " and o.time >= oref.time - '12:00:00' and o.time < oref.time + '12:00:00'"
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

    /**
     * JPA persistence entity manager
     */
    public MatchupTool() {
        super("mms-matchup", "0.1");
    }

    /**
     * Loops over (A)ATSR reference observations and inquires METOP and SEVIRI
     * fulfilling the coincidence criterion by a spatio-temporal database query.
     * Creates matchups for the temporally nearest coincidences. Does the same
     * for METOP as reference and SEVIRI as inquired coincidence.
     *
     * @throws Exception
     */
    public void findMultiSensorMatchups() throws ToolException {
        try {
            // open database
            getPersistenceManager().transaction();

            // loop over aatsr observations
            int count = 0;
            long time = System.currentTimeMillis();
            final List<ReferenceObservation> aatsrObservations = inquireObservations(SENSOR_OBSERVATION_QUERY,
                                                                                     SENSOR_NAME_AATSR_MD.getSensor());
            for (ReferenceObservation aatsrObservation : aatsrObservations) {
                Matchup matchup = null;

                // determine corresponding metop observation if any
                final ReferenceObservation metopObservation = findCorrespondingObservation(aatsrObservation,
                                                                                           SENSOR_NAME_METOP.getSensor());
                if (metopObservation != null) {
                    matchup = createMatchup(aatsrObservation);
                    getPersistenceManager().persist(matchup);
                    Coincidence metopCoincidence = createCoincidence(matchup, metopObservation);
                    getPersistenceManager().persist(metopCoincidence);
                    matchup.setPattern(0x3);
                }

                // determine corresponding seviri observation if any
                final ReferenceObservation seviriObservation = findCorrespondingObservation(aatsrObservation,
                                                                                            SENSOR_NAME_SEVIRI.getSensor());
                if (seviriObservation != null) {
                    if (matchup == null) {
                        matchup = createMatchup(aatsrObservation);
                        getPersistenceManager().persist(matchup);
                        matchup.setPattern(0x1);
                    }
                    Coincidence seviriCoincidence = createCoincidence(matchup, seviriObservation);
                    getPersistenceManager().persist(seviriCoincidence);
                    matchup.setPattern(matchup.getPattern() | 0x4);
                }
                if (matchup != null) {
                    findRelatedObservations(matchup);
                }

                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    System.out.format("%6d/%d %s processed in %d ms\n", count, aatsrObservations.size(),
                                      SENSOR_NAME_AATSR_MD.getSensor(), System.currentTimeMillis() - time);
                    time = System.currentTimeMillis();
                }
            }

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            // loop over metop observations not yet included in aatsr coincidences
            count = 0;
            time = System.currentTimeMillis();
            final List<ReferenceObservation> metopObservations = inquireObservations(SECONDARY_OBSERVATION_QUERY,
                                                                                     SENSOR_NAME_METOP.getSensor());
            for (ReferenceObservation metopObservation : metopObservations) {

                // determine corresponding seviri observation if any
                final ReferenceObservation seviriObservation = findCorrespondingObservation(metopObservation,
                                                                                            SENSOR_NAME_SEVIRI.getSensor());
                if (seviriObservation != null) {
                    Matchup matchup = createMatchup(metopObservation);
                    getPersistenceManager().persist(matchup);
                    Coincidence seviriCoincidence = createCoincidence(matchup, seviriObservation);
                    getPersistenceManager().persist(seviriCoincidence);
                    matchup.setPattern(0x6);
                    findRelatedObservations(matchup);
                }

                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    System.out.format("%6d/%d %s processed in %d ms\n", count, metopObservations.size(),
                                      SENSOR_NAME_METOP.getSensor(), System.currentTimeMillis() - time);
                    time = System.currentTimeMillis();
                }
            }

            // make changes in database
            getPersistenceManager().commit();
        } catch (Exception e) {
            // do not make any change in case of errors
            getPersistenceManager().rollback();
            throw new ToolException(e.getMessage(), 1, e);
        }
    }


    public void findSingleSensorMatchups() throws ToolException {
        try {
            // open database
            getPersistenceManager().transaction();

            // loop over aatsr observations
            int count = 0;
            long time = System.currentTimeMillis();
            final List<ReferenceObservation> aatsrObservations = inquireObservations(SINGLE_SENSOR_OBSERVATION_QUERY,
                                                                                     SENSOR_NAME_AATSR_MD.getSensor());
            for (ReferenceObservation aatsrObservation : aatsrObservations) {
                Matchup matchup = createMatchup(aatsrObservation);
                getPersistenceManager().persist(matchup);
                matchup.setPattern(0x1);
                findRelatedObservations(matchup);
                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    System.out.format("%6d/%d %s processed in %d ms\n", count, aatsrObservations.size(),
                                      SENSOR_NAME_AATSR_MD.getSensor(), System.currentTimeMillis() - time);
                    time = System.currentTimeMillis();
                }
            }

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            // loop over metop observations not yet included in aatsr coincidences
            count = 0;
            time = System.currentTimeMillis();
            final List<ReferenceObservation> metopObservations = inquireObservations(SINGLE_SENSOR_OBSERVATION_QUERY,
                                                                                     SENSOR_NAME_METOP.getSensor());
            for (ReferenceObservation metopObservation : metopObservations) {
                Matchup matchup = createMatchup(metopObservation);
                getPersistenceManager().persist(matchup);
                matchup.setPattern(0x2);
                findRelatedObservations(matchup);
                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    System.out.format("%6d/%d %s processed in %d ms\n", count, metopObservations.size(),
                                      SENSOR_NAME_METOP.getSensor(), System.currentTimeMillis() - time);
                    time = System.currentTimeMillis();
                }
            }

            getPersistenceManager().commit();
            getPersistenceManager().transaction();

            count = 0;
            time = System.currentTimeMillis();
            final List<ReferenceObservation> seviriObservations = inquireObservations(SINGLE_SENSOR_OBSERVATION_QUERY,
                                                                                      SENSOR_NAME_SEVIRI.getSensor());
            for (ReferenceObservation seviriObservation : seviriObservations) {
                final Matchup matchup = createMatchup(seviriObservation);
                getPersistenceManager().persist(matchup);
                matchup.setPattern(0x4);
                findRelatedObservations(matchup);
                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    System.out.format("%6d/%d %s processed in %d ms\n", count, seviriObservations.size(),
                                      SENSOR_NAME_SEVIRI.getSensor(), System.currentTimeMillis() - time);
                    time = System.currentTimeMillis();
                }
            }

            // make changes in database
            getPersistenceManager().commit();
        } catch (Exception e) {
            // do not make any change in case of errors
            getPersistenceManager().rollback();
            throw new ToolException(e.getMessage(), 1, e);
        }
    }

    private void findRelatedObservations(Matchup matchup) {
        addCoincidence(matchup, SENSOR_NAME_AVHRR.getSensor(), 0x8);
        addCoincidence(matchup, SENSOR_NAME_AMSRE.getSensor(), 0x10);
        addCoincidence(matchup, SENSOR_NAME_TMI.getSensor(), 0x20);
        addCoincidence(matchup, SENSOR_NAME_AATSR.getSensor(), 0x40);
        addTemporalCoincidence(matchup, SENSOR_NAME_AAI.getSensor(), 0x80);
        addCoincidence(matchup, SENSOR_NAME_SEA_ICE.getSensor(), 0x100);
    }


    private void addCoincidence(Matchup matchup, String sensorName, long pattern) {
        final ReferenceObservation refObs = matchup.getRefObs();
        final RelatedObservation sensorObs = findCorrespondingObservation(refObs, sensorName);
        if (sensorObs != null) {
            final Coincidence coincidence = createCoincidence(matchup, sensorObs);
            getPersistenceManager().persist(coincidence);
            matchup.setPattern(matchup.getPattern() | pattern);
        }
    }

    private void addTemporalCoincidence(Matchup matchup, String sensorName, long pattern) {
        final ReferenceObservation refObs = matchup.getRefObs();
        final GlobalObservation sensorObs = findCorrespondingGlobalObservation(refObs, sensorName);
        if (sensorObs != null) {
            final Coincidence coincidence = createCoincidence(matchup, sensorObs);
            getPersistenceManager().persist(coincidence);
            matchup.setPattern(matchup.getPattern() | pattern);
        }
    }

    /**
     * Inquires observations of a certain sensor.
     *
     * @param queryString JPA query with one numbered variable
     * @param sensor      name of sensor, for example "metop"
     *
     * @return list of observations of this sensor that fulfils the query
     */
    private List<ReferenceObservation> inquireObservations(String queryString, String sensor) {

        final Query query = getPersistenceManager().createQuery(queryString);
        query.setParameter(1, sensor);
        return (List<ReferenceObservation>) query.getResultList();
    }

    /**
     * Determines temporally nearest common observation of a specific sensor
     * for a reference observation.
     *
     * @param referenceObservation reference observation, for example of sensor aatsr.ref
     * @param sensor               sensor name for common observations to be looked for
     *
     * @return common observation of sensor with coincidence to reference observation
     *         that is temporally closest to reference observation, null if no
     *         observation of the specified sensor has a coincidence with the
     *         reference observation
     */
    private ReferenceObservation findCorrespondingObservation(ReferenceObservation referenceObservation,
                                                              String sensor) {

        final Query observationQuery = getPersistenceManager().createNativeQuery(CORRESPONDING_OBSERVATION_QUERY,
                                                                                 ReferenceObservation.class);
        observationQuery.setParameter(1, referenceObservation.getId());
        observationQuery.setParameter(2, sensor);
        final List<ReferenceObservation> observations = observationQuery.getResultList();

        if (observations.size() > 0) {
            // select temporally nearest common observation
            return observations.get(0);
        } else {
            return null;
        }
    }

    private GlobalObservation findCorrespondingGlobalObservation(ReferenceObservation referenceObservation,
                                                                 String sensor) {

        final Query observationQuery = getPersistenceManager().createNativeQuery(CORRESPONDING_GLOBALOBS_QUERY,
                                                                                 GlobalObservation.class);
        observationQuery.setParameter(1, referenceObservation.getId());
        observationQuery.setParameter(2, sensor);
        final List<GlobalObservation> observations = observationQuery.getResultList();

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
    private Coincidence createCoincidence(Matchup matchup, GlobalObservation observation) {

        final int timeDifference = (int) Math.abs(
                (matchup.getRefObs().getTime().getTime() - observation.getTime().getTime()) / 1000);

        final Coincidence coincidence = new Coincidence();
        coincidence.setMatchup(matchup);
        coincidence.setObservation(observation);
        coincidence.setTimeDifference(timeDifference);

        return coincidence;
    }

    public void cleanup() throws ToolException {
        getPersistenceManager().transaction();

        // clear coincidences as they are computed from scratch
        Query delete = getPersistenceManager().createQuery("delete from Coincidence c");
        delete.executeUpdate();
        delete = getPersistenceManager().createQuery("delete from Matchup m");
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

    public static void main(String[] args) {
        MatchupTool tool = new MatchupTool();
        try {
            if (!tool.setCommandLineArgs(args)) {
                return;
            }
            tool.initialize();
            tool.cleanup();
            tool.findMultiSensorMatchups();
            tool.findSingleSensorMatchups();
        } catch (ToolException e) {
            System.err.println("Error: " + e.getMessage());
            if (tool.isDebug()) {
                e.printStackTrace(System.err);
            }
            System.exit(e.getExitCode());
        }
    }

}
