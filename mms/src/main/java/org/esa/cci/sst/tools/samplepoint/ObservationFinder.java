package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.tools.overlap.PolarOrbitingPolygon;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SensorNames;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ObservationFinder {

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
     * @param samples   The list of sampling points.
     * @param parameter The parameter object
     */
    public void findPrimarySensorObservations(List<SamplingPoint> samples, Parameter parameter) {
        findObservations(samples, parameter, true);
    }

    /**
     * Finds related observations for each sampling point in a list of samples. On return any
     * sampling points, which could not be associated with a related observations are removed
     * from the list of samples.
     * </p>
     * To be used for dual-sensor matchups only.
     *
     * @param samples   The list of sampling points.
     * @param parameter The parameter object
     */
    public void findSecondarySensorObservations(List<SamplingPoint> samples, Parameter parameter) {
        findObservations(samples, parameter, false);
    }

    public void findObservations(List<SamplingPoint> samples, Parameter parameter, boolean primarySensor) {
        final long searchTimePastMillis = parameter.getSearchTimePast() * 1000;
        final long searchTimeFutureMillis = parameter.getSearchTimeFuture() * 1000;
        final Date startDate = new Date(parameter.getStartTime() - searchTimePastMillis);
        final Date stopDate = new Date(parameter.getStopTime() + searchTimeFutureMillis);
        final String orbitSensorName = SensorNames.ensureOrbitName(parameter.getSensorName());

        final Storage storage = persistenceManager.getStorage();
        final List<RelatedObservation> orbitObservations = storage.getRelatedObservations(orbitSensorName, startDate, stopDate);
        final PolarOrbitingPolygon[] polygons = new PolarOrbitingPolygon[orbitObservations.size()];
        for (int i = 0; i < orbitObservations.size(); ++i) {
            final RelatedObservation orbitObservation = orbitObservations.get(i);
            polygons[i] = new PolarOrbitingPolygon(orbitObservation.getId(),
                    orbitObservation.getTime().getTime(),
                    orbitObservation.getLocation().getGeometry());
        }
        findObservations(samples, searchTimePastMillis, primarySensor, polygons);
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
                            assignToSamplingPoint(primarySensor, point, polygons[i0]);
                            accu.add(point);
                            break;
                        }
                        --i0;
                    } else
                        // the next polygon in the future is closer than the next polygon in the past
                        if (i1 < polygons.length &&
                                Math.abs(point.getTime() - polygons[i1].getTime()) <= halfRevisitTimeMillis) {
                            if (polygons[i1].isPointInPolygon(point.getLat(), point.getLon())) {
                                assignToSamplingPoint(primarySensor, point, polygons[i1]);
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

    // package access for testing only tb 2014-04-01
    static void assignToSamplingPoint(boolean primarySensor, SamplingPoint point, PolarOrbitingPolygon polygon) {
        if (primarySensor) {
            point.setReference(polygon.getId());
            // original time and orbit time may fall into different months
            // we need the orbit time (and not the original time) when exporting samples
            point.setReferenceTime(polygon.getTime());
        } else {
            point.setReference2(polygon.getId());
            point.setReference2Time(polygon.getTime());
        }
    }

    public static class Parameter {
        private String sensorName;
        private long startTime;
        private long stopTime;
        private int searchTimeFuture;
        private int searchTimePast;

        public void setSensorName(String sensorName) {
            this.sensorName = sensorName;
        }

        public String getSensorName() {
            return sensorName;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public void setStopTime(long stopTime) {
            this.stopTime = stopTime;
        }

        public long getStopTime() {
            return stopTime;
        }

        public void setSearchTimeFuture(int searchTimeFuture) {
            this.searchTimeFuture = searchTimeFuture;
        }

        public int getSearchTimeFuture() {
            return searchTimeFuture;
        }

        public void setSearchTimePast(int searchTimePast) {
            this.searchTimePast = searchTimePast;
        }

        public int getSearchTimePast() {
            return searchTimePast;
        }
    }
}
