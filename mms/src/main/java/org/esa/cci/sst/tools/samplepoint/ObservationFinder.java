package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.overlap.PolarOrbitingPolygon;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ObservationFinder {

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

    public void findObservations(List<SamplingPoint> samples, String sensor, boolean secondSensor,
                                 long startTime, long stopTime, int searchTimeDeltaSeconds) {

        final long searchTimeDeltaMillis = searchTimeDeltaSeconds * 1000;
        final Date startDate = new Date(startTime - searchTimeDeltaMillis);
        final Date stopDate = new Date(stopTime + searchTimeDeltaMillis);
        final List<ReferenceObservation> orbitObservations = findOrbits(sensor, startDate, stopDate);
        final PolarOrbitingPolygon[] polygons = new PolarOrbitingPolygon[orbitObservations.size()];
        for (int i = 0; i < orbitObservations.size(); ++i) {
            final ReferenceObservation orbitObservation = orbitObservations.get(i);
            polygons[i] = new PolarOrbitingPolygon(orbitObservation.getId(),
                                                   orbitObservation.getTime().getTime(),
                                                   orbitObservation.getLocation().getGeometry());
        }
        findObservations(samples, secondSensor, searchTimeDeltaMillis, polygons);
    }

    public static void findObservations(List<SamplingPoint> samples, boolean secondSensor, long searchTimeDelta,
                                 PolarOrbitingPolygon[] polygons) {
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
                    Math.abs(point.getTime() - polygons[i0].getTime()) <= searchTimeDelta &&
                    (i1 >= polygons.length ||
                     point.getTime() < polygons[i0].getTime() ||
                     point.getTime() - polygons[i0].getTime() < polygons[i1].getTime() - point.getTime())) {
                    if (polygons[i0].isPointInPolygon(point.getLat(), point.getLon())) {
                        if (!secondSensor) {
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
                        Math.abs(point.getTime() - polygons[i1].getTime()) <= searchTimeDelta) {
                        if (polygons[i1].isPointInPolygon(point.getLat(), point.getLon())) {
                            if (!secondSensor) {
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

    public List<ReferenceObservation> findOrbits(String sensor, Date startDate, Date stopDate) {
        final String s1 = TimeUtil.formatCcsdsUtcFormat(startDate);
        final String s2 = TimeUtil.formatCcsdsUtcFormat(stopDate);
        final String queryString = SENSOR_OBSERVATION_QUERY_TEMPLATE_STRING
                .replaceAll("\\?2", "'" + s1 + "'")
                .replaceAll("\\?3", "'" + s2 + "'");
        final Query query = persistenceManager.createNativeQuery(queryString, ReferenceObservation.class);
        query.setParameter(1, sensor);

        //noinspection unchecked
        return query.getResultList();
    }

}
