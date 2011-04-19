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
import org.esa.cci.sst.tools.MmsTool;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
 */
public class Arc1ProcessingTool extends MmsTool {

    private static final String AVHRR_FILES_AND_POINTS_QUERY = "SELECT m.id, ST_astext(ref.point), df.path " +
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

    public Arc1ProcessingTool() {
        super("mmssubscenes.sh", "0.1");
    }

    public static void main(String[] args) throws ToolException {
        final Arc1ProcessingTool tool = new Arc1ProcessingTool();
        tool.setCommandLineArgs(args);
        tool.initialize();
        final List<Object[]> avhrrFilesAndPoints = tool.inquireAvhrrFilesAndPoints();
        try {
            tool.prepareAndPerformArcCall(avhrrFilesAndPoints);
        } catch (IOException e) {
            tool.getErrorHandler().handleError(e, "Tool failed.", ToolException.TOOL_ERROR);
        }
    }

    void prepareAndPerformArcCall(List<Object[]> avhrrFilesAndPoints) throws IOException {
        final List<String> geoPositions = new ArrayList<String>();
        final List<String> matchupIds = new ArrayList<String>();
        String currentFilename = null;
        for (Object[] result : avhrrFilesAndPoints) {
            final String matchupId = result[0].toString();
            final String point = result[1].toString();
            final String filename = result[2].toString();
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
    List<Object[]> inquireAvhrrFilesAndPoints() {
        final Query allPointsQuery = getPersistenceManager().createNativeQuery(AVHRR_FILES_AND_POINTS_QUERY,
                                                                               Object[].class);
        allPointsQuery.setParameter(1, getTimeProperty("mms.arcprocessing.starttime"));
        allPointsQuery.setParameter(2, getTimeProperty("mms.arcprocessing.endtime"));
        return allPointsQuery.getResultList();
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

    private void callShellScript(final String currentFilename, final File latLonFile) {
        final String latLonFileName = latLonFile.getName();
        // todo - ts 19Apr2011 - replace by concrete call
        System.out.format("scp %s tstorm@eddie.ecdf.ed.ac.uk:tmp/\n", latLonFileName);
        System.out.format("ssh tstorm@eddie.ecdf.ed.ac.uk arc1arc2.sh %s tmp/%s\n", currentFilename, latLonFileName);
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
        final int slashIndex = currentFilename.lastIndexOf('/');
        final String baseFilename = currentFilename.substring(slashIndex + 1);
        return new File(baseFilename + ".latlon");
    }

    private Date getTimeProperty(String key) {
        final String time = getConfiguration().getProperty(key);
        Date date = null;
        try {
            date = new Date(TimeUtil.parseCcsdsUtcFormat(time));
        } catch (ParseException e) {
            getErrorHandler().handleError(e, "Unable to parse time parameter '" + key + "'.",
                                          ToolException.CONFIGURATION_FILE_IO_ERROR);
        }
        return date;
    }
}
