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

import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.YFlip;
import org.esa.cci.sst.util.NcUtils;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SobolSequenceGenerator;
import org.esa.cci.sst.util.TimeUtil;
import ucar.nc2.NetcdfFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SamplingTool extends BasicTool {

    private long startTime;
    private long stopTime;
    private int sampleCount;

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
        //super.initialize();
        final String startTimeString = getConfiguration().getProperty("mms.sampling.startTime",
                                                                      "2003-01-01T00:00:00Z");
        final String stopTimeString = getConfiguration().getProperty("mms.sampling.stopTime",
                                                                     "2003-01-02T00:00:00Z");
        final String countString = getConfiguration().getProperty("mms.sampling.count", "10000");

        try {
            startTime = TimeUtil.parseCcsdsUtcFormat(startTimeString).getTime();
            stopTime = TimeUtil.parseCcsdsUtcFormat(stopTimeString).getTime();
            sampleCount = Integer.parseInt(countString);
        } catch (ParseException e) {
            throw new ToolException("Unable to parse sampling start and stop times.", e,
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        } catch (NumberFormatException e) {
            throw new ToolException("Unable to parse sample counts.", e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    private void run() {
        createSamples();
    }

    List<SamplingPoint> createSamples() {
        final SobolSequenceGenerator sequenceGenerator = new SobolSequenceGenerator(4);
        final List<SamplingPoint> sampleList = new ArrayList<SamplingPoint>();

        for (int i = 0; i < sampleCount; i++) {
            final double[] sample = sequenceGenerator.nextVector();
            final double x = sample[0];
            final double y = sample[1];
            final double t = sample[2];
            final double random = sample[3];

            final double lon = x * 360.0 - 180.0;
            final double lat = 90.0 - y * 180.0;
            final long time = (long) (t * (stopTime - startTime)) + startTime;

            sampleList.add(new SamplingPoint(lon, lat, time, random));
        }

        return sampleList;
    }

    void removeLandSamples(List<SamplingPoint> sampleList) {
        final BufferedImage waterImage;
        try {
            final URL url = getClass().getResource("water.png");
            waterImage = ImageIO.read(url);
        } catch (IOException e) {
            throw new ToolException("Unable to read land/water mask image.", e, ToolException.TOOL_IO_ERROR);
        }

        final GridDef gridDef = GridDef.createGlobal(0.01);

        for (Iterator<SamplingPoint> iterator = sampleList.iterator(); iterator.hasNext(); ) {
            final SamplingPoint point = iterator.next();

            final int x = gridDef.getGridX(point.getLon(), true);
            final int y = gridDef.getGridY(point.getLat(), true);
            final int sample = waterImage.getRaster().getSample(x, y, 0);
            if (sample == 0) {
                iterator.remove();
            }
        }
    }

    public void reduceClearSamples(List<SamplingPoint> sampleList) {
        final NetcdfFile file;
        try {
            file = NetcdfFile.openInMemory(getClass().getResource("AATSR_prior_run081222.nc").toURI());
        } catch (IOException e) {
            throw new ToolException("Cannot read cloud priors.", e, ToolException.TOOL_IO_ERROR);
        } catch (URISyntaxException e) {
            throw new ToolException("Cannot read cloud priors.", e, ToolException.TOOL_IO_ERROR);
        }

        final GridDef gridDef = GridDef.createGlobal(1.0);
        final Grid grid;
        try {
            grid = YFlip.create(NcUtils.readGrid(file, "clr_prior", gridDef));
            for (Iterator<SamplingPoint> iterator = sampleList.iterator(); iterator.hasNext(); ) {
                final SamplingPoint point = iterator.next();

                final double lon = point.getLon();
                final double lat = point.getLat();
                final int x = gridDef.getGridX(lon, true);
                final int y = gridDef.getGridY(lat, true);
                // final double f = Math.abs(lat) < 30.0 ? 1.0 : Math.cos(Math.toRadians(Math.abs(lat) - 30.0));
                final double f = 0.05 / grid.getSampleDouble(x, y);
                if (point.getRandom() > f) {
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            throw new ToolException("Cannot read cloud priors.", e, ToolException.TOOL_IO_ERROR);
        } finally {
            try {
                file.close();
            } catch (IOException ignored) {
            }
        }
    }

}
