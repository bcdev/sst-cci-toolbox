package org.esa.cci.sst;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.orm.PersistenceManager;

import javax.persistence.Query;
import java.util.List;

/**
 * Tool to ingest new MD files into the MMS database.
 */
public class MatchupTool {

    static final String PERSISTENCE_UNIT_NAME = "matchupdb";

    static final String AATSR_AS_REFERENCE = "aatsr.ref";
    static final String METOP_AS_REFERENCE = "metop.ref";
    static final String METOP_SENSOR = "metop";
    static final String SEVIRI_SENSOR = "seviri";

    static final String SENSOR_OBSERVATION_QUERY =
            "select o"
            + " from Observation o"
            + " where o.sensor = ?1";
    static final String SECONDARY_OBSERVATION_QUERY =
            "select o from Observation o"
            + " where o.sensor = ?1"
            + " and not exists (select c from Coincidence c where c.observation = o)";
    static final String CORRESPONDING_OBSERVATION_QUERY =
            "select o.id"
            + " from mm_observation o, mm_observation oref"
            + " where oref.id = ?"
            + " and o.sensor = ?"
            + " and o.time >= oref.time - '12:00:00' and o.time < oref.time + '12:00:00'"
            + " and st_intersects(o.location,oref.location)"
            + " order by abs(extract(epoch from o.time) - extract(epoch from oref.time))";
    static final String DISTANCE_QUERY =
            "select abs(extract(epoch from o1.time) - extract(epoch from o2.time)), st_distance(o1.location,o2.location)"
            + " from mm_observation o1, mm_observation o2"
            + " where o1.id = ?"
            + " and o2.id = ?";

    private PersistenceManager persistenceManager = new PersistenceManager(PERSISTENCE_UNIT_NAME);

    public void findCoincidences() throws Exception {
        try {
            // open database
            persistenceManager.transaction();

            // clear coincidences as they are computed from scratch
            Query delete = persistenceManager.createQuery("delete from Coincidence c");
            delete.executeUpdate();

            // loop over aatsr observations
            final List<Observation> aatsrObservations = inquireObservations(SENSOR_OBSERVATION_QUERY, AATSR_AS_REFERENCE);
            for (Observation aatsrObservation : aatsrObservations) {
                //System.out.println(aatsrObservation);
                Coincidence aatsrCoincidence = null;

                // determine corresponding metop observation if any
                final Observation metopObservation = findCorrespondingObservation(aatsrObservation, METOP_SENSOR);
                if (metopObservation != null) {
                    aatsrCoincidence = createSelfCoincidence(aatsrObservation);
                    persistenceManager.persist(aatsrCoincidence);
                    Coincidence metopCoincidence = createCoincidence(aatsrObservation, metopObservation);
                    persistenceManager.persist(metopCoincidence);
                }

                // determine corresponding seviri observation if any
                final Observation seviriObservation = findCorrespondingObservation(aatsrObservation, SEVIRI_SENSOR);
                if (seviriObservation != null) {
                    if (aatsrCoincidence == null) {
                        aatsrCoincidence = createSelfCoincidence(aatsrObservation);
                        persistenceManager.persist(aatsrCoincidence);
                    }
                    Coincidence seviriCoincidence = createCoincidence(aatsrObservation, seviriObservation);
                    persistenceManager.persist(seviriCoincidence);
                }
            }

            System.out.println();

            // loop over metop observations not yet included in aatsr coincidences
            final List<Observation> metopObservations = inquireObservations(SECONDARY_OBSERVATION_QUERY, METOP_AS_REFERENCE);
            for (Observation metopObservation : metopObservations) {
                //System.out.println(metopObservation);

                // determine corresponding seviri observation if any
                final Observation seviriObservation = findCorrespondingObservation(metopObservation, SEVIRI_SENSOR);
                if (seviriObservation != null) {
                    Coincidence metopCoincidence = createSelfCoincidence(metopObservation);
                    persistenceManager.persist(metopCoincidence);
                    Coincidence seviriCoincidence = createCoincidence(metopObservation, seviriObservation);
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

    private List<Observation> inquireObservations(String queryString, String sensor) {

        final Query query = persistenceManager.createQuery(queryString);
        query.setParameter(1, sensor);
        return (List<Observation>) query.getResultList();
    }

    private Observation findCorrespondingObservation(Observation referenceObservation, String sensor) {

        final Query observationQuery = persistenceManager.createNativeQuery(CORRESPONDING_OBSERVATION_QUERY, Observation.class);
        observationQuery.setParameter(1, referenceObservation.getId());
        observationQuery.setParameter(2, sensor);
        final List<Observation> observations = observationQuery.getResultList();

        if (observations.size() > 0) {
            // select temporally nearest seviri aatsrObservation
            return observations.get(0);
        } else {
            return null;
        }
    }

    private Coincidence createSelfCoincidence(Observation referenceObservation) {

        final Coincidence coincidence = new Coincidence();
        coincidence.setRefObs(referenceObservation);
        coincidence.setObservation(referenceObservation);
        return coincidence;
    }

    private Coincidence createCoincidence(Observation referenceObservation, Observation observation) {

        final Object[] diffs = (Object[]) persistenceManager.pickNative(DISTANCE_QUERY, referenceObservation.getId(), observation.getId());
        final int timeDifference = ((Double) diffs[0]).intValue();
        final float distance = ((Double) diffs[1]).floatValue();

        final Coincidence coincidence = new Coincidence();
        coincidence.setRefObs(referenceObservation);
        coincidence.setObservation(observation);
        coincidence.setDistance(distance);
        coincidence.setTimeDifference(timeDifference);

        System.out.println(String.format("  %d sec, %.3f m: %s-%d %s-%d",
                                         timeDifference, distance,
                                         referenceObservation.getSensor(), referenceObservation.getId(),
                                         observation.getSensor(), observation.getId()));
        return coincidence;
    }
}
