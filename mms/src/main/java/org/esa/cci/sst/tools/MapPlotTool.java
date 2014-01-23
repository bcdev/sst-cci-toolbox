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
import org.esa.cci.sst.util.TimeUtil;

import javax.imageio.ImageIO;
import javax.persistence.Query;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
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
                    + " from ReferenceObservation o, Matchup m"
                    + " where m.refObs = o and o.sensor = 'sobol' and o.time >= ?2 and o.time < ?3 and exists ( select o2 from Observation o2, Coincidence c2 where c2.matchup = m and c2.observation = o2 and o2.sensor = ?1 )"
                    + " order by o.time, m.id";

    private String samplingSensor;
    private Date startTime;
    private Date stopTime;
    private boolean showMapsFlag;

    MapPlotTool() {
        super("mapplot-tool", "1.0");
    }

    public static void main(String[] args) {
        final MapPlotTool tool = new MapPlotTool();
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
        samplingSensor = getConfiguration().getProperty("mms.sampling.sensor",
                                                        "atsr_orb.3");
        final String startTimeString = getConfiguration().getProperty("mms.sampling.startTime",
                                                                      "2004-06-12T00:00:00Z");
        final String stopTimeString = getConfiguration().getProperty("mms.sampling.stopTime",
                                                                     "2004-06-14T00:00:00Z");
        final String showMapsString = getConfiguration().getProperty("mms.sampling.showmaps",
                                                                     "true");
        try {
            startTime = TimeUtil.parseCcsdsUtcFormat(startTimeString);
            stopTime = TimeUtil.parseCcsdsUtcFormat(stopTimeString);
            showMapsFlag = Boolean.valueOf(showMapsString);
        } catch (ParseException e) {
            throw new ToolException("Unable to parse sampling start and stop times.", e,
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
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
            final String imageName = "sobol-" + samplingSensor + "-" + TimeUtil.formatCompactUtcFormat(startTime) + ".png";
            plotSamples(refobsList, imageName, showMapsFlag ? imageName : null);

            final List<ReferenceObservation> orbits = findOrbits(TimeUtil.formatCcsdsUtcFormat(startTime), TimeUtil.formatCcsdsUtcFormat(stopTime));
            int noOrbitsToPlot = 14;
            for (ReferenceObservation orbit : orbits) {
                final DataFile orbitDataFile = orbit.getDatafile();
                List<ReferenceObservation> orbitSamples = filter(refobsList, new Predicate<ReferenceObservation>() {
                    @Override
                    public boolean apply(ReferenceObservation s) {
                        return s.getDatafile() == orbitDataFile;
                    }
                });
                final String orbitImageName = "sobol-" + orbitDataFile.getPath().substring(orbitDataFile.getPath().lastIndexOf(File.separator) + 1) + "-" + TimeUtil.formatCompactUtcFormat(startTime) + ".png";
                plotSamples(orbitSamples, orbitImageName, showMapsFlag ? orbitImageName : null);
                if (--noOrbitsToPlot <= 0) {
                    break;
                }
            }
        } finally {
            persistenceManager.commit();
        }

    }

    private static void plotSamples(List<ReferenceObservation> samples, String title, String imagePath)
            throws IOException {
        final int w = 800;
        final int h = 400;
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        final JLabel label = new JLabel(new ImageIcon(image));

        final JFrame frame = new JFrame();
        frame.setTitle(title);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(label);
        frame.setSize(w, h);
        frame.setVisible(true);

        final Graphics2D graphics = image.createGraphics();

        for (ReferenceObservation p : samples) {
            final double x = (p.getLocation().getGeometry().getFirstPoint().getX() + 180.0) / 360.0;
            final double y = (90.0 - p.getLocation().getGeometry().getFirstPoint().getY()) / 180.0;
            final int i = (int) (y * h);
            final int k = (int) (x * w);
            graphics.fill(new Rectangle(k, i, 1, 1));
            label.repaint();

            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
                // ignore
            }
        }

        if (imagePath != null) {
            ImageIO.write(image, "png", new File(imagePath));
        }
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
