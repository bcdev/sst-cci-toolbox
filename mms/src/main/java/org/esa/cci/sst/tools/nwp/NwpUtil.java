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
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Provides some helper methods for nwp generation.
 *
 * @author Ralf Quast
 * @author Thomas Storm
 */
class NwpUtil {

    private NwpUtil() {
    }

    static List<String> getRelevantNwpDirs(Variable timeVariable) throws IOException {
        final Number fillValue = timeVariable.findAttribute("_FillValue").getNumericValue();
        int startTime = Integer.MAX_VALUE;
        int endTime = Integer.MIN_VALUE;
        final Array times = timeVariable.read();
        for (int i = 0; i < times.getSize(); i++) {
            final int currentTime = times.getInt(i);
            if (currentTime != fillValue.intValue()) {
                if (currentTime < startTime) {
                    startTime = currentTime;
                }
                if (currentTime > endTime) { // using else if results is not correct, if there is a single time step only
                    endTime = currentTime;
                }
            }
        }

        // TODO - throw exception, if start time or end time are invalid

        final int seventyTwoHours = 72 * 60 * 60;
        final int fortyEightHours = 48 * 60 * 60;
        final Date startDate = TimeUtil.secondsSince1978ToDate(startTime - seventyTwoHours);
        final Date stopDate = TimeUtil.secondsSince1978ToDate(endTime + fortyEightHours);
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTime(startDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        final List<String> dirs = new ArrayList<>();
        while (!calendar.getTime().after(stopDate)) {
            dirs.add(simpleDateFormat.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return dirs;
    }

    static int getAttribute(Variable s, String name, int defaultValue) {
        final Attribute a = s.findAttribute(name);
        if (a == null) {
            return defaultValue;
        }
        return a.getNumericValue().intValue();
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

    static void copyValues(Map<Variable, Variable> map,
                                   NetcdfFileWriter targetFile,
                                   int targetMatchup,
                                   int[] sourceStart,
                                   int[] sourceShape) throws IOException, InvalidRangeException {
        for (final Variable targetVariable : map.keySet()) {
            final Variable sourceVariable = map.get(targetVariable);
            final Array sourceData = sourceVariable.read(sourceStart, sourceShape);
            final int[] targetShape = targetVariable.getShape();
            targetShape[0] = 1;
            final int[] targetStart = new int[targetShape.length];
            targetStart[0] = targetMatchup;
            targetFile.write(targetVariable, targetStart, sourceData.reshape(targetShape));
        }
    }

    static void copyVariables(NetcdfFile source, NetcdfFileWriter target, Map<Variable, Variable> map,
                                      final String id) {
        for (final Variable s : source.getVariables()) {
            if (s.getRank() == 4) {
                if (s.getDimension(1).getLength() == 1) {
                    final String sourceName = s.getShortName();
                    final String targetName = "matchup.nwp." + id + "." + sourceName;
                    final String dimensions = "matchup matchup.nwp." + id + ".time matchup.nwp.ny matchup.nwp.nx";
                    final Variable t = target.addVariable(null, targetName, s.getDataType(), dimensions);
                    for (final Attribute a : s.getAttributes()) {
                        t.addAttribute(a);
                    }
                    map.put(t, s);
                }
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

    static String composeFilesString(final String dirPath, final List<String> subDirectories, final String pattern) {
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
            if (files == null) {
                throw new RuntimeException(String.format("%s directory does not exist", dir.getPath()));
            }
            for (final File file : files) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(file.getPath());
            }
        }
        return sb.toString();
    }

    static void addVariable(NetcdfFileWriter netcdfFile, Variable s) {
        final Variable t = netcdfFile.addVariable(null, s.getShortName(), s.getDataType(), s.getDimensionsString());
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

    static Variable findVariable(NetcdfFile file, String... names) throws IOException {
        for (final String name : names) {
            final Variable v = file.findVariable(NetcdfFile.makeValidPathName(name));
            if (v != null) {
                return v;
            }
        }
        throw new IOException(
                MessageFormat.format("Expected to find any variable in ''{0}''.", Arrays.toString(names)));
    }
}
