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

package org.esa.cci.sst.tools.nwp;

import org.esa.beam.util.math.FracIndex;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TemplateResolver;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;

/**
 * Provides some helper methods for nwp generation.
 *
 * @author Ralf Quast
 * @author Thomas Storm
 */
class NwpUtil {

    private NwpUtil() {
    }

    static float getAttribute(Variable s, String name, float defaultValue) {
        final Attribute a = s.findAttribute(name);
        if (a == null) {
            return defaultValue;
        }
        return a.getNumericValue().floatValue();
    }

    static FracIndex interpolationIndex(Array sourceTimes, int targetTime) {
        for (int i = 1; i < sourceTimes.getSize(); i++) {
            final double maxTime = sourceTimes.getDouble(i);
            final double minTime = sourceTimes.getDouble(i - 1);
            if (targetTime >= minTime && targetTime <= maxTime) {
                final FracIndex index = new FracIndex();
                index.i = i - 1;
                index.f = (targetTime - minTime) / (maxTime - minTime);
                return index;
            }
        }
        System.out.println("sourceTimes[0] = " + sourceTimes.getInt(0));
        System.out.println("sourceTimes[n] = " + sourceTimes.getInt((int) (sourceTimes.getSize() - 1)));
        System.out.println("targetTime = " + targetTime);
        throw new ToolException("Not enough time steps in NWP time series.", ToolException.TOOL_ERROR);
    }

    static void writeAnalysisMmdFile(NetcdfFile mmd, NetcdfFile analysisFile,
                                     String anTargetLocation, int pastTimeStepCount, int futureTimeStepCount) throws IOException {
        final Dimension matchupDimension = findDimension(mmd, "matchup");
        final Dimension yDimension = findDimension(analysisFile, "y");
        final Dimension xDimension = findDimension(analysisFile, "x");

        final int matchupCount = matchupDimension.getLength();
        final int gy = yDimension.getLength() / matchupCount;
        final int gx = xDimension.getLength();

        final NetcdfFileWriteable anMmd = NetcdfFileWriteable.createNew(anTargetLocation, true);
        anMmd.addDimension(matchupDimension.getName(), matchupCount);

        final int timeStepCount = pastTimeStepCount + futureTimeStepCount + 1;
        anMmd.addDimension("matchup.nwp.an.time", timeStepCount);
        anMmd.addDimension("matchup.nwp.ny", gy);
        anMmd.addDimension("matchup.nwp.nx", gx);

        final Variable matchupId = findVariable(mmd, "matchup.id");
        anMmd.addVariable(matchupId.getName(), matchupId.getDataType(), matchupId.getDimensionsString());

        for (final Variable s : analysisFile.getVariables()) {
            if (s.getRank() == 4) {
                if (s.getDimension(1).getLength() == 1) {
                    final Variable t = anMmd.addVariable("matchup.nwp.an." + s.getName(), s.getDataType(),
                                                         "matchup matchup.nwp.an.time matchup.nwp.ny matchup.nwp.nx");
                    for (final Attribute a : s.getAttributes()) {
                        t.addAttribute(a);
                    }
                }
            }
        }

        anMmd.create();

        final Array matchupIds = findVariable(mmd, "matchup.id").read();
        final Array targetTimes = findVariable(mmd, "matchup.time").read();
        final Array sourceTimes = findVariable(analysisFile, "time").read();

        try {
            anMmd.write(NetcdfFile.escapeName("matchup.id"), matchupIds);

            final int[] sourceShape = {timeStepCount, 1, gy, gx};
            for (int i = 0; i < matchupCount; i++) {
                final int targetTime = targetTimes.getInt(i);
                final int timeStep = nearestTimeStep(sourceTimes, targetTime);

                if (timeStep - pastTimeStepCount < 0 || timeStep + futureTimeStepCount > sourceTimes.getSize() - 1) {
                    throw new ToolException("Not enough time steps in NWP time series.", ToolException.TOOL_ERROR);
                }

                final int[] sourceStart = {timeStep - pastTimeStepCount, 0, i * gy, 0};

                for (final Variable t : anMmd.getVariables()) {
                    if ("matchup.id".equals(t.getName())) {
                        continue;
                    }
                    final Variable s = findVariable(analysisFile, t.getName().substring("matchup.nwp.an.".length()));
                    final Array sourceData = s.read(sourceStart, sourceShape);

                    final int[] targetShape = t.getShape();
                    targetShape[0] = 1;
                    final int[] targetStart = new int[targetShape.length];
                    targetStart[0] = i;
                    anMmd.write(t.getNameEscaped(), targetStart, sourceData.reshape(targetShape));
                }
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            try {
                anMmd.close();
            } catch (IOException ignored) {
            }
        }
    }

    static void writeForecastMmdFile(NetcdfFile mmd, NetcdfFile forecastFile,
                                     String fcTargetLocation, int pastTimeStepCount, int futureTimeStepCount) throws IOException {
        final Dimension matchupDimension = findDimension(mmd, "matchup");
        final Dimension yDimension = findDimension(forecastFile, "y");
        final Dimension xDimension = findDimension(forecastFile, "x");

        final int matchupCount = matchupDimension.getLength();
        final int gy = yDimension.getLength() / matchupCount;
        final int gx = xDimension.getLength();

        final NetcdfFileWriteable fcMmd = NetcdfFileWriteable.createNew(fcTargetLocation, true);
        fcMmd.addDimension(matchupDimension.getName(), matchupCount);

        final int timeStepCount = pastTimeStepCount + futureTimeStepCount + 1;
        fcMmd.addDimension("matchup.nwp.fc.time", timeStepCount);
        fcMmd.addDimension("matchup.nwp.ny", gy);
        fcMmd.addDimension("matchup.nwp.nx", gx);

        final Variable matchupId = findVariable(mmd, "matchup.id");
        fcMmd.addVariable(matchupId.getName(), matchupId.getDataType(), matchupId.getDimensionsString());

        for (final Variable s : forecastFile.getVariables()) {
            if (s.getRank() == 4) {
                if (s.getDimension(1).getLength() == 1) {
                    final Variable t = fcMmd.addVariable("matchup.nwp.fc." + s.getName(), s.getDataType(),
                                                         "matchup matchup.nwp.fc.time matchup.nwp.ny matchup.nwp.nx");
                    for (final Attribute a : s.getAttributes()) {
                        t.addAttribute(a);
                    }
                }
            }
        }

        fcMmd.create();

        final Array matchupIds = findVariable(mmd, "matchup.id").read();
        final Array targetTimes = findVariable(mmd, "matchup.time").read();
        final Array sourceTimes = findVariable(forecastFile, "time").read();

        try {
            fcMmd.write(NetcdfFile.escapeName("matchup.id"), matchupIds);

            final int[] sourceShape = {timeStepCount, 1, gy, gx};
            for (int i = 0; i < matchupCount; i++) {
                final int targetTime = targetTimes.getInt(i);
                final int timeStep = nearestTimeStep(sourceTimes, targetTime);

                if (timeStep - pastTimeStepCount < 0 || timeStep + futureTimeStepCount > sourceTimes.getSize() - 1) {
                    throw new ToolException("Not enough time steps in NWP time series.", ToolException.TOOL_ERROR);
                }

                final int[] sourceStart = {timeStep - pastTimeStepCount, 0, i * gy, 0};

                for (final Variable t : fcMmd.getVariables()) {
                    if ("matchup.id".equals(t.getName())) {
                        continue;
                    }
                    final Variable s = findVariable(forecastFile, t.getName().substring("matchup.nwp.fc.".length()));
                    final Array sourceData = s.read(sourceStart, sourceShape);

                    final int[] targetShape = t.getShape();
                    targetShape[0] = 1;
                    final int[] targetStart = new int[targetShape.length];
                    targetStart[0] = i;
                    fcMmd.write(t.getNameEscaped(), targetStart, sourceData.reshape(targetShape));
                }
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            try {
                fcMmd.close();
            } catch (IOException ignored) {
            }
        }
    }

    static int nearestTimeStep(Array sourceTimes, int targetTime) {
        int timeStep = 0;
        int minTimeDelta = Math.abs(targetTime - sourceTimes.getInt(0));

        for (int i = 1; i < sourceTimes.getSize(); i++) {
            final int sourceTime = sourceTimes.getInt(i);
            final int actTimeDelta = Math.abs(targetTime - sourceTime);
            if (actTimeDelta < minTimeDelta) {
                minTimeDelta = actTimeDelta;
                timeStep = i;
            }
        }

        return timeStep;
    }

    static File createTempFile(String prefix, String suffix, boolean deleteOnExit) throws IOException {
        final File tempFile = File.createTempFile(prefix, suffix);
        if (deleteOnExit) {
            tempFile.deleteOnExit();
        }
        return tempFile;
    }

    static String files(final String dirPath, List<String> subDirectories, final String pattern) {
        final StringBuilder sb = new StringBuilder();
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(pattern);
            }
        };
        for (String subDirectory : subDirectories) {
            final File dir = new File(dirPath, subDirectory);
            final File[] files = dir.listFiles(filter);
            for (final File file : files) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(file.getPath());
            }
        }
        return sb.toString();
    }

    static File writeCdoScript(String template, Properties properties) throws IOException {
        final File script = File.createTempFile("cdo", ".sh");
        final boolean executable = script.setExecutable(true);
        if (!executable) {
            throw new IOException("Cannot create CDO script.");
        }
        final Writer writer = new FileWriter(script);
        try {
            final TemplateResolver templateResolver = new TemplateResolver(properties);
            writer.write(templateResolver.resolve(template));
        } finally {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
        return script;
    }

    static void addVariable(NetcdfFileWriteable netcdfFile, Variable s) {
        final Variable t = netcdfFile.addVariable(s.getName(), s.getDataType(), s.getDimensionsString());
        for (final Attribute a : s.getAttributes()) {
            t.addAttribute(a);
        }
    }

    static Dimension findDimension(NetcdfFile file, String name) throws IOException {
        final Dimension d = file.findDimension(name);
        if (d == null) {
            throw new IOException(MessageFormat.format("Expected dimension ''{0}''.", name));
        }
        return d;
    }

    static Variable findVariable(NetcdfFile file, String name) throws IOException {
        final Variable v = file.findVariable(NetcdfFile.escapeName(name));
        if (v == null) {
            throw new IOException(MessageFormat.format("Expected variable ''{0}''.", name));
        }
        return v;
    }
}
