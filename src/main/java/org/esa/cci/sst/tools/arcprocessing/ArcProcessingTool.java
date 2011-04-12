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

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.cci.sst.tools.MmsTool;
import org.esa.cci.sst.tools.ToolException;
import org.postgis.PGgeometry;
import org.postgis.Point;

import javax.persistence.Query;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Tool responsible for extracting subscenes using the ARC processors, and to re-ingest these subscenes into the MMDB.
 * Its basic steps are
 * <pre>
 * 1) get coordinates c for each matchup from the database
 * 2) process all AVHRR-GAC files using ARC1; result: corresponding files gi with geo information.
 * 3) for each coordinate of c: get pixel position for each file of gi for that information; result: list of pixel
 *    positions pp
 * 4) pp serves as input for ARC2, together with each AVHRR-GAC file; result: AVHRR subscenes s.
 * 5) ingest s into the MMDB.
 * </pre>
 *
 * @author Thomas Storm
 */
public class ArcProcessingTool extends MmsTool {

    private static final String ALL_POINTS_QUERY = "SELECT o.point " +
                                                   "FROM ReferenceObservation o, Matchup m " +
                                                   "WHERE m.refObs.id = o.id " +
                                                   "ORDER BY m.id";

    public ArcProcessingTool() {
        super("mmssubscenes.sh", "0.1");
    }

    public static void main(String[] args) {
//        ArcProcessingTool tool = createArcProcessingTool(args);
//        tool.performArcChain();
        testSimpleExecutable();
    }

    public static void testSimpleExecutable() {
        try {
            Runtime runTime = Runtime.getRuntime();
            Process child = runTime.exec("halloWorld.sh");
            BufferedWriter outCommand = new BufferedWriter(new OutputStreamWriter(child.getOutputStream()));
            outCommand.write("MyShellScript");
            outCommand.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ArcProcessingTool createArcProcessingTool(final String[] args) {
        final ArcProcessingTool tool = new ArcProcessingTool();
        try {
            tool.setCommandLineArgs(args);
            tool.initialize();
        } catch (ToolException e) {
            tool.getErrorHandler().handleError(e, MessageFormat.format("Unable to initialise ''{0}''.", tool.getName()),
                                               ToolException.TOOL_ERROR);
        }
        return tool;
    }

    private void performArcChain() {
        List<GeoPos> coordinates = getCoordinates();
        List<File> geoInformationFiles = processAllAvhrrFilesWithArc1();
//        List<PixelPos> pixelPositions = getPixelPositions(coordinates, geoInformationFiles);
//        List<File> avhrrSubscenes = processArc2(pixelPositions);
//        ingestAvhrrSubscenes(avhrrSubscenes);
    }

    List<GeoPos> getCoordinates() {
        getPersistenceManager().transaction();
        final List<GeoPos> result;
        try {
            result = getCoordinatesFromOpenDatabase();
        } finally {
            getPersistenceManager().commit();
        }
        return result;
    }

    @SuppressWarnings({"unchecked"})
    private List<GeoPos> getCoordinatesFromOpenDatabase() {
        final Query allPointsQuery = getPersistenceManager().createQuery(ALL_POINTS_QUERY);
        final List<PGgeometry> results = allPointsQuery.getResultList();
        final List<GeoPos> result = new ArrayList<GeoPos>(results.size());
        for (PGgeometry geometry : results) {
            final Point point = geometry.getGeometry().getFirstPoint();
            final GeoPos geoPos = new GeoPos((float) point.getY(), (float) point.getX());
            result.add(geoPos);
        }
        return result;
    }

    private List<File> processAllAvhrrFilesWithArc1() {
        final Properties configuration = getConfiguration();
        final Arc1Caller arc1Caller = new Arc1Caller(this);
        return arc1Caller.processAllAvhrrFiles();
    }

}
