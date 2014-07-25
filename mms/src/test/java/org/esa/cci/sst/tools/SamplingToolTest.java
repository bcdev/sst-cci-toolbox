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

import org.esa.cci.sst.DatabaseTestRunner;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.samplepoint.DirtySubsceneRemover;
import org.esa.cci.sst.tools.samplepoint.ObservationFinder;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SamplingPointPlotter;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(DatabaseTestRunner.class)
public class SamplingToolTest {

    private SamplingTool tool;

    @Before
    public void setUp() throws Exception {
        tool = new SamplingTool();

        tool.setCommandLineArgs(new String[]{"-Dmms.sampling.count=10000"});
        tool.initialize();
    }

    @Test
    public void testRemoveOverlappingAreas() throws Exception {
        final long startTime = TimeUtil.parseCcsdsUtcFormat("2004-06-01T00:00:00Z").getTime();
        final long stopTime = TimeUtil.parseCcsdsUtcFormat("2004-06-04T00:00:00Z").getTime();
        final List<SamplingPoint> sampleList = SamplingTool.createSamples(10000, 0, startTime, stopTime);
        SamplingTool.removeLandSamples(sampleList);
        SamplingTool.reduceByClearSkyStatistic(sampleList);
        final PersistenceManager persistenceManager = tool.getPersistenceManager();

        final int quarterRepeatCycleInSeconds = 86400 * 175 / 20;
        final ObservationFinder observationFinder = new ObservationFinder(persistenceManager);
        final ObservationFinder.Parameter parameter = new ObservationFinder.Parameter();
        parameter.setSensorName(Constants.SENSOR_NAME_ORB_ATSR_3);
        parameter.setStartTime(tool.getStartTime());
        parameter.setStopTime(tool.getStopTime());
        parameter.setSearchTimePast(quarterRepeatCycleInSeconds);
        parameter.setSearchTimeFuture(quarterRepeatCycleInSeconds);
        observationFinder.findPrimarySensorObservations(sampleList, parameter);

        DirtySubsceneRemover.removeSamples(sampleList, true, 7, 7, tool.getConfig(),
                                           tool.getStorage(), tool.getLogger(),
                                           0.0);
        tool.removeOverlappingSamples(sampleList);

        assertEquals(77, sampleList.size());
    }

    public static void main(String[] args) throws IOException, ParseException {
        final String startTimeString = "2004-06-17T00:00:00Z";
        final String stopTimeString = "2004-06-18T00:00:00Z";
        final int sampleCount = 10000;

        final SamplingTool tool = new SamplingTool();
        tool.setCommandLineArgs(new String[]{
                "-Dmms.sampling.count=" + sampleCount,
                "-Dmms.sampling.skip=" + 0,
                "-Dmms.sampling.startTime=" + startTimeString,
                "-Dmms.sampling.stopTime=" + stopTimeString,
                "-Dmms.sampling.cleanup=" + true
        });
        tool.initialize();
        final Configuration config = tool.getConfig();
        if (config.getBooleanValue("mms.sampling.cleanup")) {
            tool.cleanup();
        } else if (config.getBooleanValue("mms.sampling.cleanupinterval")) {
            tool.cleanupInterval();
        }

        System.out.println("Creating samples...");
        final List<SamplingPoint> sampleList = SamplingTool.createSamples(10000, 0, 0, 1);
        System.out.println("Creating samples... " + sampleList.size());
        System.out.println("Removing land samples...");
        //tool.removeLandSamples(sampleList);
        System.out.println("Removing land samples..." + sampleList.size());
        System.out.println("Reducing clear samples...");
        //tool.reduceByClearSkyStatistic(sampleList);
        System.out.println("Reducing clear samples..." + sampleList.size());
        System.out.println("Finding reference observations...");
        //tool.findObservations2(sampleList, Constants.SENSOR_NAME_ORB_ATSR_3);
        System.out.println("Finding reference observations..." + sampleList.size());
        System.out.println("Finding satellite sub-scenes...");
        //tool.findSatelliteSubscenes(sampleList, Constants.SENSOR_NAME_ORB_ATSR_3);
        System.out.println("Finding satellite sub-scenes..." + sampleList.size());
        System.out.println("Removing overlapping areas...");
        //tool.removeOverlappingSamples(sampleList);
        System.out.println("Removing overlapping areas..." + sampleList.size());
        System.out.println("Creating matchups...");
        //tool.createMatchups(sampleList);
        System.out.println("Creating matchups..." + sampleList.size());

        new SamplingPointPlotter()
                .samples(sampleList)
                .windowTitle("n days with (nearly) global revisit of AATSR")
                .filePath("sampling.png")
                .plot();

        /*
        final Date startTime = TimeUtil.parseCcsdsUtcFormat(startTimeString);
        final Date stopTime = TimeUtil.parseCcsdsUtcFormat(stopTimeString);

        final List<ReferenceObservation> orbits = tool.findOrbits(startTimeString, stopTimeString);
        int noOrbitsToPlot = 14;
        for (ReferenceObservation orbit : orbits) {
            final int orbitId = orbit.getId();
            List<SamplingPoint> orbitSamples = filter(sampleList, new Predicate<SamplingPoint>() {
                @Override
                public boolean apply(SamplingPoint s) {
                    return s.getReference() == orbitId;
                }
            });
            showSamples(orbitSamples, "orbit " + orbit.getDatafile().getPath(), null);
            if (--noOrbitsToPlot <= 0) {
                break;
            }
        }
        */
    }

    public interface Predicate<T> {

        boolean apply(T type);
    }

    public static <T> List<T> filter(Collection<T> target, Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T element : target) {
            if (predicate.apply(element)) {
                result.add(element);
            }
        }
        return result;
    }

}
