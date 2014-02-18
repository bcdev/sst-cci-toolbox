package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.overlap.PolarOrbitingPolygon;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ObservationFinder {

    // rq-20140217 - do not delete, might be useful later
    private static final String COINCIDING_OBSERVATION_QUERY_TEMPLATE_STRING =
            "select o.id"
            + " from mm_observation o"
            + " where o.sensor = ?1"
            + " and o.time >= timestamp ?2 - interval '420:00:00' and o.time < timestamp ?2 + interval '420:00:00'"
            + " and st_intersects(o.location, st_geomfromewkt(?3))"
            + " order by abs(extract(epoch from o.time) - extract(epoch from timestamp ?2))";

    private static final String SENSOR_OBSERVATION_QUERY_TEMPLATE_STRING =
            "select o.id"
            + " from mm_observation o"
            + " where o.sensor = ?1"
            + " and o.time >= timestamp ?2 and o.time < timestamp ?3"
            + " order by o.time, o.id";

    private final PersistenceManager persistenceManager;

    public ObservationFinder(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    /**
     * Finds related observations for each sampling point in a list of samples. On return any
     * sampling points, which could not be associated with a related observations are removed
     * from the list of samples.
     * <p/>
     * To be used for single-sensor and dual-sensor matchups.
     *
     * @param samples         The list of sampling points.
     * @param sensor          The sensor name.
     * @param startTime       The start time.
     * @param stopTime        The stop time.
     * @param halfRevisitTime Half the sensor revisit time (seconds).
     */
    public void findPrimarySensorObservations(List<SamplingPoint> samples, String sensor,
                                              long startTime, long stopTime, int halfRevisitTime) {
        findObservations(samples, sensor, startTime, stopTime, halfRevisitTime, true);
    }

    /**
     * Finds related observations for each sampling point in a list of samples. On return any
     * sampling points, which could not be associated with a related observations are removed
     * from the list of samples.
     * <p/>
     * To be used for dual-sensor matchups only.
     *
     * @param samples                The list of sampling points.
     * @param sensorName             The sensor name.
     * @param startTime              The start time.
     * @param stopTime               The stop time.
     * @param searchTimeDeltaSeconds The tolerable time difference (seconds).
     */
    public void findSecondarySensorObservations(List<SamplingPoint> samples, String sensorName,
                                                long startTime, long stopTime, int searchTimeDeltaSeconds) {
        findObservations(samples, sensorName, startTime, stopTime, searchTimeDeltaSeconds, false);
    }

    public void findObservations(List<SamplingPoint> samples, String sensorName,
                                 long startTime, long stopTime, int halfRevisitTime, boolean primarySensor) {
        final long halfRevisitTimeMillis = halfRevisitTime * 1000;
        final Date startDate = new Date(startTime - halfRevisitTimeMillis);
        final Date stopDate = new Date(stopTime + halfRevisitTimeMillis);
        final List<RelatedObservation> orbitObservations = findOrbitObservations(sensorName, startDate, stopDate);
        final PolarOrbitingPolygon[] polygons = new PolarOrbitingPolygon[orbitObservations.size()];
        for (int i = 0; i < orbitObservations.size(); ++i) {
            final RelatedObservation orbitObservation = orbitObservations.get(i);
            polygons[i] = new PolarOrbitingPolygon(orbitObservation.getId(),
                                                   orbitObservation.getTime().getTime(),
                                                   orbitObservation.getLocation().getGeometry());
        }
        findObservations(samples, halfRevisitTimeMillis, primarySensor, polygons);
    }

    public static void findObservations(List<SamplingPoint> samples, long halfRevisitTimeMillis, boolean primarySensor,
                                        PolarOrbitingPolygon... polygons) {
        final List<SamplingPoint> accu = new ArrayList<>(samples.size());
        for (final SamplingPoint point : samples) {
            // look for orbit temporally before (i0) and after (i1) point with binary search
            int i0 = 0;
            int i1 = polygons.length - 1;
            while (i0 + 1 < i1) {
                int i = (i1 + i0) / 2;
                if (point.getTime() < polygons[i].getTime()) {
                    i1 = i;
                } else {
                    i0 = i;
                }
            }
            // check orbitObservations temporally closest to point first for spatial overlap
            while (true) {
                // the next polygon in the past is closer to the sample than the next polygon in the future
                if (i0 >= 0 &&
                    Math.abs(point.getTime() - polygons[i0].getTime()) <= halfRevisitTimeMillis &&
                    (i1 >= polygons.length ||
                     point.getTime() < polygons[i0].getTime() ||
                     point.getTime() - polygons[i0].getTime() < polygons[i1].getTime() - point.getTime())) {
                    if (polygons[i0].isPointInPolygon(point.getLat(), point.getLon())) {
                        if (primarySensor) {
                            point.setReference(polygons[i0].getId());
                        } else {
                            point.setReference2(polygons[i0].getId());
                        }
                        accu.add(point);
                        break;
                    }
                    --i0;
                } else
                    // the next polygon in the future is closer than the next polygon in the past
                    if (i1 < polygons.length &&
                        Math.abs(point.getTime() - polygons[i1].getTime()) <= halfRevisitTimeMillis) {
                        if (polygons[i1].isPointInPolygon(point.getLat(), point.getLon())) {
                            if (primarySensor) {
                                point.setReference(polygons[i1].getId());
                            } else {
                                point.setReference2(polygons[i1].getId());
                            }
                            accu.add(point);
                            break;
                        }
                        ++i1;
                    } else
                    // there is no next polygon in the past and no next polygon in the future
                    {
                        break;
                    }
            }
        }
        samples.clear();
        samples.addAll(accu);
    }

    private List<RelatedObservation> findOrbitObservations(String sensor, Date startDate, Date stopDate) {
        final String s1 = TimeUtil.formatCcsdsUtcFormat(startDate);
        final String s2 = TimeUtil.formatCcsdsUtcFormat(stopDate);
        final String queryString = SENSOR_OBSERVATION_QUERY_TEMPLATE_STRING
                .replaceAll("\\?2", "'" + s1 + "'")
                .replaceAll("\\?3", "'" + s2 + "'");
        final Query query = persistenceManager.createNativeQuery(queryString, RelatedObservation.class);
        query.setParameter(1, sensor);

        //noinspection unchecked
        return query.getResultList();
    }

}
