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
import org.esa.cci.sst.tools.overlap.RegionOverlapFilter;
import org.esa.cci.sst.tools.samplepoint.ClearSkyPointRemover;
import org.esa.cci.sst.tools.samplepoint.LandPointRemover;
import org.esa.cci.sst.tools.samplepoint.ObservationFinder;
import org.esa.cci.sst.tools.samplepoint.SobolSamplePointGenerator;
import org.esa.cci.sst.util.PixelCounter;
import org.esa.cci.sst.util.ReaderCache;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;

import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SamplingTool extends BasicTool {

    private static final byte DATASET_DUMMY = (byte) 8;
    private static final byte REFERENCE_FLAG_UNDEFINED = (byte) 4;

    private static final String MMS_SAMPLING_CLEANUP = "mms.sampling.cleanup";
    private static final String MMS_SAMPLING_CLEANUPINTERVAL = "mms.sampling.cleanupinterval";
    private static final String MMS_SAMPLING_SENSOR2 = "mms.sampling.sensor2";
    private static final String MMS_SAMPLING_MATCHUPDISTANCE = "mms.sampling.matchupdistance";

    private long startTime;
    private long stopTime;
    private int sampleCount;
    private int sampleSkip;
    private int subSceneWidth;
    private String samplingSensor;

    private int subSceneHeight;
    private String samplingSensor2;
    private int matchupDistanceSeconds;

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

        final Configuration config = getConfig();
        startTime = config.getDateValue(Configuration.KEY_MMS_SAMPLING_START_TIME, "2004-06-01T00:00:00Z").getTime();
        stopTime = config.getDateValue(Configuration.KEY_MMS_SAMPLING_STOP_TIME, "2004-06-04T00:00:00Z").getTime();
        sampleCount = config.getIntValue(Configuration.KEY_MMS_SAMPLING_COUNT, 10000);
        sampleSkip = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SKIP, 0);
        matchupDistanceSeconds = config.getIntValue(MMS_SAMPLING_MATCHUPDISTANCE, 90000);
        subSceneWidth = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SUBSCENE_WIDTH, 7);
        subSceneHeight = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SUBSCENE_HEIGHT, 7);

        samplingSensor = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR, "atsr_orb.3");
        samplingSensor2 = config.getStringValue(MMS_SAMPLING_SENSOR2);
    }

    private void run() throws ParseException {
        final Configuration config = getConfig();
        if (config.getBooleanValue(MMS_SAMPLING_CLEANUP)) {
            cleanup();
        } else if (config.getBooleanValue(MMS_SAMPLING_CLEANUPINTERVAL)) {
            cleanupInterval();
        }

        getLogger().info("Creating samples...");
        final List<SamplingPoint> sampleList = createSamples(sampleCount, sampleSkip, startTime, stopTime);
        getLogger().info("Creating samples... " + sampleList.size());

        getLogger().info("Removing land samples...");
        removeLandSamples(sampleList);
        getLogger().info("Removing land samples..." + sampleList.size());

        getLogger().info("Reducing clear samples...");
        reduceByClearSkyStatistic(sampleList);
        getLogger().info("Reducing clear samples..." + sampleList.size());

        getLogger().info("Finding reference observations...");
        final int halfRepeatCycleInSeconds = 86400 * 175 / 10;
        findObservations2(sampleList, samplingSensor, false, halfRepeatCycleInSeconds, getPersistenceManager(),
                          startTime, stopTime);
        getLogger().info("Finding reference observations..." + sampleList.size());
        Collections.sort(sampleList, new Comparator<SamplingPoint>() {
            @Override
            public int compare(SamplingPoint o1, SamplingPoint o2) {
                return Long.compare(o1.getTime(), o2.getTime());
            }
        });

        // SNIP ---------------------------------
        // - list of points - done - json file
        // - sampling sensor cmdLineParam

        getLogger().info("Finding satellite sub-scenes...");
        findSatelliteSubscenes(sampleList, samplingSensor, false);
        getLogger().info("Finding satellite sub-scenes..." + sampleList.size());
        if (samplingSensor2 != null) {
            getLogger().info("Finding " + samplingSensor2 + " observations...");
            findObservations2(sampleList, samplingSensor2, true, matchupDistanceSeconds, getPersistenceManager(),
                              startTime, stopTime);
            getLogger().info("Finding " + samplingSensor2 + " observations..." + sampleList.size());

            getLogger().info("Finding " + samplingSensor2 + " sub-scenes...");
            findSatelliteSubscenes(sampleList, samplingSensor2, true);
            getLogger().info("Finding " + samplingSensor2 + " sub-scenes..." + sampleList.size());
        }

        getLogger().info("Removing overlapping areas...");
        removeOverlappingSamples(sampleList);
        getLogger().info("Removing overlapping areas..." + sampleList.size());

        getLogger().info("Creating matchups...");
        createMatchups(sampleList, samplingSensor, samplingSensor2);
        getLogger().info("Creating matchups..." + sampleList.size());
    }

    static List<SamplingPoint> createSamples(int sampleCount, int sampleSkip, long startTime, long stopTime) {
        return new SobolSamplePointGenerator().createSamples(sampleCount, sampleSkip, startTime, stopTime);
    }

    static void removeLandSamples(List<SamplingPoint> sampleList) {
        new LandPointRemover().removeSamples(sampleList);
    }

    static void reduceByClearSkyStatistic(List<SamplingPoint> sampleList) {
        new ClearSkyPointRemover().removeSamples(sampleList);
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

            ReferenceObservation nearestCoveringObservation;
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

    public static void findObservations2(List<SamplingPoint> sampleList, String samplingSensor, boolean isSecondSensor,
                                         int searchRadiusSeconds, PersistenceManager persistenceManager,
                                         long startTime, long stopTime) throws PersistenceException, ParseException {
        new ObservationFinder(persistenceManager).findObservations(sampleList, samplingSensor, isSecondSensor,
                                                                   startTime, stopTime, searchRadiusSeconds);
    }

    void findSatelliteSubscenes(List<SamplingPoint> sampleList, String sensor, boolean isSecondSensor) {
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

        final Map<Integer, List<SamplingPoint>> sampleListsByDatafile = new HashMap<>();
        for (final SamplingPoint point : sampleList) {
            final int id = isSecondSensor ? point.getReference2() : point.getReference();

            if (!sampleListsByDatafile.containsKey(id)) {
                sampleListsByDatafile.put(id, new ArrayList<SamplingPoint>());
            }
            sampleListsByDatafile.get(id).add(point);
        }

        final ReaderCache readerCache = new ReaderCache(10, getConfig(), getLogger());
        final int[] shape = {1, subSceneHeight, subSceneWidth};
        final ExtractDefinitionBuilder builder = new ExtractDefinitionBuilder().shape(shape).fillValue(fillValue);

        final List<SamplingPoint> accu = new ArrayList<>(sampleList.size());
        final Integer[] datafileIds = sampleListsByDatafile.keySet().toArray(
                new Integer[sampleListsByDatafile.keySet().size()]);
        Arrays.sort(datafileIds);
        for (final int id : datafileIds) {
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
                            if (!isSecondSensor) {
                                point.setX(pixelX);
                                point.setY(pixelY);
                                // @todo 2 tb/** check what the consequences are if we use in-situ data here
                                point.setTime(reader.getTime(0, pixelY));
                            }

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

    void createMatchups(List<SamplingPoint> sampleList, String samplingSensor, String samplingSensor2) {
        long pattern = getSensor(samplingSensor).getPattern();
        if (samplingSensor2 != null) {
            pattern |= getSensor(samplingSensor2).getPattern();
        }

        final ArrayList<ReferenceObservation> referenceObservationList = new ArrayList<>();
        final ArrayList<Matchup> matchupList = new ArrayList<>();
        final ArrayList<Coincidence> coincidenceList = new ArrayList<>();

        final PersistenceManager persistenceManager = getPersistenceManager();
        try {
            int j = 200;
            persistenceManager.transaction();
            for (SamplingPoint samplingPoint : sampleList) {
                final ReferenceObservation referenceObservation = new ReferenceObservation();
                // @todo 2 tb/** make this configurable tb 2014-02-12
                referenceObservation.setName("0123");
                referenceObservation.setSensor("sobol");

                final PGgeometry location = new PGgeometry(new Point(samplingPoint.getLon(), samplingPoint.getLat()));
                referenceObservation.setLocation(location);
                referenceObservation.setPoint(location);

                // @todo 2 tb/** check for insitu - we may want to keep the *real* time delta tb 2014-02-12
                final Date time = new Date(samplingPoint.getTime());
                referenceObservation.setTime(time);
                referenceObservation.setTimeRadius(0.0);

                // @todo 1 tb/** we need to keep the fileId of insitu-file, orbit-file and eventually second orbit-file tb 2014-02-12
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
                // @todo 2 tb/** check pattern when using with insitu data - we may have to add a "| historyPattern" here   tb 2014-02-12
                matchup.setPattern(pattern);

                matchupList.add(matchup);

                final Observation observation = getObservation(samplingPoint.getReference());
                final Coincidence coincidence = new Coincidence();
                coincidence.setMatchup(matchup);
                coincidence.setObservation(observation);
                // @todo 2 tb/** check for insitu - we may want to keep the *real* time delta tb 2014-02-12
                coincidence.setTimeDifference(0.0);
                // TODO handle pattern
                coincidenceList.add(coincidence);

                if (samplingSensor2 != null) {
                    final Observation observation2 = getObservation(samplingPoint.getReference2());
                    final Coincidence coincidence2 = new Coincidence();
                    coincidence2.setMatchup(matchup);
                    coincidence2.setObservation(observation2);
                    coincidence2.setTimeDifference(
                            TimeUtil.timeDifferenceInSeconds(matchup, ((ReferenceObservation) observation2)));

                    coincidenceList.add(coincidence2);
                }
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

    public long getStartTime() {
        return startTime;
    }

    public long getStopTime() {
        return stopTime;
    }
}

