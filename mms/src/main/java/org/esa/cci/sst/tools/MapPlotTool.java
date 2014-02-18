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

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SamplingPointPlotter;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.Point;

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class MapPlotTool extends BasicTool {

    private static final String REFOBS_QUERY =
            "select o"
            + " from ReferenceObservation o, Matchup m, Observation o2, Coincidence c2"
            + " where m.refObs = o and o.sensor = 'sobol' and o.time >= ?2 and o.time < ?3 and c2.matchup = m and c2.observation = o2 and o2.sensor = ?1"
            + " order by o.time, m.id";

    private String samplingSensor;
    private Date startTime;
    private Date stopTime;
    private boolean showMaps;

    MapPlotTool() {
        super("mapplot-tool", "1.0");
    }

    public static void main(String[] args) {
        final MapPlotTool tool = new MapPlotTool();
        try {
            boolean ok = tool.setCommandLineArgs(args);
            if (!ok) {
                tool.printHelp();
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
        samplingSensor = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR_1);
        startTime = config.getDateValue(Configuration.KEY_MMS_SAMPLING_START_TIME);
        stopTime = config.getDateValue(Configuration.KEY_MMS_SAMPLING_STOP_TIME);
        showMaps = config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_SHOW_MAPS);
    }

    private void run() throws IOException, ParseException {
        final PersistenceManager persistenceManager = getPersistenceManager();
        try {
            persistenceManager.transaction();
            final Query query = getPersistenceManager().createQuery(REFOBS_QUERY);
            query.setParameter(1, samplingSensor);
            query.setParameter(2, startTime);
            query.setParameter(3, stopTime);
            final List<ReferenceObservation> refobsList = query.getResultList();
            final String imageName = "sobol-" + samplingSensor + "-" + TimeUtil.formatCompactUtcFormat(
                    startTime) + ".png";
            plotSamples(refobsList, imageName, imageName, showMaps);

            final List<ReferenceObservation> orbits = findOrbits(TimeUtil.formatCcsdsUtcFormat(startTime),
                                                                 TimeUtil.formatCcsdsUtcFormat(stopTime));
            int noOrbitsToPlot = 14;
            for (ReferenceObservation orbit : orbits) {
                final DataFile orbitDataFile = orbit.getDatafile();
                List<ReferenceObservation> orbitSamples = filter(refobsList, new Predicate<ReferenceObservation>() {
                    @Override
                    public boolean apply(ReferenceObservation s) {
                        return s.getDatafile() == orbitDataFile;
                    }
                });
                final String orbitImageName = "sobol-" + orbitDataFile.getPath().substring(
                        orbitDataFile.getPath().lastIndexOf(
                                File.separator) + 1) + "-" + TimeUtil.formatCompactUtcFormat(startTime) + ".png";
                plotSamples(orbitSamples, orbitImageName, orbitImageName, showMaps);
                if (--noOrbitsToPlot <= 0) {
                    break;
                }
            }
        } finally {
            persistenceManager.commit();
        }

    }

    private static void plotSamples(List<ReferenceObservation> samples, String title, String path, boolean showPlot)
            throws IOException {
        final List<SamplingPoint> samplingPoints = new ArrayList<>(samples.size());
        for (final ReferenceObservation s : samples) {
            final Point p = s.getPoint().getGeometry().getPoint(0);
            samplingPoints.add(new SamplingPoint(p.getX(), p.getY(), 0, 0.0));
        }
        new SamplingPointPlotter()
                .samples(samplingPoints)
                .show(showPlot)
                .live(showPlot)
                .windowTitle(title)
                .filePath(path)
                .plot();
    }

    private static final String SENSOR_OBSERVATION_QUERY =
            "select o.id"
            + " from mm_observation o"
            + " where o.sensor = ?1"
            + " and o.time >= timestamp ?2 and o.time < timestamp ?3"
            + " order by o.time, o.id";

    List<ReferenceObservation> findOrbits(String startTimeString, String stopTimeString) throws ParseException {
        //Date startTime = new Date(TimeUtil.parseCcsdsUtcFormat(startTimeString).getTime());
        //Date stopTime = new Date(TimeUtil.parseCcsdsUtcFormat(stopTimeString).getTime());
        final String queryString2 = SENSOR_OBSERVATION_QUERY.replaceAll("\\?2", "'" + startTimeString + "'").replaceAll(
                "\\?3", "'" + stopTimeString + "'");
        final Query query = getPersistenceManager().createNativeQuery(queryString2, ReferenceObservation.class);
        query.setParameter(1, "atsr_orb.3");
        //query.setParameter(2, startTime);
        //query.setParameter(3, stopTime);
        return query.getResultList();
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
