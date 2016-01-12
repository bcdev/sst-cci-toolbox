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
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.ProcessRunner;
import org.esa.cci.sst.util.SensorNames;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * NWP extraction tool.
 *
 * @author Ralf Quast
 */
class NwpTool extends BasicTool {

    private static final String CDO_NWP_TEMPLATE =
            "#! /bin/sh\n" +
                    "${CDO} ${CDO_OPTS} -f nc2 mergetime ${GGAS_TIMESTEPS} ${GGAS_TIME_SERIES} && " +
                    "${CDO} ${CDO_OPTS} -f grb mergetime ${GGAM_TIMESTEPS} ${GGAM_TIME_SERIES} && " +
                    "${CDO} ${CDO_OPTS} -f grb mergetime ${SPAM_TIMESTEPS} ${SPAM_TIME_SERIES} && " +
                    "${CDO} ${CDO_OPTS} -f nc2 mergetime ${GAFS_TIMESTEPS} ${GAFS_TIME_SERIES} && " +
                    // attention: chaining the operations below results in a loss of the y dimension in the result file
                    "${CDO} ${CDO_OPTS} -f nc2 -R -t ecmwf setreftime,${REFTIME} -remapbil,${GEO} -selname,Q,O3,CLWC,CIWC ${GGAM_TIME_SERIES} ${GGAM_TIME_SERIES_REMAPPED} && " +
                    "${CDO} ${CDO_OPTS} -f nc2 -R -t ecmwf setreftime,${REFTIME} -remapbil,${GEO} -sp2gp -selname,LNSP,T ${SPAM_TIME_SERIES} ${SPAM_TIME_SERIES_REMAPPED} && " +
                    "${CDO} ${CDO_OPTS} -f nc2 -R -t ecmwf setreftime,${REFTIME} -remapbil,${GEO} -selname,TP -selhour,0,6,12,18 ${GAFS_TIME_SERIES} ${GAFS_TIME_SERIES_REMAPPED} && " +
                    "${CDO} ${CDO_OPTS} -f nc2 merge -setreftime,${REFTIME} -remapbil,${GEO} -selname,CI,ASN,SSTK,TCWV,MSL,TCC,U10,V10,T2,D2,AL,SKT ${GGAS_TIME_SERIES} ${GGAM_TIME_SERIES_REMAPPED} ${SPAM_TIME_SERIES_REMAPPED} ${GAFS_TIME_SERIES_REMAPPED} ${NWP_TIME_SERIES}\n";

    private static final String CDO_MATCHUP_AN_TEMPLATE =
            "#! /bin/sh\n" +
                    "${CDO} ${CDO_OPTS} -f nc2 mergetime ${GGAS_TIMESTEPS} ${GGAS_TIME_SERIES} && " +
                    "${CDO} ${CDO_OPTS} -f nc2 setreftime,${REFTIME} -remapbil,${GEO} -selname,CI,SSTK,U10,V10 ${GGAS_TIME_SERIES} ${AN_TIME_SERIES}\n";

    private static final String CDO_MATCHUP_FC_TEMPLATE =
            "#! /bin/sh\n" +
                    "${CDO} ${CDO_OPTS} -f nc2 mergetime ${GAFS_TIMESTEPS} ${GAFS_TIME_SERIES} && " +
                    "${CDO} ${CDO_OPTS} -f nc2 mergetime ${GGFS_TIMESTEPS} ${GGFS_TIME_SERIES} && " +
                    // attention: chaining the operations below results in a loss of the y dimension in the result file
                    "${CDO} ${CDO_OPTS} -f nc2 setreftime,${REFTIME} -remapbil,${GEO} -selname,SSTK,MSL,BLH,U10,V10,T2,D2 ${GGFS_TIME_SERIES} ${GGFS_TIME_SERIES_REMAPPED} && " +
                    "${CDO} ${CDO_OPTS} -f nc2 merge -setreftime,${REFTIME} -remapbil,${GEO} -selname,SSHF,SLHF,SSRD,STRD,SSR,STR,EWSS,NSSS,E,TP ${GAFS_TIME_SERIES} ${GGFS_TIME_SERIES_REMAPPED} ${FC_TIME_SERIES}\n";

    private String cdoHome;
    private String sourceMmdLocation;
    private String sourceNwpLocation;
    private String targetNwpLocation;
    private String dimensionFilePath;
    private String sensorName;
    private int sensorPattern;
    private boolean deleteOnExit;
    private boolean forSensor;

    NwpTool() {
        super("nwp-tool", "1.0");
    }

    public static void main(String[] args) {
        final NwpTool tool = new NwpTool();
        try {
            if (!tool.setCommandLineArgs(args)) {
                tool.printHelp();
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
        // no database functions needed, therefore don't call
        // super.initialize();

        final Configuration config = getConfig();

        deleteOnExit = config.getBooleanValue(Configuration.KEY_MMS_IO_TMPDELETEONEXIT, true);
        cdoHome = config.getStringValue(Configuration.KEY_MMS_NWP_CDO_HOME);
        dimensionFilePath = config.getStringValue(Configuration.KEY_MMS_MMD_DIMENSIONS);
        sourceMmdLocation = config.getStringValue(Configuration.KEY_MMS_NWP_MMD_SOURCE);
        sourceNwpLocation = config.getStringValue(Configuration.KEY_MMS_NWP_NWP_SOURCE);
        targetNwpLocation = config.getStringValue(Configuration.KEY_MMS_NWP_NWP_TARGET);

        forSensor = config.getBooleanValue(Configuration.KEY_MMS_NWP_FOR_SENSOR);
        if (forSensor) {
            sensorName = SensorNames.getStandardName(config.getStringValue(Configuration.KEY_MMS_NWP_SENSOR));
            sensorPattern = (int) config.getPattern(sensorName);
        }
    }

    private void run() throws IOException, InterruptedException {
        final boolean exists = new File(sourceMmdLocation).exists();
        if (!exists) {
            logger.warning(MessageFormat.format("missing source file: {0}", sourceMmdLocation));
            logger.warning(MessageFormat.format("skipping target file: {0}", targetNwpLocation));
            return;
        }

        logger.info(MessageFormat.format("loading dimensions from file: {0}", dimensionFilePath));
        final Properties dimensions = new Properties();
        dimensions.load(new BufferedReader(new FileReader(dimensionFilePath)));
        logger.info(MessageFormat.format("completed loading dimensions from file: {0}", dimensionFilePath));

        if (forSensor) {
            logger.info(MessageFormat.format("extracting matchups from source file: {0}", sourceMmdLocation));
            final String sensorMmdLocation = writeSingleSensorMmdFile(sourceMmdLocation, sensorName, sensorPattern,
                    deleteOnExit);
            logger.info(MessageFormat.format("completed extracting matchups from source file: {0}", sourceMmdLocation));

            if (sensorMmdLocation == null) {
                logger.warning(MessageFormat.format("no records with pattern {0} found in source file: {1}",
                        sensorPattern, sourceMmdLocation));
                logger.warning(MessageFormat.format("skipping target file: {0}", targetNwpLocation));
                return;
            }


            logger.info(MessageFormat.format("extracting NWP data for source file: {0}", sensorMmdLocation));
            writeSensorNwpFile(sensorMmdLocation, dimensions);
            logger.info(MessageFormat.format("completed extracting NWP data for source file: {0}", sensorMmdLocation));
        } else {
            final int analysisTimeStepCount = Integer.parseInt(dimensions.getProperty("matchup.nwp.an.time"));
            final int forecastTimeStepCount = Integer.parseInt(dimensions.getProperty("matchup.nwp.fc.time"));

            logger.info(
                    MessageFormat.format("extracting NWP analysis data for source file: {0}", sourceMmdLocation));
            final String analysisFileLocation = createAnalysisFile(sourceMmdLocation);
            logger.info(
                    MessageFormat.format("completed extracting NWP analysis data for source file: {0}",
                            sourceMmdLocation));

            logger.info(
                    MessageFormat.format("extracting NWP forecast data for source file: {0}", sourceMmdLocation));
            final String forecastFileLocation = createForecastFile(sourceMmdLocation);
            logger.info(
                    MessageFormat.format("completed extracting NWP forecast data for source file: {0}",
                            sourceMmdLocation));

            logger.info(MessageFormat.format("writing matchup NWP data for source file: {0}", sourceMmdLocation));
            writeMatchupNwpFile(sourceMmdLocation, forecastFileLocation, analysisFileLocation,
                    targetNwpLocation, forecastTimeStepCount, analysisTimeStepCount);
            logger.info(MessageFormat.format("completed writing matchup NWP data for source file: {0}", sourceMmdLocation));
        }
    }

    String createAnalysisFile(String mmdFileLocation) throws IOException, InterruptedException {
        final NetcdfFile mmdFile = NetcdfFile.open(mmdFileLocation);
        final Variable timeVariable = NwpUtil.findVariable(mmdFile, "matchup.time");
        final List<String> subDirectories = NwpUtil.getRelevantNwpDirs(timeVariable, logger);

        try {
            final String geoFileLocation = writeMatchupGeoFile(mmdFile, deleteOnExit);

            final Properties properties = new Properties();
            properties.setProperty("CDO", cdoHome + "/bin/cdo");
            properties.setProperty("CDO_OPTS", "-M");
            properties.setProperty("REFTIME", "1978-01-01,00:00:00,seconds");

            properties.setProperty("GEO", geoFileLocation);
            properties.setProperty("GGAS_TIMESTEPS",
                    NwpUtil.composeFilesString(sourceNwpLocation + "/ggas", subDirectories,
                            "ggas[0-9]*.nc", 0));
            properties.setProperty("GGAS_TIME_SERIES", NwpUtil.createTempFile("ggas", ".nc", deleteOnExit).getPath());
            final String analysisFileLocation = NwpUtil.createTempFile("analysis", ".nc", deleteOnExit).getPath();
            properties.setProperty("AN_TIME_SERIES", analysisFileLocation);

            final ProcessRunner runner = new ProcessRunner();
            final String resolvedTemplate = ProcessRunner.resolveTemplate(CDO_MATCHUP_AN_TEMPLATE, properties);
            runner.execute(ProcessRunner.writeExecutableScript(resolvedTemplate, "cdo", ".sh", deleteOnExit).getPath());

            return analysisFileLocation;
        } finally {
            try {
                mmdFile.close();
            } catch (IOException ignored) {
            }
        }
    }

    String createForecastFile(String mmdFileLocation) throws IOException, InterruptedException {
        final NetcdfFile mmdFile = NetcdfFile.open(mmdFileLocation);
        final Variable timeVariable = NwpUtil.findVariable(mmdFile, "matchup.time");
        final List<String> subDirectories = NwpUtil.getRelevantNwpDirs(timeVariable, logger);

        try {
            final String geoFileLocation = writeMatchupGeoFile(mmdFile, true);

            final Properties properties = new Properties();
            properties.setProperty("CDO", "cdo");
            properties.setProperty("CDO_OPTS", "-M");
            properties.setProperty("REFTIME", "1978-01-01,00:00:00,seconds");

            properties.setProperty("GEO", geoFileLocation);
            properties.setProperty("GAFS_TIMESTEPS",
                    NwpUtil.composeFilesString(sourceNwpLocation + "/gafs", subDirectories,
                            "gafs[0-9]*.nc", 0));
            properties.setProperty("GGFS_TIMESTEPS",
                    NwpUtil.composeFilesString(sourceNwpLocation + "/ggfs", subDirectories,
                            "ggfs[0-9]*.nc", 0));
            properties.setProperty("GAFS_TIME_SERIES", NwpUtil.createTempFile("gafs", ".nc", deleteOnExit).getPath());
            properties.setProperty("GGFS_TIME_SERIES", NwpUtil.createTempFile("ggfs", ".nc", deleteOnExit).getPath());
            properties.setProperty("GGFS_TIME_SERIES_REMAPPED",
                    NwpUtil.createTempFile("ggfr", ".nc", deleteOnExit).getPath());
            final String forecastFileLocation = NwpUtil.createTempFile("forecast", ".nc", deleteOnExit).getPath();
            properties.setProperty("FC_TIME_SERIES", forecastFileLocation);

            final ProcessRunner runner = new ProcessRunner();
            final String resolvedTemplate = ProcessRunner.resolveTemplate(CDO_MATCHUP_FC_TEMPLATE, properties);
            runner.execute(ProcessRunner.writeExecutableScript(resolvedTemplate, "cdo", ".sh", deleteOnExit).getPath());

            return forecastFileLocation;
        } finally {
            try {
                mmdFile.close();
            } catch (IOException ignored) {
            }
        }
    }

    void writeSensorNwpFile(String mmdFileLocation, Properties dimensions) throws IOException, InterruptedException {
        final NetcdfFile mmdFile = NetcdfFile.open(mmdFileLocation);
        try {
            final Variable timeVariable = NwpUtil.findVariable(mmdFile, sensorName + ".time");
            logger.info("Looking for relevant NWP sub-directories...");
            final List<String> subDirectories = NwpUtil.getRelevantNwpDirs(timeVariable, logger);
            logger.info("Found NWP sub-directories: " + Arrays.toString(subDirectories.toArray(new String[subDirectories.size()])));
            final String sensorBasename = getSensorBasename(sensorName);
            final int nx = Integer.parseInt(dimensions.getProperty(sensorBasename + ".nx"));
            final int ny = Integer.parseInt(dimensions.getProperty(sensorBasename + ".ny"));
            final int nwpNx = Integer.parseInt(dimensions.getProperty(sensorBasename + ".nwp.nx"));
            final int nwpNy = Integer.parseInt(dimensions.getProperty(sensorBasename + ".nwp.ny"));

            final int strideX = calculateStride(nx, nwpNx);
            final int strideY = calculateStride(ny, nwpNy);

            final String geoFileLocation = writeSensorGeoFile(mmdFile, nwpNx, nwpNy, strideX, strideY, sensorName, deleteOnExit);

            final Properties properties = new Properties();
            properties.setProperty("CDO", "cdo");
            properties.setProperty("CDO_OPTS", "-M");
            properties.setProperty("REFTIME", "1978-01-01,00:00:00,seconds");

            properties.setProperty("GEO", geoFileLocation);
            properties.setProperty("GGAS_TIMESTEPS",
                    NwpUtil.composeFilesString(sourceNwpLocation + "/ggas", subDirectories,
                            "ggas[0-9]*.nc", 1));
            properties.setProperty("GGAM_TIMESTEPS",
                    NwpUtil.composeFilesString(sourceNwpLocation + "/ggam", subDirectories,
                            "ggam[0-9]*.grb", 1));
            properties.setProperty("SPAM_TIMESTEPS",
                    NwpUtil.composeFilesString(sourceNwpLocation + "/spam", subDirectories,
                            "spam[0-9]*.grb", 1));
            properties.setProperty("GAFS_TIMESTEPS",
                    NwpUtil.composeFilesString(sourceNwpLocation + "/gafs", subDirectories,
                            "gafs[0-9]*[62].nc", -1));
            properties.setProperty("GGAS_TIME_SERIES", NwpUtil.createTempFile("ggas", ".nc", deleteOnExit).getPath());
            properties.setProperty("GGAM_TIME_SERIES", NwpUtil.createTempFile("ggam", ".grb", deleteOnExit).getPath());
            properties.setProperty("SPAM_TIME_SERIES", NwpUtil.createTempFile("spam", ".grb", deleteOnExit).getPath());
            properties.setProperty("GAFS_TIME_SERIES", NwpUtil.createTempFile("gafs", ".nc", deleteOnExit).getPath());
            properties.setProperty("GGAM_TIME_SERIES_REMAPPED",
                    NwpUtil.createTempFile("ggar", ".nc", deleteOnExit).getPath());
            properties.setProperty("SPAM_TIME_SERIES_REMAPPED",
                    NwpUtil.createTempFile("spar", ".nc", deleteOnExit).getPath());
            properties.setProperty("GAFS_TIME_SERIES_REMAPPED",
                    NwpUtil.createTempFile("gafr", ".nc", deleteOnExit).getPath());
            properties.setProperty("NWP_TIME_SERIES", NwpUtil.createTempFile("nwp", ".nc", deleteOnExit).getPath());

            final ProcessRunner runner = new ProcessRunner();
            final String resolvedTemplate = ProcessRunner.resolveTemplate(CDO_NWP_TEMPLATE, properties);
            final String path = ProcessRunner.writeExecutableScript(resolvedTemplate, "cdo", ".sh", deleteOnExit).getPath();
            runner.execute(path);

            final NetcdfFile nwpFile = NetcdfFile.open(properties.getProperty("NWP_TIME_SERIES"));
            try {
                logger.info(MessageFormat.format("Starting to write NWP MMD file: {0}", targetNwpLocation));
                merge(mmdFile, nwpFile, sensorName, targetNwpLocation);
                logger.info(MessageFormat.format("Finished writing NWP MMD file: {0}", targetNwpLocation));
            } finally {
                try {
                    nwpFile.close();
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


    static void writeMatchupNwpFile(String sourceMmdLocation,
                                            String forecastFileLocation,
                                            String analysisFileLocation,
                                            String targetMmdLocation,
                                            int targetFcTimeStepCount,
                                            int targetAnTimeStepCount) throws IOException {
        final NetcdfFileWriter targetMmd = createNew(targetMmdLocation);
        NetcdfFile sourceMmd = null;
        NetcdfFile forecastFile = null;
        NetcdfFile analysisFile = null;

        try {
            sourceMmd = NetcdfFile.open(sourceMmdLocation);
            forecastFile = NetcdfFile.open(forecastFileLocation);
            analysisFile = NetcdfFile.open(analysisFileLocation);

            final Dimension matchupDimension = NwpUtil.findDimension(sourceMmd, "matchup");
            final Dimension callsignDimension = NwpUtil.findDimension(sourceMmd, "callsign_length");
            final Dimension yDimension = NwpUtil.findDimension(forecastFile, "y");
            final Dimension xDimension = NwpUtil.findDimension(forecastFile, "x");

            final int matchupCount = matchupDimension.getLength();
            final int gy = yDimension.getLength() / matchupCount;
            final int gx = xDimension.getLength();

            targetMmd.addDimension(null, matchupDimension.getShortName(), matchupCount);
            targetMmd.addDimension(null, callsignDimension.getShortName(), callsignDimension.getLength());
            targetMmd.addDimension(null, "matchup.nwp.fc.time", targetFcTimeStepCount);
            targetMmd.addDimension(null, "matchup.nwp.an.time", targetAnTimeStepCount);
            targetMmd.addDimension(null, "matchup.nwp.ny", gy);
            targetMmd.addDimension(null, "matchup.nwp.nx", gx);

            addVariable(Constants.MATCHUP_ID, sourceMmd, targetMmd);
            addVariable(Constants.MATCHUP_TIME, sourceMmd, targetMmd);
            addVariable(Constants.MATCHUP_LONGITUDE, sourceMmd, targetMmd);
            addVariable(Constants.MATCHUP_LATITUDE, sourceMmd, targetMmd);
            addVariable(Constants.MATCHUP_INSITU_CALLSIGN, sourceMmd, targetMmd);
            addVariable(Constants.MATCHUP_INSITU_DATASET, sourceMmd, targetMmd);
            addVariable(Constants.MATCHUP_REFERENCE_FLAG, sourceMmd, targetMmd);
            addVariable(Constants.MATCHUP_DATASET_ID, sourceMmd, targetMmd);

            // create forecast variables
            final Map<Variable, Variable> fcMap = new HashMap<>();
            NwpUtil.copyVariables(forecastFile, targetMmd, fcMap, "fc");
            final Variable mmdTime = NwpUtil.findVariable(sourceMmd, "matchup.time");
            final Variable fcT0 = addCenterTimeVariable(targetMmd, mmdTime, "fc");

            // create analysis variables
            final Map<Variable, Variable> anMap = new HashMap<>();
            NwpUtil.copyVariables(analysisFile, targetMmd, anMap, "an");
            final Variable anT0 = addCenterTimeVariable(targetMmd, mmdTime, "an");

            targetMmd.create();

            copyVariableData(Constants.MATCHUP_ID, sourceMmd, targetMmd);
            copyVariableData(Constants.MATCHUP_TIME, sourceMmd, targetMmd);
            copyVariableData(Constants.MATCHUP_LONGITUDE, sourceMmd, targetMmd);
            copyVariableData(Constants.MATCHUP_LATITUDE, sourceMmd, targetMmd);
            copyVariableData(Constants.MATCHUP_INSITU_CALLSIGN, sourceMmd, targetMmd);
            copyVariableData(Constants.MATCHUP_INSITU_DATASET, sourceMmd, targetMmd);
            copyVariableData(Constants.MATCHUP_REFERENCE_FLAG, sourceMmd, targetMmd);
            copyVariableData(Constants.MATCHUP_DATASET_ID, sourceMmd, targetMmd);

            // write forecast data
            final Array fcTargetTimes = mmdTime.read();
            final Array fcSourceTimes = NwpUtil.findVariable(forecastFile, "time", "t").read();
            final int[] fcSourceShape = {targetFcTimeStepCount, 1, gy, gx};
            final int fcPastTimeStepCount = NwpTool.computePastTimeStepCount(targetFcTimeStepCount);
            final int fcFutureTimeStepCount = NwpTool.computeFutureTimeStepCount(targetFcTimeStepCount);
            final int[] centerTimes = new int[matchupCount];
            for (int i = 0; i < matchupCount; i++) {
                final int targetTime = fcTargetTimes.getInt(i);
                final int timeStep = NwpUtil.nearestTimeStep(fcSourceTimes, targetTime);
                if (timeStep - fcPastTimeStepCount < 0 || timeStep + fcFutureTimeStepCount > fcSourceTimes.getSize() - 1) {
                    throw new ToolException("Not enough time steps in NWP time series.", ToolException.TOOL_ERROR);
                }
                final int[] sourceStart = {timeStep - fcPastTimeStepCount, 0, i * gy, 0};
                NwpUtil.copyValues(fcMap, targetMmd, i, sourceStart, fcSourceShape);
                centerTimes[i] = fcSourceTimes.getInt(timeStep);
            }
            targetMmd.write(fcT0, Array.factory(centerTimes));

            // write analysis data
            final Array anTargetTimes = mmdTime.read();
            final Array anSourceTimes = NwpUtil.findVariable(analysisFile, "time", "t").read();
            final int[] anSourceShape = {targetAnTimeStepCount, 1, gy, gx};
            final int anPastTimeStepCount = NwpTool.computePastTimeStepCount(targetAnTimeStepCount);
            final int anFutureTimeStepCount = NwpTool.computeFutureTimeStepCount(targetAnTimeStepCount);
            for (int i = 0; i < matchupCount; i++) {
                final int targetTime = anTargetTimes.getInt(i);
                final int timeStep = NwpUtil.nearestTimeStep(anSourceTimes, targetTime);
                if (timeStep - anPastTimeStepCount < 0 || timeStep + anFutureTimeStepCount > anSourceTimes.getSize() - 1) {
                    throw new ToolException("Not enough time steps in NWP time series.", ToolException.TOOL_ERROR);
                }
                final int[] sourceStart = {timeStep - anPastTimeStepCount, 0, i * gy, 0};
                NwpUtil.copyValues(anMap, targetMmd, i, sourceStart, anSourceShape);
                centerTimes[i] = fcSourceTimes.getInt(timeStep);
            }
            targetMmd.write(anT0, Array.factory(centerTimes));
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            if (analysisFile != null) {
                try {
                    analysisFile.close();
                } catch (IOException ignored) {
                }
            }
            if (forecastFile != null) {
                try {
                    forecastFile.close();
                } catch (IOException ignored) {
                }
            }
            if (sourceMmd != null) {
                try {
                    sourceMmd.close();
                } catch (IOException ignored) {
                }
            }
            try {
                targetMmd.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static NetcdfFileWriter createNew(String path) throws IOException {
        final File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        return NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4_classic, path);
    }

    private static NetcdfFileWriter createNewLarge(String path) throws IOException {
        final File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        final NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, path);
        writer.setLargeFile(true);
        return writer;
    }

    private static void copyVariableData(String name, NetcdfFile sourceMmd, NetcdfFileWriter targetMmd) throws
            IOException,
            InvalidRangeException {
        final Array data = NwpUtil.findVariable(sourceMmd, name).read();
        targetMmd.write(targetMmd.findVariable(NetcdfFile.makeValidPathName(name)), data);
    }

    private static void addVariable(String name, NetcdfFile source, NetcdfFileWriter target) throws IOException {
        final Variable s = NwpUtil.findVariable(source, name);
        final Variable t = target.addVariable(null, s.getShortName(), s.getDataType(), s.getDimensionsString());
        for (final Attribute a : s.getAttributes()) {
            t.addAttribute(a);
        }
    }

    private static Variable addCenterTimeVariable(NetcdfFileWriter targetMmd, Variable mmdTime, String id) {
        final Variable anT0 = targetMmd.addVariable(null, "matchup.nwp." + id + ".t0",
                mmdTime.getDataType(),
                mmdTime.getDimensionsString());
        for (final Attribute a : mmdTime.getAttributes()) {
            anT0.addAttribute(a);
        }
        return anT0;
    }

    private static void merge(NetcdfFile sourceMmd, NetcdfFile sourceNwp, String sensorName,
                              String targetPath) throws IOException {
        final NetcdfFileWriter targetMmd = createNewLarge(targetPath);
        try {
            // copy MMD structure
            final Dimension sourceMatchupDimension = NwpUtil.findDimension(sourceMmd, "matchup");
            final int matchupCount = sourceMatchupDimension.getLength();
            targetMmd.addDimension(null,
                    sourceMatchupDimension.getShortName(),
                    matchupCount);
            final Variable sourceMatchupId = NwpUtil.findVariable(sourceMmd, "matchup.id");
            final Variable targetMatchupId = targetMmd.addVariable(null,
                    sourceMatchupId.getShortName(),
                    sourceMatchupId.getDataType(),
                    sourceMatchupId.getDimensionsString());

            // copy NWP structure
            final Dimension yDimension = NwpUtil.findDimension(sourceNwp, "y");
            final Dimension xDimension = NwpUtil.findDimension(sourceNwp, "x");
            final Dimension levDimension = NwpUtil.findDimension(sourceNwp, "lev");
            final int gy = yDimension.getLength() / matchupCount;
            final int gx = xDimension.getLength();
            final int gz = levDimension.getLength();

            final String sensorBasename = getSensorBasename(sensorName);
            targetMmd.addDimension(null, sensorBasename + ".nwp.nx", gx);
            targetMmd.addDimension(null, sensorBasename + ".nwp.ny", gy);
            targetMmd.addDimension(null, sensorBasename + ".nwp.nz", gz);

            final Map<Variable, Variable> map = new HashMap<>();
            for (final Variable s : sourceNwp.getVariables()) {
                if (s.getRank() == 4) {
                    final String dimensions;
                    if (s.getDimension(1).getLength() == 1) {
                        dimensions = String.format("matchup %s.nwp.ny %s.nwp.nx",
                                sensorBasename, sensorBasename);
                    } else {
                        dimensions = String.format("matchup %s.nwp.nz %s.nwp.ny %s.nwp.nx",
                                sensorBasename, sensorBasename, sensorBasename);
                    }
                    final String sourceName = s.getShortName();
                    final String targetName = sensorName + ".nwp." + sourceName;
                    final Variable t = targetMmd.addVariable(null, targetName, s.getDataType(), dimensions);
                    map.put(t, s);
                    for (final Attribute a : s.getAttributes()) {
                        t.addAttribute(a);
                    }
                }
            }

            targetMmd.create();

            // copy MMD matchup IDs
            targetMmd.write(targetMatchupId, NwpUtil.findVariable(sourceMmd, "matchup.id").read());

            //copy NWP data;
            final Variable targetTimesVariable = NwpUtil.findVariable(sourceMmd, sensorName + ".time");
            final Array sourceTimes = NwpUtil.findVariable(sourceNwp, "time", "t").read();
            final Array targetTimes = targetTimesVariable.read();
            final float targetTimeFillValue = NwpUtil.getAttribute(targetTimesVariable, "_FillValue",
                    Integer.MIN_VALUE);

            for (int i = 0; i < matchupCount; i++) {
                final int[] sourceStart = {0, 0, i * gy, 0};
                final int[] sourceShape = {1, 0, gy, gx};

                final int targetTime = targetTimes.getInt(i);
                if (targetTime == (int) targetTimeFillValue) {
                    continue;
                }

                final FracIndex fi = NwpUtil.interpolationIndex(sourceTimes, targetTime);

                for (final Map.Entry<Variable, Variable> entry : map.entrySet()) {
                    final Variable sourceVariable = entry.getValue();
                    final Variable targetVariable = entry.getKey();

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
                    targetMmd.write(targetVariable, targetStart, slice2.reshape(targetShape));
                }
            }
        } catch (IOException | InvalidRangeException e) {
            final String message = MessageFormat.format("Failed to write NWP MMD file: {0} ({1})", targetPath,
                    e.getMessage());
            throw new IOException(message, e);
        } finally {
            try {
                targetMmd.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    /**
     * Writes the latitude, longitude and time for those records in an MMD file that correspond
     * to a certain sensor to a new MMD file.
     *
     * @param sourceMmdLocation The location of the source (multi-sensor) MMD file.
     * @param sensorName        The sensor name.
     * @param sensorPattern     The sensor pattern.
     * @param deleteOnExit      A flag indicating whether temporary files shall be deleted on exit.
     * @return the location of the netCDF file written.
     * @throws java.io.IOException when an error occurred.
     */
    private static String writeSingleSensorMmdFile(String sourceMmdLocation, String sensorName,
                                                   int sensorPattern, boolean deleteOnExit) throws IOException {
        final NetcdfFile sourceMmd = NetcdfFile.open(sourceMmdLocation);
        try {
            final Dimension matchupDimension = NwpUtil.findDimension(sourceMmd, "matchup");
            final String sensorBasename = getSensorBasename(sensorName);
            final Dimension nyDimension = NwpUtil.findDimension(sourceMmd, sensorBasename + ".ny");
            final Dimension nxDimension = NwpUtil.findDimension(sourceMmd, sensorBasename + ".nx");

            final Array sensorPatterns = NwpUtil.findVariable(sourceMmd, Constants.MATCHUP_DATASET_ID).read();
            final int matchupCount = getMatchupCount(sensorPatterns, sensorPattern);
            if (matchupCount == 0) {
                return null;
            }
            final String sensorMmdLocation = NwpUtil.createTempFile("mmd", ".nc", deleteOnExit).getPath();
            final NetcdfFileWriter targetMmd = createNewLarge(sensorMmdLocation);

            final int ny = nyDimension.getLength();
            final int nx = nxDimension.getLength();

            targetMmd.addDimension(null, matchupDimension.getShortName(), matchupCount);
            targetMmd.addDimension(null, nyDimension.getShortName(), ny);
            targetMmd.addDimension(null, nxDimension.getShortName(), nx);

            NwpUtil.addVariable(targetMmd, NwpUtil.findVariable(sourceMmd, Constants.MATCHUP_ID));
            NwpUtil.addVariable(targetMmd, NwpUtil.findVariable(sourceMmd, Constants.MATCHUP_TIME));
            NwpUtil.addVariable(targetMmd, NwpUtil.findVariable(sourceMmd, Constants.MATCHUP_LATITUDE));
            NwpUtil.addVariable(targetMmd, NwpUtil.findVariable(sourceMmd, Constants.MATCHUP_LONGITUDE));
            NwpUtil.addVariable(targetMmd, NwpUtil.findVariable(sourceMmd, sensorName + ".latitude"));
            NwpUtil.addVariable(targetMmd, NwpUtil.findVariable(sourceMmd, sensorName + ".longitude"));
            NwpUtil.addVariable(targetMmd, NwpUtil.findVariable(sourceMmd, sensorName + ".time"));

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
     * @param mmd          The MMD file.
     * @param deleteOnExit True if intermediate files shall be deleted on exit.
     * @throws java.io.IOException when an error occurred.
     */
    private static String writeMatchupGeoFile(NetcdfFile mmd, boolean deleteOnExit) throws IOException {
        final Dimension matchupDimension = NwpUtil.findDimension(mmd, "matchup");

        final String location = NwpUtil.createTempFile("geo", ".nc", deleteOnExit).getPath();
        final NetcdfFileWriter geoFile = createNewLarge(location);

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

            final Variable sourceLat = NwpUtil.findVariable(mmd, Constants.MATCHUP_LATITUDE);
            final Variable sourceLon = NwpUtil.findVariable(mmd, Constants.MATCHUP_LONGITUDE);

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
        return geoFile.getNetcdfFile().getLocation();
    }

    /**
     * Writes the sensor geo-coordinates from an MMD file to a SCRIP compatible file.
     *
     * @param mmd          The MMD file.
     * @param gx           The the number of tie points in x direction.
     * @param gy           The the number of tie points in y direction.
     * @param strideX      The tie point stride in x direction.
     * @param strideY      The tie point stride in y direction.
     * @param sensorName   The sensor name.
     * @param deleteOnExit True if intermediate files shall be deleted on exit.
     * @throws java.io.IOException when an error occurred.
     */
    private static String writeSensorGeoFile(NetcdfFile mmd, int gx, int gy, int strideX, int strideY,
                                             String sensorName, boolean deleteOnExit) throws IOException {
        final Dimension matchupDimension = NwpUtil.findDimension(mmd, "matchup");
        final String sensorBasename = getSensorBasename(sensorName);
        final Dimension nyDimension = NwpUtil.findDimension(mmd, sensorBasename + ".ny");
        final Dimension nxDimension = NwpUtil.findDimension(mmd, sensorBasename + ".nx");

        final String location = NwpUtil.createTempFile("geo", ".nc", deleteOnExit).getPath();
        final NetcdfFileWriter geoFile = createNewLarge(location);

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

            final Variable sourceLat = NwpUtil.findVariable(mmd, sensorName + ".latitude");
            final Variable sourceLon = NwpUtil.findVariable(mmd, sensorName + ".longitude");

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
        return geoFile.getNetcdfFile().getLocation();
    }

    // package public for testing
    static int computeFutureTimeStepCount(int timeStepCount) {
        return ((timeStepCount - 1) / 8) * 3;
    }

    // package public for testing
    static int computePastTimeStepCount(int timeStepCount) {
        return ((timeStepCount - 1) / 8) * 5;
    }

    // package access for testing only tb 2015-12-08
    static String getSensorBasename(String sensorName) {
        return sensorName.replaceAll("\\..+", "");
    }

    // package access for testing only tb 2015-12-08
    static int getMatchupCount(Array sensorPatterns, int wantedPattern) {
        int matchupCount = 0;
        for (int i = 0; i < sensorPatterns.getSize(); ++i) {
            if ((sensorPatterns.getInt(i) & wantedPattern) == wantedPattern) {
                matchupCount++;
            }
        }
        return matchupCount;
    }

    // package access for testing only tb 2015-12-08
    static int calculateStride(int n, int nwpN) {
        int stride;
        if (nwpN > 1) {
            stride = (n - 1) / (nwpN - 1);
        } else {
            stride = 1;
        }
        return stride;
    }

}
