package org.esa.cci.sst.tools;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.common.ExtractDefinitionBuilder;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tools.overlap.PolarOrbitingPolygon;
import org.esa.cci.sst.tools.overlap.RegionOverlapFilter;
import org.esa.cci.sst.util.CloudPriors;
import org.esa.cci.sst.util.PixelCounter;
import org.esa.cci.sst.util.ReaderCache;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SobolSequenceGenerator;
import org.esa.cci.sst.util.TimeUtil;
import org.esa.cci.sst.util.Watermask;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;

import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//import org.esa.cci.sst.data.Sample;

public class SamplingTool extends BasicTool {

    private static final byte DATASET_DUMMY = (byte) 8;
    private static final byte REFERENCE_FLAG_UNDEFINED = (byte) 4;
    private static final String MMS_SAMPLING_START_TIME = "mms.sampling.startTime";
    private static final String MMS_SAMPLING_STOP_TIME = "mms.sampling.stopTime";
    private static final String MMS_SAMPLING_COUNT = "mms.sampling.count";
    private static final String MMS_SAMPLING_SENSOR = "mms.sampling.sensor";
    private static final String MMS_SAMPLING_SUBSCENE_WIDTH = "mms.sampling.subscene.width";
    private static final String MMS_SAMPLING_SUBSCENE_HEIGHT = "mms.sampling.subscene.height";
    private static final String MMS_SAMPLING_CLEANUP = "mms.sampling.cleanup";
    private static final String MMS_SAMPLING_CLEANUPINTERVAL = "mms.sampling.cleanupinterval";

    private long startTime;
    private long stopTime;
    private int sampleCount;
    private int subSceneWidth;
    private String samplingSensor;

    private int subSceneHeight;
    private transient Watermask watermask;
    private transient CloudPriors cloudPriors;

    SamplingTool() {
        super("sampling-tool", "1.0");
    }

    public static void main(String[] args) {
        final SamplingTool tool = new SamplingTool();
        try {
            if (!tool.setCommandLineArgs(args)) {
                return;
            }
            tool.initialize();
            tool.run();
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Exception e) {
            tool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.UNKNOWN_ERROR));
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        final String startTimeString = getConfiguration().getProperty(MMS_SAMPLING_START_TIME,
                                                                      "2004-06-01T00:00:00Z");
        final String stopTimeString = getConfiguration().getProperty(MMS_SAMPLING_STOP_TIME,
                                                                     "2004-06-04T00:00:00Z");
        final String countString = getConfiguration().getProperty(MMS_SAMPLING_COUNT, "10000");
        final String subsceneWidthString = getConfiguration().getProperty(MMS_SAMPLING_SUBSCENE_WIDTH, "7");
        final String subsceneHeightString = getConfiguration().getProperty(MMS_SAMPLING_SUBSCENE_HEIGHT, "7");
        samplingSensor = getConfiguration().getProperty(MMS_SAMPLING_SENSOR, "atsr_orb.3");

        try {
            startTime = TimeUtil.parseCcsdsUtcFormat(startTimeString).getTime();
            stopTime = TimeUtil.parseCcsdsUtcFormat(stopTimeString).getTime();
            sampleCount = Integer.parseInt(countString);
            subSceneWidth = Integer.parseInt(subsceneWidthString);
            subSceneHeight = Integer.parseInt(subsceneHeightString);
        } catch (ParseException e) {
            throw new ToolException("Unable to parse sampling start and stop times.", e,
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        } catch (NumberFormatException e) {
            throw new ToolException("Unable to parse sample counts.", e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    private void run() throws ParseException {
        if (Boolean.parseBoolean(getConfiguration().getProperty(MMS_SAMPLING_CLEANUP))) {
            cleanup();
        } else if (Boolean.parseBoolean(getConfiguration().getProperty(MMS_SAMPLING_CLEANUPINTERVAL))) {
            cleanupInterval();
        }
        getLogger().info("Creating samples...");
        final List<SamplingPoint> sampleList = createSamples();
        getLogger().info("Creating samples... " + sampleList.size());
        getLogger().info("Removing land samples...");
        removeLandSamples(sampleList);
        getLogger().info("Removing land samples..." + sampleList.size());
        getLogger().info("Reducing clear samples...");
        reduceClearSamples(sampleList);
        getLogger().info("Reducing clear samples..." + sampleList.size());
        getLogger().info("Finding reference observations...");
        findObservations2(sampleList, samplingSensor);
        getLogger().info("Finding reference observations..." + sampleList.size());
        Collections.sort(sampleList, new Comparator<SamplingPoint>() {
            @Override
            public int compare(SamplingPoint o1, SamplingPoint o2) {
                return Long.compare(o1.getTime(), o2.getTime());
            }
        });
        getLogger().info("Finding satellite sub-scenes...");
        findSatelliteSubscenes(sampleList, samplingSensor);
        getLogger().info("Finding satellite sub-scenes..." + sampleList.size());
        getLogger().info("Removing overlapping areas...");
        removeOverlappingSamples(sampleList);
        getLogger().info("Removing overlapping areas..." + sampleList.size());
        getLogger().info("Creating matchups...");
        createMatchups(sampleList);
        getLogger().info("Creating matchups..." + sampleList.size());
    }

    List<SamplingPoint> createSamples() {
        final SobolSequenceGenerator sequenceGenerator = new SobolSequenceGenerator(4);
        final List<SamplingPoint> sampleList = new ArrayList<SamplingPoint>(sampleCount);

        for (int i = 0; i < sampleCount; i++) {
            final double[] sample = sequenceGenerator.nextVector();
            final double x = sample[0];
            final double y = sample[1];
            final double t = sample[2];
            final double random = sample[3];

            final double lon = x * 360.0 - 180.0;
            final double lat = 90.0 - y * 180.0;
            final long time = (long) (t * (stopTime - startTime)) + startTime;

            sampleList.add(new SamplingPoint(lon, lat, time, random));
        }

        return sampleList;
    }

    void removeLandSamples(List<SamplingPoint> sampleList) {
        if (watermask == null) {
            watermask = new Watermask(); // will always be used in a single thread
        }

        final ArrayList<SamplingPoint> waterSampleList = new ArrayList<>(sampleList.size());
        for (final SamplingPoint point : sampleList) {
            if (watermask.isWater(point.getLon(), point.getLat())) {
                waterSampleList.add(point);
            }
        }

        sampleList.clear();
        sampleList.addAll(waterSampleList);
    }

    void reduceClearSamples(List<SamplingPoint> sampleList) {
        if (cloudPriors == null) {
            cloudPriors = new CloudPriors();
        }

        final ArrayList<SamplingPoint> reducedSampleList = new ArrayList<>(sampleList.size());
        for (final SamplingPoint point : sampleList) {
            // final double f = Math.abs(lat) < 30.0 ? 1.0 : Math.cos(Math.toRadians(Math.abs(lat) - 30.0));
            final double f = 0.05 / cloudPriors.getSample(point.getLon(), point.getLat());
            if (point.getRandom() <= f) {
                reducedSampleList.add(point);
            }
        }

        sampleList.clear();
        sampleList.addAll(reducedSampleList);
    }

    private static final String COINCIDING_OBSERVATION_QUERY =
            "select o.id"
                    + " from mm_observation o"
                    + " where o.sensor = ?1"
                    + " and o.time >= timestamp ?2 - interval '420:00:00' and o.time < timestamp ?2 + interval '420:00:00'"
                    + " and st_intersects(o.location, st_geomfromewkt(?3))"
                    + " order by abs(extract(epoch from o.time) - extract(epoch from timestamp ?2))";

    public void findObservations(List<SamplingPoint> sampleList, String sensor) throws PersistenceException {
        for (Iterator<SamplingPoint> iterator = sampleList.iterator(); iterator.hasNext(); ) {
            final SamplingPoint point = iterator.next();
            final double lon = point.getLon();
            final double lat = point.getLat();
            final long time = point.getTime();

            // since binding a date to a parameter failed ...
            final String queryString2 = COINCIDING_OBSERVATION_QUERY.replaceAll("\\?2",
                                                                                "'" + TimeUtil.formatCcsdsUtcFormat(
                                                                                        new Date(time)) + "'");
            final Query query = getPersistenceManager().createNativeQuery(queryString2, ReferenceObservation.class);
            query.setParameter(1, sensor);
            //query.setParameter("time", new Date(time), TemporalType.TIMESTAMP);
            query.setParameter(3, String.format("POINT(%.4f %.4f)", lon, lat));
            query.setMaxResults(1);

            ReferenceObservation nearestCoveringObservation = null;
            @SuppressWarnings({"unchecked"})
            final List<? extends ReferenceObservation> observations = query.getResultList();
            if (observations.isEmpty()) {
                iterator.remove();
                continue;
            }
            // select temporally nearest common observation
            nearestCoveringObservation = observations.get(0);
            point.setReference(nearestCoveringObservation.getId());
//            point.setTime(nearestCoveringObservation.getTime().getTime());
        }
    }

    public void findObservations2(List<SamplingPoint> sampleList, String samplingSensor) throws PersistenceException, ParseException {
        final List<ReferenceObservation> orbitObservations = findOrbits(samplingSensor,
                TimeUtil.formatCcsdsUtcFormat(new Date(startTime - 86400 * 100 * 175)),
                TimeUtil.formatCcsdsUtcFormat(new Date(stopTime + 86400 * 100 * 175)));
        final PolarOrbitingPolygon[] polygons = new PolarOrbitingPolygon[orbitObservations.size()];
        for (int i = 0; i < orbitObservations.size(); ++i) {
            final ReferenceObservation orbitObservation = orbitObservations.get(i);
            polygons[i] = new PolarOrbitingPolygon(orbitObservation.getId(), orbitObservation.getTime().getTime(),
                                                   orbitObservation.getLocation().getGeometry());
        }
        final List<SamplingPoint> accu = new ArrayList<SamplingPoint>(sampleList.size());
        for (Iterator<SamplingPoint> iterator = sampleList.iterator(); iterator.hasNext(); ) {
            final SamplingPoint point = iterator.next();
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
                if (i0 >= 0 && (i1 >= polygons.length || point.getTime() < polygons[i0].getTime() || point.getTime() - polygons[i0].getTime() < polygons[i1].getTime() - point.getTime())) {
                    if (polygons[i0].isPointInPolygon(point.getLat(), point.getLon())) {
                        point.setReference(polygons[i0].getId());
                        accu.add(point);
                        break;
                    }
                    --i0;
                } else if (i1 < polygons.length) {
                    if (polygons[i1].isPointInPolygon(point.getLat(), point.getLon())) {
                        point.setReference(polygons[i1].getId());
                        accu.add(point);
                        break;
                    }
                    ++i1;
                } else {
                    break;
                }
            }
        }
        sampleList.clear();
        sampleList.addAll(accu);
    }

    private static final String SENSOR_OBSERVATION_QUERY =
            "select o.id"
                    + " from mm_observation o"
                    + " where o.sensor = ?1"
                    + " and o.time >= timestamp ?2 and o.time < timestamp ?3"
                    + " order by o.time, o.id";

    List<ReferenceObservation> findOrbits(String sensor, String startTimeString, String stopTimeString) throws ParseException {
        //Date startTime = new Date(TimeUtil.parseCcsdsUtcFormat(startTimeString).getTime());
        //Date stopTime = new Date(TimeUtil.parseCcsdsUtcFormat(stopTimeString).getTime());
        final String queryString2 = SENSOR_OBSERVATION_QUERY.replaceAll("\\?2", "'" + startTimeString + "'").replaceAll(
                "\\?3", "'" + stopTimeString + "'");
        final Query query = getPersistenceManager().createNativeQuery(queryString2, ReferenceObservation.class);
        query.setParameter(1, sensor);
        //query.setParameter(2, startTime);
        //query.setParameter(3, stopTime);
        return query.getResultList();
    }

    void findSatelliteSubscenes(List<SamplingPoint> sampleList, String sensor) {
        // TODO - make parameters from variables below
        final String cloudFlagsName = "cloud_flags_nadir";
        final int pixelMask = 3;
        final double cloudyPixelFraction = 0.0;

        final Query columnQuery = getPersistenceManager().createQuery("select c from Column c where c.name = ?1");
        final String columnName = sensor + "." + cloudFlagsName;
        columnQuery.setParameter(1, columnName);

        final Object columnQueryResult = columnQuery.getSingleResult();
        if (!(columnQueryResult instanceof Column)) {
            throw new ToolException("No such column '" + columnName + "'.", ToolException.UNKNOWN_ERROR);
        }
        final Column column = (Column) columnQueryResult;
        final Number fillValue = column.getFillValue();
        final PixelCounter pixelCounter = new PixelCounter(pixelMask, fillValue);

        final Map<Integer, List<SamplingPoint>> sampleListsByDatafile = new HashMap<Integer, List<SamplingPoint>>();
        for (final SamplingPoint point : sampleList) {
            final int id = point.getReference();

            if (!sampleListsByDatafile.containsKey(id)) {
                sampleListsByDatafile.put(id, new ArrayList<SamplingPoint>());
            }
            sampleListsByDatafile.get(id).add(point);
        }

        final ReaderCache readerCache = new ReaderCache(10, getConfiguration(), getLogger());
        final int[] shape = {1, subSceneHeight, subSceneWidth};
        final ExtractDefinitionBuilder builder = new ExtractDefinitionBuilder().shape(shape).fillValue(fillValue);

        final List<SamplingPoint> accu = new ArrayList<SamplingPoint>(sampleList.size());
        for (final int id : sampleListsByDatafile.keySet()) {
            final List<SamplingPoint> points = sampleListsByDatafile.get(id);

            final Observation observation = getObservation(id);
            if (observation != null) {
                final DataFile datafile = observation.getDatafile();
                try {
                    final Reader reader = readerCache.getReader(datafile, true);
                    for (final SamplingPoint point : points) {
                        final double lat = point.getLat();
                        final double lon = point.getLon();

                        final GeoCoding geoCoding = reader.getGeoCoding(0);
                        final GeoPos geoPos = new GeoPos((float) lat, (float) lon);
                        final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, new PixelPos());

                        final int numCols = reader.getElementCount();
                        final int numRows = reader.getScanLineCount();
                        final int pixelX = (int) Math.floor(pixelPos.getX());
                        final int pixelY = (int) Math.floor(pixelPos.getY());

                        if (pixelPos.isValid() && pixelX >= 0 && pixelY >= 0 && pixelX < numCols && pixelY < numRows) {
                            point.setX(pixelX);
                            point.setY(pixelY);
                            point.setTime(reader.getTime(0, pixelY));

                            final ExtractDefinition extractDefinition = builder.lat(lat).lon(lon).build();
                            final Array array = reader.read(cloudFlagsName, extractDefinition);
                            final int cloudyPixelCount = pixelCounter.count(array);
                            if (cloudyPixelCount <= (subSceneWidth * subSceneHeight) * cloudyPixelFraction) {
                                accu.add(point);
                            }
                        } else {
                            final String message = MessageFormat.format(
                                    "Cannot find pixel at ({0}, {1}) in datafile ''{2}''.", lon, lat,
                                    datafile.getPath());
                            getLogger().fine(message);
                        }
                    }
                } catch (IOException e) {
                    throw new ToolException(
                            MessageFormat.format("Cannot read data file ''{0}''.", datafile.getPath()), e,
                            ToolException.TOOL_IO_ERROR);
                } finally {
                    readerCache.closeReader(datafile);
                }
            }
        }
        sampleList.clear();
        sampleList.addAll(accu);
    }

    public void removeOverlappingSamples(List<SamplingPoint> sampleList) {
        final RegionOverlapFilter regionOverlapFilter = new RegionOverlapFilter(subSceneWidth, subSceneHeight);
        final List<SamplingPoint> filteredList = regionOverlapFilter.filterOverlaps(sampleList);
        sampleList.clear();
        sampleList.addAll(filteredList);
    }

    void createMatchups(List<SamplingPoint> sampleList) {
        final long pattern = getSensor(samplingSensor).getPattern();

        final ArrayList<ReferenceObservation> referenceObservationList = new ArrayList<>();
        final ArrayList<Matchup> matchupList = new ArrayList<>();
        final ArrayList<Coincidence> coincidenceList = new ArrayList<>();

        final PersistenceManager persistenceManager = getPersistenceManager();
        try {
            int j = 200;
            persistenceManager.transaction();
            for (SamplingPoint samplingPoint : sampleList) {
                final ReferenceObservation referenceObservation = new ReferenceObservation();
                referenceObservation.setName("0123");
                referenceObservation.setSensor("sobol");
                final PGgeometry location = new PGgeometry(new Point(samplingPoint.getLon(), samplingPoint.getLat()));
                referenceObservation.setLocation(location);
                referenceObservation.setPoint(location);
                final Date time = new Date(samplingPoint.getTime());
                referenceObservation.setTime(time);
                referenceObservation.setTimeRadius(0.0);
                final Observation observation = getObservation(samplingPoint.getReference());
                referenceObservation.setDatafile(observation.getDatafile());
                referenceObservation.setRecordNo(0);
                referenceObservation.setDataset(DATASET_DUMMY);
                referenceObservation.setReferenceFlag(REFERENCE_FLAG_UNDEFINED);

                referenceObservationList.add(referenceObservation);
                persistenceManager.persist(referenceObservation);
                if (--j == 0) {
                    persistenceManager.commit();
                    persistenceManager.transaction();
                    j = 200;
                }
            }
            persistenceManager.commit();

            persistenceManager.transaction();
            for (int i = 0; i < sampleList.size(); i++) {
                final SamplingPoint samplingPoint = sampleList.get(i);
                final ReferenceObservation referenceObservation = referenceObservationList.get(i);
                final Matchup matchup = new Matchup();
                matchup.setId(referenceObservation.getId());
                matchup.setRefObs(referenceObservation);
                matchup.setPattern(pattern);

                matchupList.add(matchup);

                final Observation observation = getObservation(samplingPoint.getReference());
                final Coincidence coincidence = new Coincidence();
                coincidence.setMatchup(matchup);
                coincidence.setObservation(observation);
                coincidence.setTimeDifference(0.0);

                coincidenceList.add(coincidence);
            }
            persistenceManager.commit();

            persistenceManager.transaction();
            for (Matchup m : matchupList) {
                persistenceManager.persist(m);
            }
            for (Coincidence c : coincidenceList) {
                persistenceManager.persist(c);
            }
            persistenceManager.commit();
        } catch (Exception e) {
            persistenceManager.rollback();
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    void cleanup() {
        getPersistenceManager().transaction();

        Query delete = getPersistenceManager().createQuery("delete from Coincidence c");
        delete.executeUpdate();
        delete = getPersistenceManager().createQuery("delete from Matchup m");
        delete.executeUpdate();
        delete = getPersistenceManager().createQuery("delete from Observation o where o.sensor = 'sobol'");
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

    void cleanupInterval() {
        getPersistenceManager().transaction();

        Query delete = getPersistenceManager().createNativeQuery(
                "delete from mm_coincidence c where exists ( select r.id from mm_observation r where c.matchup_id = r.id and r.time >= ?1 and r.time < ?2 and r.sensor = 'sobol')");
        delete.setParameter(1, new Date(startTime));
        delete.setParameter(2, new Date(stopTime));
        delete.executeUpdate();
        delete = getPersistenceManager().createNativeQuery(
                "delete from mm_matchup m where exists ( select r from mm_observation r where m.refobs_id = r.id and r.time >= ?1 and r.time < ?2 and r.sensor = 'sobol')");
        delete.setParameter(1, new Date(startTime));
        delete.setParameter(2, new Date(stopTime));
        delete.executeUpdate();
        delete = getPersistenceManager().createNativeQuery(
                "delete from mm_observation r where r.time >= ?1 and r.time < ?2 and r.sensor = 'sobol'");
        delete.setParameter(1, new Date(startTime));
        delete.setParameter(2, new Date(stopTime));
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

}

