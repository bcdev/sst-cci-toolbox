package org.esa.cci.sst;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.orm.PersistenceManager;

import javax.persistence.Query;
import java.util.List;

/**
 * Tool to compute multi-sensor matchups the MMS database.
 */
public class MatchupTool extends MmsTool {

    /**
     * Name of persistence unit in META-INF/persistence.xml
     */
    private static final String PERSISTENCE_UNIT_NAME = "matchupdb";

    private static final String AATSR_AS_REFERENCE = "aatsr.ref";
    private static final String METOP_AS_REFERENCE = "metop.ref";
    private static final String METOP_SENSOR = "metop";
    private static final String SEVIRI_SENSOR = "seviri";

    private static final String SENSOR_OBSERVATION_QUERY =
            "select o"
            + " from Observation o"
            + " where o.sensor = ?1"
            + " order by o.time";

    private static final String SECONDARY_OBSERVATION_QUERY =
            "select o from Observation o"
            + " where o.sensor = ?1"
            + " and not exists (select c from Coincidence c where c.observation = o)"
            + " order by o.time";

    private static final String CORRESPONDING_OBSERVATION_QUERY =
            "select o.id"
            + " from mm_observation o, mm_observation oref"
            + " where oref.id = ?"
            + " and o.sensor = ?"
            + " and o.time >= oref.time - '12:00:00' and o.time < oref.time + '12:00:00'"
            + " and st_intersects(o.location,oref.location)"
            + " order by abs(extract(epoch from o.time) - extract(epoch from oref.time))";

    private static final String DISTANCE_QUERY =
            "select abs(extract(epoch from o1.time) - extract(epoch from o2.time)), st_distance(o1.location,o2.location)"
            + " from mm_observation o1, mm_observation o2"
            + " where o1.id = ?"
            + " and o2.id = ?";

    /**
     * JPA persistence entity manager
     */
    private PersistenceManager persistenceManager = new PersistenceManager(PERSISTENCE_UNIT_NAME);

    /**
     * Loops over (A)ATSR reference observations and inquires METOP and SEVIRI
     * fulfilling the coincidence criterion by a spatio-temporal database query.
     * Creates matchups for the temporally nearest coincidences. Does the same
     * for METOP as reference and SEVIRI as inquired coincidence.
     *
     * @throws Exception
     */
    public void findCoincidences() throws Exception {
        try {
            // open database
            persistenceManager.transaction();

            // clear coincidences as they are computed from scratch
            Query delete = persistenceManager.createQuery("delete from Coincidence c");
            delete.executeUpdate();
            delete = persistenceManager.createQuery("delete from Matchup m");
            delete.executeUpdate();

            // loop over aatsr observations
            final List<Observation> aatsrObservations = inquireObservations(SENSOR_OBSERVATION_QUERY,
                                                                            AATSR_AS_REFERENCE);
            for (Observation aatsrObservation : aatsrObservations) {
                //System.out.println(aatsrObservation);
                Matchup matchup = null;

                // determine corresponding metop observation if any
                final Observation metopObservation = findCorrespondingObservation(aatsrObservation, METOP_SENSOR);
                if (metopObservation != null) {
                    matchup = createMatchup(aatsrObservation);
                    persistenceManager.persist(matchup);
                    Coincidence metopCoincidence = createCoincidence(matchup, metopObservation);
                    persistenceManager.persist(metopCoincidence);
                }

                // determine corresponding seviri observation if any
                final Observation seviriObservation = findCorrespondingObservation(aatsrObservation, SEVIRI_SENSOR);
                if (seviriObservation != null) {
                    if (matchup == null) {
                        matchup = createMatchup(aatsrObservation);
                        persistenceManager.persist(matchup);
                    }
                    Coincidence seviriCoincidence = createCoincidence(matchup, seviriObservation);
                    persistenceManager.persist(seviriCoincidence);
                }
            }

            System.out.println();

            // loop over metop observations not yet included in aatsr coincidences
            final List<Observation> metopObservations = inquireObservations(SECONDARY_OBSERVATION_QUERY,
                                                                            METOP_AS_REFERENCE);
            for (Observation metopObservation : metopObservations) {
                //System.out.println(metopObservation);

                // determine corresponding seviri observation if any
                final Observation seviriObservation = findCorrespondingObservation(metopObservation, SEVIRI_SENSOR);
                if (seviriObservation != null) {
                    Matchup matchup = createMatchup(metopObservation);
                    persistenceManager.persist(matchup);
                    Coincidence seviriCoincidence = createCoincidence(matchup, seviriObservation);
                    persistenceManager.persist(seviriCoincidence);
                }
            }

            // make changes in database
            persistenceManager.commit();

        } catch (Exception e) {

            // do not make any change in case of errors
            persistenceManager.rollback();
            throw e;
        }

    }

    /**
     * Inquires observations of a certain sensor.
     *
     * @param queryString JPA query with one numbered variable
     * @param sensor      name of sensor, for example "aatsr.ref"
     *
     * @return list of observations of this sensor that fulfils the query
     */
    private List<Observation> inquireObservations(String queryString, String sensor) {

        final Query query = persistenceManager.createQuery(queryString);
        query.setParameter(1, sensor);
        return (List<Observation>) query.getResultList();
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
    private Observation findCorrespondingObservation(Observation referenceObservation, String sensor) {

        final Query observationQuery = persistenceManager.createNativeQuery(CORRESPONDING_OBSERVATION_QUERY,
                                                                            Observation.class);
        observationQuery.setParameter(1, referenceObservation.getId());
        observationQuery.setParameter(2, sensor);
        final List<Observation> observations = observationQuery.getResultList();

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
    private Matchup createMatchup(Observation referenceObservation) {

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

        final Object[] diffs = (Object[]) persistenceManager.pickNative(DISTANCE_QUERY, matchup.getRefObs().getId(),
                                                                        observation.getId());
        final int timeDifference = ((Double) diffs[0]).intValue();
        final float distance = ((Double) diffs[1]).floatValue();

        final Coincidence coincidence = new Coincidence();
        coincidence.setMatchup(matchup);
        coincidence.setObservation(observation);
        coincidence.setDistance(distance);
        coincidence.setTimeDifference(timeDifference);

        // TODO convert to log entry or suppress
        System.out.println(String.format("  %d sec, %.3f m: %s-%d %s-%d",
                                         timeDifference, distance,
                                         matchup.getRefObs().getSensor(), matchup.getId(),
                                         observation.getSensor(), observation.getId()));
        return coincidence;
    }
}
