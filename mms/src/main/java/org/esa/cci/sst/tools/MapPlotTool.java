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

import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SamplingPointPlotter;
import org.postgis.Point;

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapPlotTool extends BasicTool {

    private static final String SQL_GET_REFERENCE_OBSERVATIONS =
            "select o"
            + " from ReferenceObservation o"
            + " where o.sensor = ?1 and o.time >= ?2 and o.time < ?3"
            + " order by o.time";

    private String sensor;
    private Date startTime;
    private Date stopTime;
    private boolean show;
    private String mapStrategyName;
    private String targetFilename;
    private String title;
    private String targetDir;

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
        } finally {
            tool.getPersistenceManager().close();
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        final Configuration config = getConfig();
        sensor = config.getStringValue(Configuration.KEY_MMS_MAPPLOT_SENSOR);
        startTime = config.getDateValue(Configuration.KEY_MMS_MAPPLOT_START_TIME);
        stopTime = config.getDateValue(Configuration.KEY_MMS_MAPPLOT_STOP_TIME);
        show = config.getBooleanValue(Configuration.KEY_MMS_MAPPLOT_SHOW, false);
        mapStrategyName = config.getStringValue(Configuration.KEY_MMS_MAPPLOT_STATEGY, "latlon");
        targetDir = config.getStringValue(Configuration.KEY_MMS_MAPPLOT_TARGET_DIR);
        targetFilename = config.getStringValue(Configuration.KEY_MMS_MAPPLOT_TARGET_FILENAME);
        title = config.getStringValue(Configuration.KEY_MMS_MAPPLOT_TITLE, null);
    }

    private void run() throws IOException, ParseException {
        final PersistenceManager persistenceManager = getPersistenceManager();
        try {
            persistenceManager.transaction();
            final Query query = getPersistenceManager().createQuery(SQL_GET_REFERENCE_OBSERVATIONS);
            query.setParameter(1, sensor);
            query.setParameter(2, startTime);
            query.setParameter(3, stopTime);

            @SuppressWarnings("unchecked")
            final List<ReferenceObservation> referenceObservations = query.getResultList();
            final List<SamplingPoint> samples = new ArrayList<>(referenceObservations.size());
            for (final ReferenceObservation o : referenceObservations) {
                final Point p = o.getPoint().getGeometry().getPoint(0);
                samples.add(new SamplingPoint(p.getX(), p.getY(), 0, 0.0));
            }
            new SamplingPointPlotter()
                    .samples(samples)
                    .show(show)
                    .live(false)
                    .windowTitle(title)
                    .filePath(new File(targetDir, targetFilename).getPath())
                    .mapStrategyName(mapStrategyName)
                    .plot();
        } finally {
            persistenceManager.commit();
        }
    }
}
