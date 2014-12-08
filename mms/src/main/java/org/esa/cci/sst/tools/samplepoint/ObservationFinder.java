package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.tools.overlap.PolarOrbitingPolygon;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SensorNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
        final String orbitSensorName = SensorNames.getOrbitName(parameter.getSensorName());

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
            // do not trust database sorting
            Arrays.sort(polygons, new Comparator<PolarOrbitingPolygon>() {
                @Override
                public int compare(PolarOrbitingPolygon o1, PolarOrbitingPolygon o2) {
                    return Long.compare(o1.getTime(), o2.getTime());
                }
            });
            final long[] polygonTimes = new long[polygons.length];
            for (int i = 0; i < polygons.length; i++) {
                polygonTimes[i] = polygons[i].getTime();
            }

            for (final SamplingPoint point : samples) {
                final long pointTime = getPointTime(point, primarySensor);

                // binary search for orbit temporally before or at the same time (iBefore) and after (iAfter) point
                int iBefore = binarySearch(polygonTimes, pointTime);
                int iAfter = iBefore + 1;

                // find overlapping orbit that is closest in time to the sampling point
                final double pointLat = point.getLat();
                final double pointLon = point.getLon();
                while (iBefore >= 0) {
                    if (pointTime - polygonTimes[iBefore] <= searchTimePast) {
                        if (polygons[iBefore].isPointInPolygon(pointLat, pointLon)) {
                            break;
                        }
                    }
                    --iBefore;
                }
                while (iAfter < polygons.length) {
                    if (polygonTimes[iAfter] - pointTime <= searchTimeFuture) {
                        if (polygons[iAfter].isPointInPolygon(pointLat, pointLon)) {
                            break;
                        }
                    }
                    ++iAfter;
                }
                final boolean foundBefore = iBefore >= 0;
                final boolean foundAfter = iAfter < polygons.length;
                if (foundBefore) {
                    if (foundAfter) {
                        if (pointTime - polygonTimes[iBefore] < polygonTimes[iAfter] - pointTime) {
                            assignToSamplingPoint(primarySensor, point, polygons[iBefore]);
                        } else {
                            assignToSamplingPoint(primarySensor, point, polygons[iAfter]);
                        }
                    } else {
                        assignToSamplingPoint(primarySensor, point, polygons[iBefore]);
                    }
                    accu.add(point);
                } else if (foundAfter) {
                    assignToSamplingPoint(primarySensor, point, polygons[iAfter]);
                    accu.add(point);
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

    // package access for testing only rq 2014-04-15
    static int binarySearch(long[] values, long value) {
        int low = 0;
        int high = values.length - 1;

        while (low <= high) {
            final int mid = (high + low) >> 1;
            final long midValue = values[mid];
            if (midValue <= value) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return high;
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

        // search time range into the future in seconds
        public int getSearchTimeFuture() {
            return searchTimeFuture;
        }

        // search time range into the past in seconds
        public void setSearchTimePast(int searchTimePast) {
            this.searchTimePast = searchTimePast;
        }

        public int getSearchTimePast() {
            return searchTimePast;
        }
    }
}
