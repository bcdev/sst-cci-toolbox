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
        final List<RelatedObservation> orbitObservations = storage.getRelatedObservations(orbitSensorName, startDate,
                                                                                          stopDate);
        final PolarOrbitingPolygon[] polygons = new PolarOrbitingPolygon[orbitObservations.size()];
        for (int i = 0; i < orbitObservations.size(); ++i) {
            final RelatedObservation orbitObservation = orbitObservations.get(i);
            polygons[i] = new PolarOrbitingPolygon(orbitObservation.getId(),
                                                   orbitObservation.getTime().getTime(),
                                                   orbitObservation.getLocation().getGeometry());
        }
        findObservations(samples, searchTimePastMillis, searchTimeFutureMillis, primarySensor, polygons);
    }

    // package access for testing only tb 2014-04-02
    static void findObservations(List<SamplingPoint> samples, long searchTimePast, long searchTimeFuture,
                                 boolean primarySensor,
                                 PolarOrbitingPolygon... polygons) {
        final List<SamplingPoint> accu = new ArrayList<>(samples.size());
        if (polygons.length > 0) {
            for (final SamplingPoint point : samples) {
                final long pointTime = getPointTime(point, primarySensor);

                // binary search for orbit temporally before (iBefore) and after (iAfter) point
                int iBefore = 0;
                int iAfter = polygons.length - 1;
                while (iBefore + 1 < iAfter) {
                    int i = (iAfter + iBefore) / 2;
                    if (pointTime < polygons[i].getTime()) {
                        iAfter = i;
                    } else {
                        iBefore = i;
                    }
                }

                // find overlapping orbit that is closest in time to the sampling point
                while (iBefore >= 0) {
                    if (pointTime - polygons[iBefore].getTime() <= searchTimePast) {
                        if (polygons[iBefore].isPointInPolygon(point.getLat(), point.getLon())) {
                            break;
                        }
                    }
                    --iBefore;
                }
                final boolean foundBefore = iBefore >= 0;
                while (iAfter < polygons.length) {
                    final long timeDifference = polygons[iAfter].getTime() - pointTime;
                    if (timeDifference <= searchTimeFuture) {
                        if (foundBefore) {
                            final long timeDifferenceBefore = pointTime - polygons[iBefore].getTime();
                            if (timeDifference < timeDifferenceBefore) {
                                if (polygons[iAfter].isPointInPolygon(point.getLat(), point.getLon())) {
                                    assignToSamplingPoint(primarySensor, point, polygons[iAfter]);
                                    accu.add(point);
                                    break;
                                }
                            } else {
                                assignToSamplingPoint(primarySensor, point, polygons[iBefore]);
                                accu.add(point);
                                break;
                            }
                        } else {
                            if (polygons[iAfter].isPointInPolygon(point.getLat(), point.getLon())) {
                                assignToSamplingPoint(primarySensor, point, polygons[iAfter]);
                                accu.add(point);
                                break;
                            }
                        }
                    }
                    ++iAfter;
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

    // package access for testing only tb 2014-04-02
    static long getPointTime(SamplingPoint samplingPoint, boolean primarySensor) {
        if (primarySensor) {
            return samplingPoint.getTime();
        }
        return samplingPoint.getReferenceTime();
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
