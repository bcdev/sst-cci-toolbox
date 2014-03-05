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
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.ProcessRunner;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

/**
 * NWP extraction tool.
 *
 * @author Ralf Quast
 */
class NwpTool extends BasicTool {

    private static final String CDO_NWP_TEMPLATE =
            "#! /bin/sh\n" +
            "${CDO} ${CDO_OPTS} -f nc selname,CI,ASN,SSTK,TCWV,MSL,TCC,U10,V10,T2,D2,AL,SKT -mergetime ${GGAS_TIMESTEPS} ${GGAS_TIME_SERIES} && " +
            "${CDO} ${CDO_OPTS} -f grb -t ecmwf selname,Q,O3 -mergetime ${GGAM_TIMESTEPS} ${GGAM_TIME_SERIES} && " +
            "${CDO} ${CDO_OPTS} -f grb -t ecmwf selname,LNSP,T -mergetime ${SPAM_TIMESTEPS} ${SPAM_TIME_SERIES} && " +
            // attention: chaining the operations below results in a loss of the y dimension in the result file
            "${CDO} ${CDO_OPTS} -f nc -R -t ecmwf setreftime,${REFTIME} -remapbil,${GEO} -selname,Q,O3 ${GGAM_TIME_SERIES} ${GGAM_TIME_SERIES_REMAPPED} && " +
            "${CDO} ${CDO_OPTS} -f nc -t ecmwf setreftime,${REFTIME} -remapbil,${GEO} -sp2gp -selname,LNSP,T ${SPAM_TIME_SERIES} ${SPAM_TIME_SERIES_REMAPPED} && " +
            "${CDO} ${CDO_OPTS} -f nc merge -setreftime,${REFTIME} -remapbil,${GEO} -selname,CI,ASN,SSTK,TCWV,MSL,TCC,U10,V10,T2,D2,AL,SKT ${GGAS_TIME_SERIES} ${GGAM_TIME_SERIES_REMAPPED} ${SPAM_TIME_SERIES_REMAPPED} ${NWP_TIME_SERIES}\n";

    private static final String CDO_MATCHUP_AN_TEMPLATE =
            "#! /bin/sh\n" +
            "${CDO} ${CDO_OPTS} -f nc mergetime ${GGAS_TIMESTEPS} ${GGAS_TIME_SERIES} && " +
            "${CDO} ${CDO_OPTS} -f nc setreftime,${REFTIME} -remapbil,${GEO} -selname,CI,SSTK,U10,V10 ${GGAS_TIME_SERIES} ${AN_TIME_SERIES}\n";

    private static final String CDO_MATCHUP_FC_TEMPLATE =
            "#! /bin/sh\n" +
            "${CDO} ${CDO_OPTS} -f nc mergetime ${GAFS_TIMESTEPS} ${GAFS_TIME_SERIES} && " +
            "${CDO} ${CDO_OPTS} -f nc mergetime ${GGFS_TIMESTEPS} ${GGFS_TIME_SERIES} && " +
            // attention: chaining the operations below results in a loss of the y dimension in the result file
            "${CDO} ${CDO_OPTS} -f nc setreftime,${REFTIME} -remapbil,${GEO} -selname,SSTK,MSL,BLH,U10,V10,T2,D2 ${GGFS_TIME_SERIES} ${GGFS_TIME_SERIES_REMAPPED} && " +
            "${CDO} ${CDO_OPTS} -f nc merge -setreftime,${REFTIME} -remapbil,${GEO} -selname,SSHF,SLHF,SSRD,STRD,SSR,STR,EWSS,NSSS,E,TP ${GAFS_TIME_SERIES} ${GGFS_TIME_SERIES_REMAPPED} ${FC_TIME_SERIES}\n";

    private boolean forMatchupPoints;
    private String sensorName;
    private int sensorPattern;
    private String mmdSourceLocation;
    private String nwpSourceLocation;
    private String nwpTargetLocation;
    private String geoFileLocation;
    private String anTargetLocation;
    private String fcTargetLocation;
    private String dimensionFilePath;

    NwpTool() {
        super("nwp-tool", "1.0");
    }

    public static void main(String[] args) {
        final NwpTool tool = new NwpTool();
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

        final Configuration config = getConfig();

        mmdSourceLocation = config.getStringValue(Configuration.KEY_MMS_MMD_SOURCE_LOCATION);
        nwpSourceLocation = config.getStringValue(Configuration.KEY_MMS_NWP_SOURCE_LOCATION);
        dimensionFilePath = config.getStringValue(Configuration.KEY_MMS_MMD_TARGET_DIMENSIONS);
        forMatchupPoints = config.getBooleanValue(Configuration.KEY_MMS_NWP_FOR_MATCHUP_POINTS);

        if (forMatchupPoints) {
            anTargetLocation = config.getStringValue(Configuration.KEY_MMS_NWP_AN_TARGET_LOCATION);
            fcTargetLocation = config.getStringValue(Configuration.KEY_MMS_NWP_FC_TARGET_LOCATION);
        } else {
            nwpTargetLocation = config.getStringValue(Configuration.KEY_MMS_NWP_TARGET_LOCATION);
            sensorName = config.getStringValue(Configuration.KEY_MMS_NWP_SENSOR).replace("_orb", "");
            // TODO - check for AVHRRs, patterns of orbit files and sub-scene files are different (rq-20140304)
            sensorPattern = (int) config.getPattern(sensorName);
        }
    }

    private void run() throws IOException, InterruptedException {
        final Properties dimensions = new Properties();
        dimensions.load(new BufferedReader(new FileReader(dimensionFilePath)));

        if (forMatchupPoints) {
            createMatchupAnFile(dimensions);
            createMatchupFcFile(dimensions);
        } else {
            createMergedFile(dimensions);
        }
    }

    void createMatchupAnFile(Properties dimensions) throws IOException, InterruptedException {
        final NetcdfFile mmdFile = NetcdfFile.open(mmdSourceLocation);
        final List<String> subDirectories = getNwpSubDirectories(NwpUtil.findVariable(mmdFile, "matchup.time"));

        final int timeStepCount = Integer.parseInt(dimensions.getProperty("matchup.nwp.an.time"));
        final int futureTimeStepCount = (timeStepCount - 1) / 3;
        final int pastTimeStepCount = futureTimeStepCount * 2;

        try {
            writeMatchupGeoFile(mmdFile);

            final Properties properties = new Properties();
            properties.setProperty("CDO", "cdo");
            properties.setProperty("CDO_OPTS", "-M");
            properties.setProperty("REFTIME", "1978-01-01,00:00:00,seconds");

            properties.setProperty("GEO", geoFileLocation);
            properties.setProperty("GGAS_TIMESTEPS",
                                   NwpUtil.files(nwpSourceLocation + "/ggas", subDirectories, "ggas[0-9]*.nc"));
            properties.setProperty("GGAS_TIME_SERIES", NwpUtil.createTempFile("ggas", ".nc", true).getPath());
            properties.setProperty("AN_TIME_SERIES", NwpUtil.createTempFile("analysis", ".nc", true).getPath());

            final ProcessRunner runner = new ProcessRunner("org.esa.cci.sst");
            runner.execute(NwpUtil.writeCdoScript(CDO_MATCHUP_AN_TEMPLATE, properties).getPath());

            final NetcdfFile anFile = NetcdfFile.open(properties.getProperty("AN_TIME_SERIES"));
            try {
                NwpUtil.writeAnalysisMmdFile(mmdFile, anFile, anTargetLocation, pastTimeStepCount,
                                             futureTimeStepCount);
            } finally {
                try {
                    anFile.close();
                } catch (IOException ignored) {
                }
            }
        } finally {
            try {
                mmdFile.close();
            } catch (IOException ignored) {
            }
        }
    }

    void createMatchupFcFile(Properties dimensions) throws IOException, InterruptedException {
        final NetcdfFile mmdFile = NetcdfFile.open(mmdSourceLocation);
        final List<String> subDirectories = getNwpSubDirectories(NwpUtil.findVariable(mmdFile, "matchup.time"));

        final int timeStepCount = Integer.parseInt(dimensions.getProperty("matchup.nwp.fc.time"));
        final int futureTimeStepCount = (timeStepCount - 1) / 3;
        final int pastTimeStepCount = futureTimeStepCount * 2;

        try {
            writeMatchupGeoFile(mmdFile);

            final Properties properties = new Properties();
            properties.setProperty("CDO", "cdo");
            properties.setProperty("CDO_OPTS", "-M");
            properties.setProperty("REFTIME", "1978-01-01,00:00:00,seconds");

            properties.setProperty("GEO", geoFileLocation);
            properties.setProperty("GAFS_TIMESTEPS",
                                   NwpUtil.files(nwpSourceLocation + "/gafs", subDirectories, "gafs[0-9]*.nc"));
            properties.setProperty("GGFS_TIMESTEPS",
                                   NwpUtil.files(nwpSourceLocation + "/ggfs", subDirectories, "ggfs[0-9]*.nc"));
            properties.setProperty("GAFS_TIME_SERIES", NwpUtil.createTempFile("gafs", ".nc", true).getPath());
            properties.setProperty("GGFS_TIME_SERIES", NwpUtil.createTempFile("ggfs", ".nc", true).getPath());
            properties.setProperty("GGFS_TIME_SERIES_REMAPPED", NwpUtil.createTempFile("ggfr", ".nc", true).getPath());
            properties.setProperty("FC_TIME_SERIES", NwpUtil.createTempFile("forecast", ".nc", true).getPath());

            final ProcessRunner runner = new ProcessRunner("org.esa.cci.sst");
            runner.execute(NwpUtil.writeCdoScript(CDO_MATCHUP_FC_TEMPLATE, properties).getPath());

            final NetcdfFile fcFile = NetcdfFile.open(properties.getProperty("FC_TIME_SERIES"));
            try {
                NwpUtil.writeForecastMmdFile(mmdFile, fcFile, fcTargetLocation, pastTimeStepCount,
                                             futureTimeStepCount);
            } finally {
                try {
                    fcFile.close();
                } catch (IOException ignored) {
                }
            }
        } finally {
            try {
                mmdFile.close();
            } catch (IOException ignored) {
            }
        }
    }

    void createMergedFile(Properties dimensions) throws IOException, InterruptedException {
        final NetcdfFile sensorMmdFile = NetcdfFile.open(writeSensorGeoMmdFile(sensorName, sensorPattern));
        final Variable timeVariable = NwpUtil.findVariable(sensorMmdFile, sensorName + ".time",
                                                           getAlternativeSensorName(sensorName) + ".time");
        final List<String> subDirectories = getNwpSubDirectories(timeVariable);
        final int nx;
        final int ny;
        final int nwpNx;
        final int nwpNy;

        if (sensorName.startsWith("atsr")) {
            nx = Integer.parseInt(dimensions.getProperty("atsr.nx"));
            ny = Integer.parseInt(dimensions.getProperty("atsr.ny"));
            nwpNx = Integer.parseInt(dimensions.getProperty("atsr.nwp.nx"));
            nwpNy = Integer.parseInt(dimensions.getProperty("atsr.nwp.ny"));
        } else if (sensorName.startsWith("avhrr")) {
            nx = Integer.parseInt(dimensions.getProperty("avhrr.nx"));
            ny = Integer.parseInt(dimensions.getProperty("avhrr.ny"));
            nwpNx = Integer.parseInt(dimensions.getProperty("avhrr.nwp.nx"));
            nwpNy = Integer.parseInt(dimensions.getProperty("avhrr.nwp.ny"));
        } else {
            getLogger().warning("sensor " + sensorName + " neither atsr nor avhrr - interpolating single pixel");
            nx = 1;
            ny = 1;
            nwpNx = 1;
            nwpNy = 1;
        }
        final int strideX;
        final int strideY;
        if (nwpNx > 1) {
            strideX = (nx - 1) / (nwpNx - 1);
        } else {
            strideX = 1;
        }
        if (nwpNy > 1) {
            strideY = (ny - 1) / (nwpNy - 1);
        } else {
            strideY = 1;
        }

        try {
            writeSensorGeoFile(sensorMmdFile, nwpNx, nwpNy, strideX, strideY);

            final Properties properties = new Properties();
            properties.setProperty("CDO", "cdo");
            properties.setProperty("CDO_OPTS", "-M");
            properties.setProperty("REFTIME", "1978-01-01,00:00:00,seconds");

            properties.setProperty("GEO", geoFileLocation);
            properties.setProperty("GGAS_TIMESTEPS",
                                   NwpUtil.files(nwpSourceLocation + "/ggas", subDirectories, "ggas[0-9]*.nc"));
            properties.setProperty("GGAM_TIMESTEPS",
                                   NwpUtil.files(nwpSourceLocation + "/ggam", subDirectories, "ggam[0-9]*.grb"));
            properties.setProperty("SPAM_TIMESTEPS",
                                   NwpUtil.files(nwpSourceLocation + "/spam", subDirectories, "spam[0-9]*.grb"));
            properties.setProperty("GGAS_TIME_SERIES", NwpUtil.createTempFile("ggas", ".nc", false).getPath());
            properties.setProperty("GGAM_TIME_SERIES", NwpUtil.createTempFile("ggam", ".grb", false).getPath());
            properties.setProperty("SPAM_TIME_SERIES", NwpUtil.createTempFile("spam", ".grb", false).getPath());
            properties.setProperty("GGAM_TIME_SERIES_REMAPPED", NwpUtil.createTempFile("ggar", ".nc", false).getPath());
            properties.setProperty("SPAM_TIME_SERIES_REMAPPED", NwpUtil.createTempFile("spar", ".nc", false).getPath());
            properties.setProperty("NWP_TIME_SERIES", NwpUtil.createTempFile("nwp", ".nc", false).getPath());

            final ProcessRunner runner = new ProcessRunner("org.esa.cci.sst");
            final String path = NwpUtil.writeCdoScript(CDO_NWP_TEMPLATE, properties).getPath();
            runner.execute(path);

            final NetcdfFile nwpFile = NetcdfFile.open(properties.getProperty("NWP_TIME_SERIES"));
            try {
                getLogger().info(MessageFormat.format("Starting to write NWP MMD file: {0}", nwpTargetLocation));
                writeNwpMmdFile(nwpFile, sensorName);
                getLogger().info(MessageFormat.format("Finished writing NWP MMD file: {0}", nwpTargetLocation));
            } finally {
                try {
                    nwpFile.close();
                } catch (IOException ignored) {
                }
            }
        } finally {
            try {
                sensorMmdFile.close();
            } catch (IOException ignored) {
            }
        }
    }

    private List<String> getNwpSubDirectories(Variable timeVariable) throws IOException {
        final Number fillValue = timeVariable.findAttribute("_FillValue").getNumericValue();
        int startTime = Integer.MAX_VALUE;
        int endTime = Integer.MIN_VALUE;
        final Array times = timeVariable.read();
        for (int i = 0; i < times.getSize(); i++) {
            final int currentTime = times.getInt(i);
            if (currentTime != fillValue.intValue()) {
                if (currentTime < startTime) {
                    startTime = currentTime;
                } else if (currentTime > endTime) {
                    endTime = currentTime;
                }
            }
        }
        final Date startDate = TimeUtil.secondsSince1978ToDate(startTime - 60 * 60 * 24 * 3);
        final Date endDate = TimeUtil.secondsSince1978ToDate(endTime + 60 * 60 * 24 * 2);
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTime(startDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        final List<String> subDirectories = new ArrayList<>();
        while (!calendar.getTime().after(endDate)) {
            subDirectories.add(simpleDateFormat.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return subDirectories;
    }

    private void writeNwpMmdFile(NetcdfFile nwpSourceFile, String sensorName) throws IOException {
        final NetcdfFile mmd = NetcdfFile.open(mmdSourceLocation);
        final NetcdfFileWriter mmdNwp = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, nwpTargetLocation);

        try {
            copySensorVariablesStructure(sensorName, mmd, mmdNwp);
            copyNwpStructure(nwpSourceFile, sensorName, mmd, mmdNwp);

            mmdNwp.create();

            copySensorVariablesData(mmd, mmdNwp);
            copyNwpData(nwpSourceFile, sensorName, mmd, mmdNwp);
        } catch (IOException e) {
            final String message = MessageFormat.format("Failed to write NWP MMD file: {0} ({1})", nwpTargetLocation,
                                                        e.getMessage());
            getLogger().warning(message);
            throw new IOException(message, e);
        } finally {
            try {
                mmdNwp.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    private void copyNwpData(NetcdfFile nwpSourceFile, String sensorName, NetcdfFile mmd,
                             NetcdfFileWriter mmdNwp) throws IOException {
        final Dimension yDimension = NwpUtil.findDimension(nwpSourceFile, "y");
        final Dimension xDimension = NwpUtil.findDimension(nwpSourceFile, "x");

        final int matchupCount = getMatchupCount(
                mmd.findVariable(NetcdfFile.makeValidPathName("matchup.sensor_list")).read());
        final int gy = yDimension.getLength() / matchupCount;
        final int gx = xDimension.getLength();

        final Variable targetTimesVariable = NwpUtil.findVariable(mmdNwp.getNetcdfFile(),
                                                                  sensorName + ".time",
                                                                  getAlternativeSensorName(sensorName) + ".time");
        final Array matchupIds = NwpUtil.findVariable(mmdNwp.getNetcdfFile(), "matchup.id").read();
        final Array sourceTimes = NwpUtil.findVariable(nwpSourceFile, "time", "t").read();
        final Array targetTimes = targetTimesVariable.read();
        final float targetFillValue = NwpUtil.getAttribute(targetTimesVariable, "_FillValue", Short.MIN_VALUE);

        try {
            mmdNwp.write(mmdNwp.findVariable(NetcdfFile.makeValidPathName("matchup.id")), matchupIds);

            for (int i = 0; i < matchupCount; i++) {
                final int[] sourceStart = {0, 0, i * gy, 0};
                final int[] sourceShape = {1, 0, gy, gx};

                final int targetTime = targetTimes.getInt(i);
                if (targetTime == (int) targetFillValue) {
                    continue;
                }

                final FracIndex fi = NwpUtil.interpolationIndex(sourceTimes, targetTime);

                for (final Variable targetVariable : mmdNwp.getNetcdfFile().getVariables()) {
                    if ("matchup.id".equals(targetVariable.getShortName()) || !targetVariable.getShortName().contains(
                            ".nwp.")) {
                        continue;
                    }
                    final Variable sourceVariable = NwpUtil.findVariable(nwpSourceFile,
                                                                         targetVariable.getShortName().substring(
                                                                                 sensorName.length() + 5));
                    final float fillValue = NwpUtil.getAttribute(sourceVariable, "_FillValue", 2.0E+20F);
                    final float validMin = NwpUtil.getAttribute(sourceVariable, "valid_min", Float.NEGATIVE_INFINITY);
                    final float validMax = NwpUtil.getAttribute(sourceVariable, "valid_max", Float.POSITIVE_INFINITY);

                    sourceStart[0] = fi.i;
                    sourceShape[1] = sourceVariable.getShape(1);

                    final Array slice1 = sourceVariable.read(sourceStart, sourceShape);
                    sourceStart[0] = fi.i + 1;
                    final Array slice2 = sourceVariable.read(sourceStart, sourceShape);
                    for (int k = 0; k < slice1.getSize(); k++) {
                        final float v1 = slice1.getFloat(k);
                        final float v2 = slice2.getFloat(k);
                        final boolean invalid1 = v1 == fillValue || v1 < validMin || v1 > validMax;
                        final boolean invalid2 = v2 == fillValue || v2 < validMin || v2 > validMax;
                        if (invalid1 && invalid2) {
                            slice2.setFloat(k, fillValue);
                        } else if (invalid1) {
                            // do nothing, value is already set
                        } else if (invalid2) {
                            slice2.setFloat(k, v1);
                        } else {
                            slice2.setDouble(k, (1.0 - fi.f) * v1 + fi.f * v2);
                        }
                    }

                    final int[] targetShape = targetVariable.getShape();
                    targetShape[0] = 1;
                    final int[] targetStart = new int[targetShape.length];
                    targetStart[0] = i;
                    mmdNwp.write(targetVariable, targetStart, slice2.reshape(targetShape));
                }
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            try {
                mmdNwp.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void copyNwpStructure(NetcdfFile nwpSourceFile, String sensorName, NetcdfFile mmd,
                                  NetcdfFileWriter mmdNwp) throws IOException {
        final Dimension yDimension = NwpUtil.findDimension(nwpSourceFile, "y");
        final Dimension xDimension = NwpUtil.findDimension(nwpSourceFile, "x");
        final Dimension levDimension = NwpUtil.findDimension(nwpSourceFile, "lev");

        final int matchupCount = getMatchupCount(
                mmd.findVariable(NetcdfFile.makeValidPathName("matchup.sensor_list")).read());
        final int gy = yDimension.getLength() / matchupCount;
        final int gx = xDimension.getLength();
        final int gz = levDimension.getLength();

        final String sensorBasename = sensorName.replaceAll("\\..+", "");
        mmdNwp.addDimension(null, sensorBasename + ".nwp.nx", gx);
        mmdNwp.addDimension(null, sensorBasename + ".nwp.ny", gy);
        mmdNwp.addDimension(null, sensorBasename + ".nwp.nz", gz);

        for (final Variable sourceVariable : nwpSourceFile.getVariables()) {
            if (sourceVariable.getRank() == 4) {
                final String targetVariableName = sensorName + ".nwp." + sourceVariable.getShortName();
                final Variable targetVariable;
                if (sourceVariable.getDimension(1).getLength() == 1) {
                    final String dims = String.format("matchup %s.nwp.ny %s.nwp.nx",
                                                      sensorBasename, sensorBasename);
                    targetVariable = mmdNwp.addVariable(null, targetVariableName, sourceVariable.getDataType(), dims);
                } else {
                    final String dims = String.format("matchup %s.nwp.nz %s.nwp.ny %s.nwp.nx",
                                                      sensorBasename, sensorBasename, sensorBasename);
                    targetVariable = mmdNwp.addVariable(null, targetVariableName, sourceVariable.getDataType(), dims);
                }
                for (final Attribute attribute : sourceVariable.getAttributes()) {
                    targetVariable.addAttribute(attribute);
                }
            }
        }
    }

    private void copySensorVariablesData(NetcdfFile mmd, NetcdfFileWriter nwpMmd) throws IOException {
        final List<Integer> sensorMatchups = new ArrayList<>(10000);
        final Array array = mmd.findVariable(NetcdfFile.makeValidPathName("matchup.sensor_list")).read();
        for (int i = 0; i < array.getSize(); i++) {
            if ((array.getInt(i) & sensorPattern) == sensorPattern) {
                sensorMatchups.add(i);
            }
        }
        try {
            for (final Variable targetVariable : nwpMmd.getNetcdfFile().getVariables()) {
                final Variable sourceVariable = mmd.findVariable(targetVariable.getFullNameEscaped());
                if (sourceVariable == null) {
                    continue;
                }
                int[] sourceOrigin = new int[targetVariable.getRank()];
                for (int i = 0; i < sensorMatchups.size(); i++) {
                    sourceOrigin[0] = sensorMatchups.get(i);
                    final int[] shape = targetVariable.getShape();
                    shape[0] = 1;
                    final Array sourceArray = sourceVariable.read(sourceOrigin, shape);
                    int[] targetOrigin = new int[targetVariable.getRank()];
                    targetOrigin[0] = i;
                    nwpMmd.write(targetVariable, targetOrigin, sourceArray);
                }
            }
        } catch (InvalidRangeException e) {
            throw new IOException("Unable to copy variables.", e);
        }
    }

    private void copySensorVariablesStructure(String sensorName, NetcdfFile mmd, NetcdfFileWriter mmdNwp) throws
                                                                                                          IOException {
        final int matchupCount = getMatchupCount(
                mmd.findVariable(NetcdfFile.makeValidPathName("matchup.sensor_list")).read());
        for (Dimension dimension : mmd.getDimensions()) {
            final String dimensionName = dimension.getShortName();
            if (dimensionName.startsWith(sensorName.substring(0, sensorName.indexOf('.'))) && !dimensionName.equals(
                    "matchup")) {
                mmdNwp.addDimension(null, dimensionName, dimension.getLength());
            } else if (dimensionName.equals("matchup")) {
                mmdNwp.addDimension(null, dimensionName, matchupCount);
            } else if (dimensionName.equals("filename_length")) {
                mmdNwp.addDimension(null, dimensionName, dimension.getLength());
            }
        }
        for (final Variable sourceVariable : mmd.getVariables()) {
            final String variableName = sourceVariable.getShortName();
            if (variableName.startsWith(sensorName) || variableName.startsWith(
                    getAlternativeSensorName(sensorName)) || variableName.equals("matchup.id")) {
                final DataType dataType = sourceVariable.getDataType();
                final String dimensionsString = sourceVariable.getDimensionsString();
                final Variable targetVariable = mmdNwp.addVariable(null, variableName, dataType, dimensionsString);
                for (Attribute attribute : sourceVariable.getAttributes()) {
                    targetVariable.addAttribute(attribute);
                }
            }
        }
        for (Attribute attribute : mmd.getGlobalAttributes()) {
            mmdNwp.addGroupAttribute(null, attribute);
        }
    }

    /**
     * Writes the latitude, longitude and time for those records in an MMD file that correspond
     * to a certain sensor to a new MMD file.
     *
     * @param sensorName    The sensor name.
     * @param sensorPattern The sensor pattern.
     *
     * @return the location of the netCDF file written.
     *
     * @throws java.io.IOException when an error occurred.
     */
    @SuppressWarnings({"ConstantConditions"})
    private String writeSensorGeoMmdFile(String sensorName, int sensorPattern) throws IOException {
        final NetcdfFile sourceMmd = NetcdfFile.open(mmdSourceLocation);

        try {
            final Dimension matchupDimension = NwpUtil.findDimension(sourceMmd, "matchup");
            final String sensorBasename = sensorName.replaceAll("\\..+", "");
            final Dimension nyDimension = NwpUtil.findDimension(sourceMmd, sensorBasename + ".ny");
            final Dimension nxDimension = NwpUtil.findDimension(sourceMmd, sensorBasename + ".nx");

            final Array sensorPatterns = NwpUtil.findVariable(sourceMmd, "matchup.sensor_list").read();
            final int matchupCount = getMatchupCount(sensorPatterns);
            final String sensorMmdLocation = NwpUtil.createTempFile("mmd", ".nc", true).getPath();
            final NetcdfFileWriter targetMmd = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3,
                                                                          sensorMmdLocation);

            final int ny = nyDimension.getLength();
            final int nx = nxDimension.getLength();

            targetMmd.addDimension(null, matchupDimension.getShortName(), matchupCount);
            targetMmd.addDimension(null, nyDimension.getShortName(), ny);
            targetMmd.addDimension(null, nxDimension.getShortName(), nx);

            NwpUtil.addVariable(targetMmd, NwpUtil.findVariable(sourceMmd, "matchup.id"));
            NwpUtil.addVariable(targetMmd, NwpUtil.findVariable(sourceMmd, sensorName + ".latitude",
                                                                getAlternativeSensorName(sensorName) + ".latitude"));
            NwpUtil.addVariable(targetMmd, NwpUtil.findVariable(sourceMmd, sensorName + ".longitude",
                                                                getAlternativeSensorName(sensorName) + ".longitude"));
            NwpUtil.addVariable(targetMmd, NwpUtil.findVariable(sourceMmd, sensorName + ".time",
                                                                getAlternativeSensorName(sensorName) + ".time"));

            targetMmd.create();

            try {
                for (final Variable sourceVariable : sourceMmd.getVariables()) {
                    final int[] sourceStart = new int[sourceVariable.getRank()];
                    final int[] sourceShape = sourceVariable.getShape();
                    final int[] targetStart = new int[sourceVariable.getRank()];
                    final Variable targetVariable = targetMmd.findVariable(sourceVariable.getFullNameEscaped());
                    if (targetVariable != null) {
                        for (int m = 0, n = 0; m < matchupDimension.getLength(); m++) {
                            if ((sensorPatterns.getInt(m) & sensorPattern) == sensorPattern) {
                                sourceStart[0] = m;
                                sourceShape[0] = 1;
                                targetStart[0] = n;
                                final Array data = sourceVariable.read(sourceStart, sourceShape);
                                targetMmd.write(targetVariable, targetStart, data);
                                n++;
                            }
                        }
                    }
                }
            } catch (InvalidRangeException e) {
                throw new IOException(e);
            } finally {
                try {
                    targetMmd.close();
                } catch (IOException ignored) {
                }
            }
            return targetMmd.getNetcdfFile().getLocation();
        } finally {
            try {
                sourceMmd.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Writes the match-up geo-coordinates from an MMD file to a SCRIP compatible file.
     *
     * @param mmd The MMD file.
     *
     * @throws java.io.IOException when an error occurred.
     */
    @SuppressWarnings({"ConstantConditions"})
    private void writeMatchupGeoFile(NetcdfFile mmd) throws IOException {
        final Dimension matchupDimension = NwpUtil.findDimension(mmd, "matchup");

        final String location = NwpUtil.createTempFile("geo", ".nc", true).getPath();
        final NetcdfFileWriter geoFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, location);

        final int matchupCount = matchupDimension.getLength();

        geoFile.addDimension(null, "grid_size", matchupCount);
        geoFile.addDimension(null, "grid_matchup", matchupCount);
        geoFile.addDimension(null, "grid_ny", 1);
        geoFile.addDimension(null, "grid_nx", 1);
        geoFile.addDimension(null, "grid_corners", 4);
        geoFile.addDimension(null, "grid_rank", 2);

        final Variable gridDims = geoFile.addVariable(null, "grid_dims", DataType.INT, "grid_rank");
        final Variable gridCenterLat = geoFile.addVariable(null, "grid_center_lat", DataType.FLOAT, "grid_size");
        gridCenterLat.addAttribute(new Attribute("units", "degrees"));
        final Variable gridCenterLon = geoFile.addVariable(null, "grid_center_lon", DataType.FLOAT, "grid_size");
        gridCenterLon.addAttribute(new Attribute("units", "degrees"));
        final Variable gridMask = geoFile.addVariable(null, "grid_imask", DataType.INT, "grid_size");
        geoFile.addVariable(null, "grid_corner_lat", DataType.FLOAT, "grid_size grid_corners");
        geoFile.addVariable(null, "grid_corner_lon", DataType.FLOAT, "grid_size grid_corners");

        geoFile.addGroupAttribute(null, new Attribute("title", "MMD geo-location in SCRIP format"));

        geoFile.create();

        try {
            geoFile.write(gridDims, Array.factory(new int[]{1, matchupCount}));

            final int[] sourceStart = {0};
            final int[] sourceShape = {1};
            final int[] sourceStride = {1};
            final int[] targetStart = {0};
            final int[] targetShape = {1};
            final Array maskData = Array.factory(DataType.INT, targetShape);

            final Variable sourceLat = NwpUtil.findVariable(mmd, "matchup.latitude");
            final Variable sourceLon = NwpUtil.findVariable(mmd, "matchup.longitude");

            for (int i = 0; i < matchupCount; i++) {
                sourceStart[0] = i;
                targetStart[0] = i;
                final Section sourceSection = new Section(sourceStart, sourceShape, sourceStride);
                final Array latData = sourceLat.read(sourceSection);
                final Array lonData = sourceLon.read(sourceSection);
                for (int k = 0; k < targetShape[0]; k++) {
                    final float lat = latData.getFloat(k);
                    final float lon = lonData.getFloat(k);
                    maskData.setInt(k, lat >= -90.0f && lat <= 90.0f && lon >= -180.0f && lat <= 180.0f ? 1 : 0);
                }
                geoFile.write(gridCenterLat, targetStart, latData.reshape(targetShape));
                geoFile.write(gridCenterLon, targetStart, lonData.reshape(targetShape));
                geoFile.write(gridMask, targetStart, maskData);
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            try {
                geoFile.close();
            } catch (IOException ignored) {
            }
        }
        geoFileLocation = geoFile.getNetcdfFile().getLocation();
    }

    /**
     * Writes the sensor geo-coordinates from an MMD file to a SCRIP compatible file.
     *
     * @param mmd     The MMD file.
     * @param gx      The the number of tie points in x direction.
     * @param gy      The the number of tie points in y direction.
     * @param strideX The tie point stride in x direction.
     * @param strideY The tie point stride in y direction.
     *
     * @throws java.io.IOException when an error occurred.
     */
    @SuppressWarnings({"ConstantConditions"})
    private void writeSensorGeoFile(NetcdfFile mmd, int gx, int gy, int strideX, int strideY) throws IOException {
        final Dimension matchupDimension = NwpUtil.findDimension(mmd, "matchup");
        final String sensorBasename = sensorName.replaceAll("\\..+", "");
        final Dimension nyDimension = NwpUtil.findDimension(mmd, sensorBasename + ".ny");
        final Dimension nxDimension = NwpUtil.findDimension(mmd, sensorBasename + ".nx");

        final String location = NwpUtil.createTempFile("geo", ".nc", true).getPath();
        final NetcdfFileWriter geoFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, location);

        final int matchupCount = matchupDimension.getLength();
        final int ny = nyDimension.getLength();
        final int nx = nxDimension.getLength();

        geoFile.addDimension(null, "grid_size", matchupCount * gy * gx);
        geoFile.addDimension(null, "grid_matchup", matchupCount);
        geoFile.addDimension(null, "grid_ny", gy);
        geoFile.addDimension(null, "grid_nx", gx);
        geoFile.addDimension(null, "grid_corners", 4);
        geoFile.addDimension(null, "grid_rank", 2);

        final Variable gridDims = geoFile.addVariable(null, "grid_dims", DataType.INT, "grid_rank");
        final Variable gridCenterLat = geoFile.addVariable(null, "grid_center_lat", DataType.FLOAT, "grid_size");
        gridCenterLat.addAttribute(new Attribute("units", "degrees"));
        final Variable gridCenterLon = geoFile.addVariable(null, "grid_center_lon", DataType.FLOAT, "grid_size");
        gridCenterLon.addAttribute(new Attribute("units", "degrees"));
        final Variable gridMask = geoFile.addVariable(null, "grid_imask", DataType.INT, "grid_size");
        geoFile.addVariable(null, "grid_corner_lat", DataType.FLOAT, "grid_size grid_corners");
        geoFile.addVariable(null, "grid_corner_lon", DataType.FLOAT, "grid_size grid_corners");

        geoFile.addGroupAttribute(null, new Attribute("title", "MMD geo-location in SCRIP format"));

        geoFile.create();

        try {
            geoFile.write(gridDims, Array.factory(new int[]{gx, gy * matchupCount}));

            final int[] sourceStart = {0, (ny >> 1) - (gy >> 1) * strideY, (nx >> 1) - (gx >> 1) * strideX};
            final int[] sourceShape = {1, gy * strideY, gx * strideX};
            final int[] sourceStride = {1, strideY, strideX};
            final int[] targetStart = {0};
            final int[] targetShape = {gy * gx};
            final Array maskData = Array.factory(DataType.INT, targetShape);

            final Variable sourceLat = NwpUtil.findVariable(mmd, sensorName + ".latitude",
                                                            getAlternativeSensorName(sensorName) + ".latitude");
            final Variable sourceLon = NwpUtil.findVariable(mmd, sensorName + ".longitude",
                                                            getAlternativeSensorName(sensorName) + ".longitude");

            for (int i = 0; i < matchupCount; i++) {
                sourceStart[0] = i;
                targetStart[0] = i * gy * gx;
                final Section sourceSection = new Section(sourceStart, sourceShape, sourceStride);
                final Array latData = sourceLat.read(sourceSection);
                final Array lonData = sourceLon.read(sourceSection);
                for (int k = 0; k < targetShape[0]; k++) {
                    final float lat = latData.getFloat(k);
                    final float lon = lonData.getFloat(k);
                    maskData.setInt(k, lat >= -90.0f && lat <= 90.0f && lon >= -180.0f && lat <= 180.0f ? 1 : 0);
                }
                geoFile.write(gridCenterLat, targetStart, latData.reshape(targetShape));
                geoFile.write(gridCenterLon, targetStart, lonData.reshape(targetShape));
                geoFile.write(gridMask, targetStart, maskData);
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            try {
                geoFile.close();
            } catch (IOException ignored) {
            }
        }
        geoFileLocation = geoFile.getNetcdfFile().getLocation();
    }

    static String getAlternativeSensorName(String sensorName) {
        if (!sensorName.startsWith("avhrr.")) {
            return sensorName;
        }
        final int index = sensorName.indexOf(".");
        return sensorName.substring(0, index) + "." + sensorName.substring(index + 2);
    }

    private int getMatchupCount(Array sensorPatterns) {
        int matchupCount = 0;
        for (int i = 0; i < sensorPatterns.getSize(); ++i) {
            if ((sensorPatterns.getInt(i) & sensorPattern) == sensorPattern) {
                matchupCount++;
            }
        }
        return matchupCount;
    }

}
