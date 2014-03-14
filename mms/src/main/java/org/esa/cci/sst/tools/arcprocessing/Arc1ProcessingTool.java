/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools.arcprocessing;

import com.bc.ceres.core.Assert;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool responsible for extracting subscenes using the ARC1 and ARC2 processor.
 * Preconditions:
 * <ul>
 *     <li>thetis:mms/sst-cci-toolbox-0.2-SNAPSHOT/ installed</li>
 *     <li>configured tmp dir and output dir on thetis exist</li>
 *     <li>eddie:mms linked to /exports/work/geos_gc_sst_cci/mms/ visible from eddie nodes</li>
 *     <li>eddie:mms/sst-cci-toolbox-0.2-SNAPSHOT/ installed</li>
 *     <li>eddie:mms/ dir writable for temporary subdirectories</li>
 *     <li>eddie:tmp/ exists</li>
 *     <li>ssh key for login from thetis to eddie installed
 * </ul>
 *
 * @author Thomas Storm
 * @author Martin Boettcher
 */
public class Arc1ProcessingTool extends BasicTool {

//    private static final String AVHRR_MATCHING_OBSERVATIONS_QUERY =
//            "SELECT m.id, ST_astext(ref.point), f.path, s.name, s.pattern " +
//                    "FROM mm_observation o, mm_coincidence c, mm_matchup m, " +
//                    "     mm_observation ref, mm_datafile f, mm_sensor s " +
//                    "WHERE o.sensor LIKE 'orb_avhrr.%' " +
//                    "AND c.observation_id = o.id " +
//                    "AND m.id = c.matchup_id " +
//                    "AND ref.id = m.refobs_id " +
//                    "AND ref.time >= ? " +
//                    "AND ref.time < ? " +
//                    "AND f.id = o.datafile_id " +
//                    "AND s.id = f.sensor_id " +
//                    "ORDER BY f.path";
    private static final String AVHRR_MATCHING_OBSERVATIONS_QUERY =
            "SELECT r.id, ST_astext(r.point), f.path, s.name, s.pattern " +
                    "FROM mm_observation o, mm_coincidence c, " +
                    "     mm_observation r, mm_datafile f, mm_sensor s " +
                    "WHERE o.sensor LIKE 'orb_avhrr.%' " +
                    "AND c.observation_id = o.id " +
                    "AND r.id = c.matchup_id " +
                    "AND r.time >= ? " +
                    "AND r.time < ? " +
                    "AND f.id = o.datafile_id " +
                    "AND s.id = f.sensor_id " +
                    "ORDER BY f.path, r.time, r.id";
    public static final String LATLON_FILE_EXTENSION = ".latlon.txt";

    private static class MatchingObservationInfo {
        String matchupId;
        String filename;
        String point;
        String sensor;
        long pattern;
    }

    private String submitCallFilename = null;
    private String collectCallFilename = null;
    private String cleanupCallFilename = null;
    private PrintWriter submitCallsWriter = null;
    private PrintWriter collectCallsWriter = null;
    private PrintWriter cleanupCallsWriter = null;

    public Arc1ProcessingTool() {
        super("arc12-tool.sh", "0.1");
    }

    public static void main(String[] args) {
        final Arc1ProcessingTool tool = new Arc1ProcessingTool();
        try {
            tool.setCommandLineArgs(args);
            tool.initialize();
            tool.run();
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Exception e) {
            tool.getErrorHandler().terminate(new ToolException("Tool failed.", e, ToolException.TOOL_ERROR));
        } finally {
            tool.getPersistenceManager().close();
        }
    }

    private void run() throws IOException {
        final Configuration config = getConfig();
        final String destPath = config.getStringValue(Constants.PROPERTY_OUTPUT_DESTDIR, ".");
        final String archiveRootPath = config.getStringValue(Configuration.KEY_ARCHIVE_ROOTDIR, ".");
        final File archiveRoot = new File(archiveRootPath);
        final String tmpPath = config.getStringValue(Constants.PROPERTY_OUTPUT_TMPDIR, ".");

        final String startTimeProperty = config.getStringValue(Constants.PROPERTY_OUTPUT_START_TIME);
        final String endTimeProperty = config.getStringValue(Constants.PROPERTY_OUTPUT_STOP_TIME);
        final String sensorName = config.getStringValue(Constants.PROPERTY_OUTPUT_SENSOR, "%");  // e.g. "orb_avhrr.n18"
        final String configurationPath = config.getStringValue(Configuration.KEY_MMS_CONFIGURATION);
        final String condition = config.getStringValue(Constants.PROPERTY_ARC1x2_CONDITION);

        final Date startTime = TimeUtil.getConfiguredTimeOf(startTimeProperty);
        final Date endTime = TimeUtil.getConfiguredTimeOf(endTimeProperty);

        prepareCommandFiles(tmpPath, archiveRoot, destPath, TimeUtil.formatCompactUtcFormat(startTime));

        final List<MatchingObservationInfo> avhrrObservationInfos = inquireMatchingAvhrrObservations(startTime, endTime, sensorName, condition);
        generateCallsAndLatlonFiles(avhrrObservationInfos, archiveRoot, destPath, tmpPath, configurationPath);

        closeCommandFiles(tmpPath);
    }

    private void prepareCommandFiles(String tmpPath, File archiveRoot, String destPath, String outputStartTime) throws IOException {
        getLogger().info(String.format("generating scripts in tmp dir '%s'", tmpPath));
        final File tmpDir = new File(tmpPath);
        tmpDir.mkdirs();
        final File destDir;
        if (destPath.startsWith(File.separator)) {
            destDir = new File(destPath);
        } else {
            destDir = new File(archiveRoot, destPath);
        }
        destDir.mkdirs();

        submitCallFilename = String.format("mms-arc1x2-%s-submit.sh", outputStartTime);
        final File submitCallFile = new File(tmpDir, submitCallFilename);
        submitCallFile.setExecutable(true);
        submitCallsWriter = new PrintWriter(new BufferedWriter(new FileWriter(submitCallFile)));
        submitCallsWriter.format("#!/bin/bash\n\n");

        collectCallFilename = String.format("mms-arc1x2-%s-collect.sh", outputStartTime);
        final File collectCallFile = new File(tmpDir, collectCallFilename);
        collectCallFile.setExecutable(true);
        collectCallsWriter = new PrintWriter(new BufferedWriter(new FileWriter(collectCallFile)));
        collectCallsWriter.format("#!/bin/bash\n\n");
        collectCallsWriter.format("if [ ! -d \"$MMS_HOME\" ]\n" +
                                    "then\n" +
                                    "    PRGDIR=`dirname $0`\n" +
                                    "    export MMS_HOME=`cd \"$PRGDIR/..\" ; pwd`\n" +
                                    "fi\n\n");

        cleanupCallFilename = String.format("mms-arc1x2-%s-cleanup.sh", outputStartTime);
        final File cleanupCallFile = new File(tmpDir, cleanupCallFilename);
        cleanupCallFile.setExecutable(true);
        cleanupCallsWriter = new PrintWriter(new BufferedWriter(new FileWriter(cleanupCallFile)));
        cleanupCallsWriter.format("#!/bin/bash\n\n");
    }

    private void closeCommandFiles(String tmpPath) throws IOException {
        cleanupCallsWriter.format("rm %s/%s\n", tmpPath, submitCallFilename);
        cleanupCallsWriter.format("rm %s/%s\n", tmpPath, collectCallFilename);
        cleanupCallsWriter.format("rm %s/%s\n", tmpPath, cleanupCallFilename);
        if (submitCallsWriter != null) {
            submitCallsWriter.close();
        }
        if (collectCallsWriter != null) {
            collectCallsWriter.close();
        }
        if (cleanupCallsWriter != null) {
            cleanupCallsWriter.close();
        }
    }

    @SuppressWarnings({"unchecked"})
    private List<MatchingObservationInfo> inquireMatchingAvhrrObservations(Date startTime, Date endTime, String sensorName, String condition) {
        final Query allPointsQuery;
        String queryString;
        if (condition != null) {
            queryString = AVHRR_MATCHING_OBSERVATIONS_QUERY.replace("WHERE", "WHERE " + condition + " AND");
        } else {
            queryString = AVHRR_MATCHING_OBSERVATIONS_QUERY;
        }
        if ("%".equals(sensorName)) {
            allPointsQuery = getPersistenceManager().createNativeQuery(queryString, Object[].class);
        } else {
            String sensorSpecificQueryString = queryString.replace("LIKE 'orb_avhrr.%'", String.format("= '%s'", sensorName));
            allPointsQuery = getPersistenceManager().createNativeQuery(sensorSpecificQueryString, Object[].class);
            getLogger().fine(String.format("sensor specific query %s", sensorSpecificQueryString));
        }
        allPointsQuery.setParameter(1, startTime);
        allPointsQuery.setParameter(2, endTime);
        final List<Object[]> queryResults = allPointsQuery.getResultList();
        final List<MatchingObservationInfo> matchingObservationInfos =
                new ArrayList<MatchingObservationInfo>(queryResults.size());
        for (Object[] info : queryResults) {
            final MatchingObservationInfo matchingObservationInfo = new MatchingObservationInfo();
            matchingObservationInfo.matchupId = info[0].toString();
            matchingObservationInfo.point = info[1].toString();
            matchingObservationInfo.filename = info[2].toString();
            matchingObservationInfo.sensor = info[3].toString();
            matchingObservationInfo.pattern = (Long) info[4];
            matchingObservationInfos.add(matchingObservationInfo);
        }
        getLogger().info(String.format("%d matching observations found in time interval", matchingObservationInfos.size()));
        return matchingObservationInfos;
    }

    private void generateCallsAndLatlonFiles(List<MatchingObservationInfo> observationInfos,
                                             File archiveRoot, String destPath, String tmpPath, String configurationPath) throws IOException {
        final List<String> geoPositions = new ArrayList<String>();
        final List<String> matchupIds = new ArrayList<String>();
        String currentFilename = null;
        String currentSensor = null;
        long currentPattern = 0;
        // loop over matching observations in sets of orbit file names (uses ordering of observationInfos)
        for (MatchingObservationInfo info : observationInfos) {
            // write previous latlon file and script entry on change of orbit file name
            if (!info.filename.equals(currentFilename)) {
                if (currentFilename != null) {
                    final File latLonFile = latLonFileOf(currentFilename, tmpPath);
                    writeLatLonFile(matchupIds, geoPositions, latLonFile);
                    generateCalls(currentFilename, latLonFile, archiveRoot, destPath,
                                  targetSensorNameOf(currentSensor), nonOrbPatternOf(currentPattern), configurationPath);
                    // clear for next set of matching observations
                    geoPositions.clear();
                    matchupIds.clear();
                }
                // memorise next orbit file name, sensor and pattern
                currentFilename = info.filename;
                currentSensor = info.sensor;
                currentPattern = info.pattern;
            }
            geoPositions.add(info.point);
            matchupIds.add(info.matchupId);
        }
        // write latlon file and script entry for last orbit file name encountered
        if (!geoPositions.isEmpty()) {
            final File latLonFile = latLonFileOf(currentFilename, tmpPath);
            writeLatLonFile(matchupIds, geoPositions, latLonFile);
            generateCalls(currentFilename, latLonFile, archiveRoot, destPath,
                          targetSensorNameOf(currentSensor), nonOrbPatternOf(currentPattern), configurationPath);
        }
    }

    private void writeLatLonFile(final List<String> matchupIds, final List<String> geoPositions,
                                 final File latLonFile) throws IOException {
        Assert.argument(matchupIds.size() == geoPositions.size(), "Same number of matchups and points expected.");
        BufferedWriter writer = new BufferedWriter(new FileWriter(latLonFile));
        writer.write(String.format("%d\n", geoPositions.size()));
        for (int i = 0; i < geoPositions.size(); i++) {
            final String geoPosition = geoPositions.get(i);
            final String matchupId = matchupIds.get(i);
            final Pattern pattern = Pattern.compile("POINT\\(([0-9.e-]*) ([0-9.e-]*)\\)");
            final Matcher matcher = pattern.matcher(geoPosition);
            matcher.matches();
            final String lon = matcher.group(1);
            final String lat = matcher.group(2);
            writer.write(String.format("\t%s\t%s\t%s\n", matchupId, lon, lat));
        }
        writer.close();
    }

    private void generateCalls(String l1bPath, final File latlonFile, File archiveRoot, String destPath,
                               String sensor, long pattern, String configurationPath) {
        final String basename = basenameOf(l1bPath);
        final String latLonFilePath = latlonFile.getPath();
        final String latLonFileName = latlonFile.getName();

        if (!destPath.startsWith(File.separator)) {
            destPath = archiveRoot.getPath() + File.separator + destPath;
        }
        if(!l1bPath.startsWith(File.separator)) {
            l1bPath = archiveRoot.getPath() + File.separator + l1bPath;
        }
        submitCallsWriter.format("scp %s eddie.ecdf.ed.ac.uk:tmp/\n", latLonFilePath);
        submitCallsWriter.format("ssh eddie.ecdf.ed.ac.uk sst-cci-toolbox-0.2-SNAPSHOT/bin/start_arc1x2.sh %s tmp/%s\n",
                l1bPath, latLonFileName);
        collectCallsWriter.format("scp eddie.ecdf.ed.ac.uk:mms/task-%s/%s.MMM.nc %s\n", basename, basename, destPath);
        collectCallsWriter.format("chmod 775 %s/%s.MMM.nc\n", destPath, basename);
        collectCallsWriter.format(
                "$MMS_HOME/bin/mmsreingestmmd.sh -Dmms.reingestion.filename=%s/%s.MMM.nc \\\n"
                        + "  -Dmms.reingestion.located=no \\\n"
                        + "  -Dmms.reingestion.sensor=%s \\\n"
                        + "  -Dmms.reingestion.pattern=%s \\\n"
                        + "  -c %s\n",
                destPath, basename, sensor, Long.toString(pattern, 16), configurationPath);
        cleanupCallsWriter.format("ssh eddie.ecdf.ed.ac.uk rm -r mms/task-%s tmp/%s.latlon.txt\n", basename, basename);
        cleanupCallsWriter.format("rm %s\n", latLonFilePath);
        getLogger().info(String.format("call for avhrr orbit %s added", basename));
    }


    private static File latLonFileOf(final String currentFilename, final String tmpPath) {
        final int slashIndex = currentFilename.lastIndexOf('/');
        final String baseFilename;
        if (currentFilename.endsWith(".gz")) {
            baseFilename = currentFilename.substring(slashIndex + 1, currentFilename.length() - ".gz".length());
        } else {
            baseFilename = currentFilename.substring(slashIndex + 1);
        }
        return new File(tmpPath + File.separator + baseFilename + LATLON_FILE_EXTENSION);
    }

    private static String basenameOf(final String currentFilename) {
        final int slashIndex = currentFilename.lastIndexOf('/');
        if (currentFilename.endsWith(".gz")) {
            return currentFilename.substring(slashIndex + 1, currentFilename.length() - ".gz".length());
        } else {
            return currentFilename.substring(slashIndex + 1);
        }
    }

    private static String targetSensorNameOf(String sensor) {
        return sensor.replaceAll("orb_", "");
    }

    private static long nonOrbPatternOf(long pattern) {
        return pattern >> 32;
    }
}
