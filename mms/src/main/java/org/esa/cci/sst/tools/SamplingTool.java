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

import gov.nasa.gsfc.seadas.watermask.operator.WatermaskClassifier;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.YFlip;
import org.esa.cci.sst.util.NcUtils;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SobolSequenceGenerator;
import org.esa.cci.sst.util.TimeUtil;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.net.URISyntaxException;
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

        for (int i  = 0; i < sampleCount; i++) {
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
        final WatermaskClassifier classifier;
        try {
            classifier = new WatermaskClassifier(WatermaskClassifier.RESOLUTION_1km, WatermaskClassifier.Mode.GSHHS,
                                                 "GSHHS_water_mask_1km.zip");
        } catch (IOException e) {
            throw new ToolException("Unable to create land/water classifier.", e, ToolException.TOOL_IO_ERROR);
        }

        for (Iterator<SamplingPoint> iterator = sampleList.iterator(); iterator.hasNext(); ) {
            final SamplingPoint point = iterator.next();

            try {
                final boolean water = classifier.isWater((float) point.getLat(), (float) point.getLon());
                if (!water) {
                    iterator.remove();
                }
            } catch (IOException ignored) {
                // cannot happen
            }
        }
    }

    public void reduceClearSamples(List<SamplingPoint> sampleList) {
        final GridDef gridDef = GridDef.createGlobal(1.0);

        final NetcdfFile file;
        try {
            file = NetcdfFile.openInMemory(getClass().getResource("AATSR_prior_run081222.nc").toURI());
        } catch (IOException e) {
            throw new ToolException("Cannot read cloud priors.", e, ToolException.TOOL_IO_ERROR);
        } catch (URISyntaxException e) {
            throw new ToolException("Cannot read cloud priors.", e, ToolException.TOOL_IO_ERROR);
        }

        final Grid grid;
        try {
            grid = YFlip.create(NcUtils.readGrid(file, "clr_prior", gridDef));
            for (Iterator<SamplingPoint> iterator = sampleList.iterator(); iterator.hasNext(); ) {
                final SamplingPoint point = iterator.next();

                final int x = gridDef.getGridX(point.getLon(), true);
                final int y = gridDef.getGridY(point.getLat(), true);
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
