package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.tools.overlap.PolarOrbitingPolygon;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SensorNames;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ObservationFinder {

    private final Storage storage;

    public ObservationFinder(Storage storage) {
        this.storage = storage;
    }

    /**
     * Finds related observations for each sampling point in a list of samples. On return any
     * sampling points, which could not be associated with a related observations are removed
     * from the list of samples.
     * <p/>
     * To be used for single-sensor and dual-sensor matchups.
     *
     * @param samples    The list of sampling points.
     * @param sensor     The sensor name.
     * @param startTime  The start time.
     * @param stopTime   The stop time.
     * @param searchTime May time delta between insitu and sensor
     */
    public void findPrimarySensorObservations(List<SamplingPoint> samples, String sensor,
                                              long startTime, long stopTime, int searchTime) {
        findObservations(samples, sensor, startTime, stopTime, searchTime, true);
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
                                 long startTime, long stopTime, int searchTimeDeltaSeconds, boolean primarySensor) {
        final long halfRevisitTimeMillis = searchTimeDeltaSeconds * 1000;
        final Date startDate = new Date(startTime - halfRevisitTimeMillis);
        final Date stopDate = new Date(stopTime + halfRevisitTimeMillis);
        final String orbitSensorName = SensorNames.ensureOrbitName(sensorName);

        final List<RelatedObservation> orbitObservations = storage.getRelatedObservations(orbitSensorName, startDate, stopDate);
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
        if (polygons.length > 0) {
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
                                point.setTime(polygons[i0].getTime());
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
                                    point.setTime(polygons[i1].getTime());
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
        }
        samples.clear();
        samples.addAll(accu);
    }
}
