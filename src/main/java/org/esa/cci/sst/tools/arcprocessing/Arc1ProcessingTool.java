/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool responsible for extracting subscenes using the ARC1 processor.
 *
 * @author Thomas Storm
 * @author Martin Boettcher
 */
public class Arc1ProcessingTool extends BasicTool {

    private static final String AVHRR_MATCHUPIDS_FILES_AND_POINTS_QUERY = "SELECT m.id, ST_astext(ref.point), df.path " +
                                                                          "FROM mm_datafile df, mm_observation o, mm_matchup m, " +
                                                                          "     mm_coincidence c, mm_observation ref " +
                                                                          "WHERE c.matchup_id = m.id " +
                                                                          "AND o.id = c.observation_id " +
                                                                          "AND df.id = o.datafile_id " +
                                                                          "AND o.sensor LIKE 'avhrr%' " +
                                                                          "AND ref.id = m.refobs_id " +
                                                                          "AND ref.time >= ? " +
                                                                          "AND ref.time < ? " +
                                                                          "ORDER BY df.path";
    public static final String LATLON_FILE_EXTENSION = ".latlon.txt";

    private PrintWriter submitCallsWriter = null;
    private PrintWriter collectCallsWriter = null;
    private PrintWriter cleanupCallsWriter = null;
    private String submitCallFilename = null;
    private String collectCallFilename = null;
    private String cleanupCallFilename = null;

    public Arc1ProcessingTool() {
        super("mmssubscenes.sh", "0.1");
    }

    public static void main(String[] args) {
        final Arc1ProcessingTool tool = new Arc1ProcessingTool();
        tool.setCommandLineArgs(args);
        tool.initialize();
        try {
            tool.prepareCommandFiles();
            final List<AvhrrInfo> avhrrFilesAndPoints = tool.inquireAvhrrInfos();
            tool.prepareAndPerformArcCall(avhrrFilesAndPoints);
            tool.closeCommandFiles();
        } catch (IOException e) {
            tool.getErrorHandler().terminate(new ToolException("Tool failed.", e, ToolException.TOOL_ERROR));
        }
    }

    private void prepareCommandFiles() throws IOException {
        final String destPath = getConfiguration().getProperty(Constants.PROPERTY_OUTPUT_DESTDIR, ".");
        final File destDir = new File(destPath);
        destDir.mkdirs();
        final String tmpPath = getConfiguration().getProperty(Constants.PROPERTY_OUTPUT_TMPDIR, ".");
        final File tmpDir = new File(tmpPath);
        tmpDir.mkdirs();
        final String time = getConfiguration().getProperty(Constants.PROPERTY_OUTPUT_START_TIME);

        submitCallFilename = String.format("mms-arc1x2-%s-submit.sh", time);
        submitCallsWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(tmpDir, submitCallFilename))));
        submitCallsWriter.format("#!/bin/bash\n\n");

        collectCallFilename = String.format("mms-arc1x2-%s-collect.sh", time);
        collectCallsWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(tmpDir, collectCallFilename))));
        collectCallsWriter.format("#!/bin/bash\n\n");

        cleanupCallFilename = String.format("mms-arc1x2-%s-cleanup.sh", time);
        cleanupCallsWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(tmpDir, cleanupCallFilename))));
        cleanupCallsWriter.format("#!/bin/bash\n\n");
    }

    private void closeCommandFiles() throws IOException {
        final String tmpPath = getConfiguration().getProperty(Constants.PROPERTY_OUTPUT_TMPDIR);
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

    void prepareAndPerformArcCall(List<AvhrrInfo> avhrrFilesAndPoints) throws IOException {
        final List<String> geoPositions = new ArrayList<String>();
        final List<String> matchupIds = new ArrayList<String>();
        String currentFilename = null;
        for (AvhrrInfo info : avhrrFilesAndPoints) {
            final String matchupId = info.matchupId;
            final String point = info.point;
            final String filename = info.filename;
            if (!filename.equals(currentFilename)) {
                if (currentFilename != null) {
                    writeLatLonFile(matchupIds, geoPositions, currentFilename);
                    callShellScript(currentFilename, getLatLonFile(currentFilename));
                    geoPositions.clear();
                    matchupIds.clear();
                }
                currentFilename = filename;
            }
            geoPositions.add(point);
            matchupIds.add(matchupId);
        }
        if (!geoPositions.isEmpty()) {
            writeLatLonFile(matchupIds, geoPositions, currentFilename);
            callShellScript(currentFilename, getLatLonFile(currentFilename));
        }
    }

    @SuppressWarnings({"unchecked"})
    List<AvhrrInfo> inquireAvhrrInfos() {
        final Query allPointsQuery = getPersistenceManager().createNativeQuery(AVHRR_MATCHUPIDS_FILES_AND_POINTS_QUERY,
                                                                               Object[].class);
        allPointsQuery.setParameter(1, getTimeProperty(Constants.PROPERTY_OUTPUT_START_TIME));
        allPointsQuery.setParameter(2, getTimeProperty(Constants.PROPERTY_OUTPUT_END_TIME));
        final List<Object[]> queryResultList = allPointsQuery.getResultList();
        final List<AvhrrInfo> avhrrInfos = new ArrayList<AvhrrInfo>(queryResultList.size());
        for (Object[] info : queryResultList) {
            final AvhrrInfo avhrrInfo = new AvhrrInfo();
            avhrrInfo.matchupId = info[0].toString();
            avhrrInfo.point = info[1].toString();
            avhrrInfo.filename = info[2].toString();
            avhrrInfos.add(avhrrInfo);
        }
        return avhrrInfos;
    }

    private void writeLatLonFile(final List<String> matchupIds, final List<String> geoPositions,
                                 final String currentFilename) throws IOException {
        Assert.argument(matchupIds.size() == geoPositions.size(), "Same number of matchups and points expected.");
        final File latLonFile = getLatLonFile(currentFilename);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(latLonFile));
            writer.write(String.format("%d\n", geoPositions.size()));
            for (int i = 0; i < geoPositions.size(); i++) {
                final String geoPosition = geoPositions.get(i);
                final String matchupId = matchupIds.get(i);
                final Pattern pattern = Pattern.compile("POINT\\(([0-9.-]*) ([0-9.-]*)\\)");
                final Matcher matcher = pattern.matcher(geoPosition);
                matcher.matches();
                final String lon = matcher.group(1);
                final String lat = matcher.group(2);
                writer.write(String.format("\t%s\t%s\t%s\n", matchupId, lon, lat));
            }
        } finally {
            close(writer);
        }
    }

    // todo determine sensor from input, use it for the output
    private void callShellScript(final String currentFilename, final File latLonFile) {
        final String destPath = getConfiguration().getProperty(Constants.PROPERTY_OUTPUT_DESTDIR);
        final String basename = getBasename(currentFilename);
        final String latLonFilePath = latLonFile.getPath();
        final String latLonFileName = latLonFile.getName();
        submitCallsWriter.format("scp %s eddie.ecdf.ed.ac.uk:tmp/\n", latLonFilePath);
        submitCallsWriter.format(
                "ssh eddie.ecdf.ed.ac.uk mms/sst-cci-toolbox-0.1-SNAPSHOT/bin/start_arc1x2.sh /exports%s tmp/%s\n",
                currentFilename, latLonFileName);
        collectCallsWriter.format("scp eddie.ecdf.ed.ak.uk:mms/task-%s/%s.MMM.nc %s\n", basename, basename, destPath);
        collectCallsWriter.format(
                "bin/mmsreingest.sh -Dmms.reingestion.filename=%s/%s.MMM.nc \\\n  -Dmms.reingestion.type=arc3 \\\n  -Dmms.reingestion.schema=avhrr_sub \\\n  -Dmms.reingestion.sensor=avhrr_nxx_sub \\\n  -Dmms.reingestion.sensorType=avhrr_sub \\\n  -c config/mms-config-eddie1.properties",
                destPath, basename);
        cleanupCallsWriter.format("ssh eddie.ecdf.ed.ak.uk rm -r mms/task-%s\n", basename);
        cleanupCallsWriter.format("rm %s", latLonFilePath);
    }

    private void close(final BufferedWriter writer) {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException ignore) {
            // ok
        }
    }

    private File getLatLonFile(final String currentFilename) {
        final String tmpPath = getConfiguration().getProperty(Constants.PROPERTY_OUTPUT_TMPDIR);
        final int slashIndex = currentFilename.lastIndexOf('/');
        final String baseFilename;
        if (currentFilename.endsWith(".gz")) {
            baseFilename = currentFilename.substring(slashIndex + 1, currentFilename.length() - ".gz".length());
        } else {
            baseFilename = currentFilename.substring(slashIndex + 1);
        }
        return new File(tmpPath + File.separator + baseFilename + LATLON_FILE_EXTENSION);
    }

    private String getBasename(final String currentFilename) {
        final int slashIndex = currentFilename.lastIndexOf('/');
        if (currentFilename.endsWith(".gz")) {
            return currentFilename.substring(slashIndex + 1, currentFilename.length() - ".gz".length());
        } else {
            return currentFilename.substring(slashIndex + 1);
        }
    }

    private Date getTimeProperty(String key) {
        final String time = getConfiguration().getProperty(key);
        final Date date;
        try {
            date = TimeUtil.parseCcsdsUtcFormat(time);
        } catch (ParseException e) {
            final String message = MessageFormat.format("Unable to parse time parameter ''{0}''.", key);
            throw new ToolException(message, e, ToolException.CONFIGURATION_FILE_IO_ERROR);
        }
        return date;
    }

    private static class AvhrrInfo {

        String matchupId;
        String filename;
        String point;
    }
}
