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

import org.esa.cci.sst.tools.overlap.RegionOverlapFilter;
import org.esa.cci.sst.tools.samplepoint.ClearSkyPointRemover;
import org.esa.cci.sst.tools.samplepoint.CloudySubsceneRemover;
import org.esa.cci.sst.tools.samplepoint.LandPointRemover;
import org.esa.cci.sst.tools.samplepoint.ObservationFinder;
import org.esa.cci.sst.tools.samplepoint.SobolSamplePointGenerator;
import org.esa.cci.sst.util.SamplingPoint;

import javax.persistence.Query;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class SamplingTool extends BasicTool {

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
        if (config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_CLEANUP)) {
            cleanup();
        } else if (config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_CLEANUPINTERVAL)) {
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
        final ObservationFinder observationFinder = new ObservationFinder(getPersistenceManager());
        observationFinder.findPrimarySensorObservations(sampleList, samplingSensor,
                                                        startTime, stopTime, halfRepeatCycleInSeconds);
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
        CloudySubsceneRemover.removeSamples(sampleList, samplingSensor, true, subSceneWidth, subSceneHeight,
                                            getConfig(),
                                            getStorage(), getLogger(), "cloud_flags_nadir", 3, 0.0);
        getLogger().info("Finding satellite sub-scenes..." + sampleList.size());
        if (samplingSensor2 != null) {
            getLogger().info("Finding " + samplingSensor2 + " observations...");
            observationFinder.findSecondarySensorObservations(sampleList, samplingSensor2,
                                                              startTime, stopTime, matchupDistanceSeconds);
            getLogger().info("Finding " + samplingSensor2 + " observations..." + sampleList.size());

            getLogger().info("Finding " + samplingSensor2 + " sub-scenes...");
            CloudySubsceneRemover.removeSamples(sampleList, samplingSensor2, false, subSceneWidth, subSceneHeight,
                                                getConfig(),
                                                getStorage(), getLogger(), "cloud_flags_nadir", 3, 0.0);
            getLogger().info("Finding " + samplingSensor2 + " sub-scenes..." + sampleList.size());
        }

        getLogger().info("Removing overlapping areas...");
        removeOverlappingSamples(sampleList);
        getLogger().info("Removing overlapping areas..." + sampleList.size());

        getLogger().info("Creating matchups...");
        MatchupGenerator.createMatchups(sampleList, getStorage(), getPersistenceManager(), "sobol", samplingSensor, samplingSensor2,
                                        getConfig().getPattern("mms.pattern.sobol"));
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

    public void removeOverlappingSamples(List<SamplingPoint> sampleList) {
        final RegionOverlapFilter regionOverlapFilter = new RegionOverlapFilter(subSceneWidth, subSceneHeight);
        final List<SamplingPoint> filteredList = regionOverlapFilter.filterOverlaps(sampleList);
        sampleList.clear();
        sampleList.addAll(filteredList);
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

