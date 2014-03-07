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
import org.esa.cci.sst.util.SensorNames;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
            // attention: chaining the operations below results in a loss of the y dimension in the result file
            "${CDO} ${CDO_OPTS} -f nc2 -R -t ecmwf setreftime,${REFTIME} -remapbil,${GEO} -selname,Q,O3 ${GGAM_TIME_SERIES} ${GGAM_TIME_SERIES_REMAPPED} && " +
            "${CDO} ${CDO_OPTS} -f nc2 -R -t ecmwf setreftime,${REFTIME} -remapbil,${GEO} -sp2gp -selname,LNSP,T ${SPAM_TIME_SERIES} ${SPAM_TIME_SERIES_REMAPPED} && " +
            "${CDO} ${CDO_OPTS} -f nc2 merge -setreftime,${REFTIME} -remapbil,${GEO} -selname,CI,ASN,SSTK,TCWV,MSL,TCC,U10,V10,T2,D2,AL,SKT ${GGAS_TIME_SERIES} ${GGAM_TIME_SERIES_REMAPPED} ${SPAM_TIME_SERIES_REMAPPED} ${NWP_TIME_SERIES}\n";

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

        anTargetLocation = config.getStringValue(Configuration.KEY_MMS_NWP_AN_TARGET_LOCATION);
        fcTargetLocation = config.getStringValue(Configuration.KEY_MMS_NWP_FC_TARGET_LOCATION);
        nwpTargetLocation = config.getStringValue(Configuration.KEY_MMS_NWP_TARGET_LOCATION);
        sensorName = SensorNames.ensureStandardName(config.getStringValue(Configuration.KEY_MMS_NWP_SENSOR));
        // TODO - check for AVHRRs, patterns of orbit files and sub-scene files are different (rq-20140304)
        sensorPattern = (int) config.getPattern(sensorName);
    }

    private void run() throws IOException, InterruptedException {
        final Properties dimensions = new Properties();
        dimensions.load(new BufferedReader(new FileReader(dimensionFilePath)));

        writeSensorNwpFile(dimensions);
        writeMatchupAnFile(dimensions);
        writeMatchupFcFile(dimensions);
    }

    void writeMatchupAnFile(Properties dimensions) throws IOException, InterruptedException {
        final NetcdfFile mmdFile = NetcdfFile.open(mmdSourceLocation);
        final List<String> subDirectories = NwpUtil.getRelevantNwpDirs(NwpUtil.findVariable(mmdFile, "matchup.time"));

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
                                   NwpUtil.composeFilesString(nwpSourceLocation + "/ggas", subDirectories,
                                                              "ggas[0-9]*.nc"));
            properties.setProperty("GGAS_TIME_SERIES", NwpUtil.createTempFile("ggas", ".nc", true).getPath());
            properties.setProperty("AN_TIME_SERIES", NwpUtil.createTempFile("analysis", ".nc", true).getPath());

            final ProcessRunner runner = new ProcessRunner("org.esa.cci.sst");
            runner.execute(ProcessRunner.writeExecutableScript(CDO_MATCHUP_AN_TEMPLATE, properties).getPath());

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

    void writeMatchupFcFile(Properties dimensions) throws IOException, InterruptedException {
        final NetcdfFile mmdFile = NetcdfFile.open(mmdSourceLocation);
        final List<String> subDirectories = NwpUtil.getRelevantNwpDirs(NwpUtil.findVariable(mmdFile, "matchup.time"));

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
                                   NwpUtil.composeFilesString(nwpSourceLocation + "/gafs", subDirectories,
                                                              "gafs[0-9]*.nc"));
            properties.setProperty("GGFS_TIMESTEPS",
                                   NwpUtil.composeFilesString(nwpSourceLocation + "/ggfs", subDirectories,
                                                              "ggfs[0-9]*.nc"));
            properties.setProperty("GAFS_TIME_SERIES", NwpUtil.createTempFile("gafs", ".nc", true).getPath());
            properties.setProperty("GGFS_TIME_SERIES", NwpUtil.createTempFile("ggfs", ".nc", true).getPath());
            properties.setProperty("GGFS_TIME_SERIES_REMAPPED", NwpUtil.createTempFile("ggfr", ".nc", true).getPath());
            properties.setProperty("FC_TIME_SERIES", NwpUtil.createTempFile("forecast", ".nc", true).getPath());

            final ProcessRunner runner = new ProcessRunner("org.esa.cci.sst");
            runner.execute(ProcessRunner.writeExecutableScript(CDO_MATCHUP_FC_TEMPLATE, properties).getPath());

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

    void writeSensorNwpFile(Properties dimensions) throws IOException, InterruptedException {
        final NetcdfFile sensorMmdFile = NetcdfFile.open(writeSensorMmdFile(sensorName, sensorPattern));
        final Variable timeVariable = NwpUtil.findVariable(sensorMmdFile, sensorName + ".time",
                                                           getAlternativeSensorName(sensorName) + ".time");
        final List<String> subDirectories = NwpUtil.getRelevantNwpDirs(timeVariable);
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
            getLogger().warning("sensor '" + sensorName + "' is neither ATSR nor AVHRR - interpolating a single pixel");
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
                                   NwpUtil.composeFilesString(nwpSourceLocation + "/ggas", subDirectories,
                                                              "ggas[0-9]*.nc"));
            properties.setProperty("GGAM_TIMESTEPS",
                                   NwpUtil.composeFilesString(nwpSourceLocation + "/ggam", subDirectories,
                                                              "ggam[0-9]*.grb"));
            properties.setProperty("SPAM_TIMESTEPS",
                                   NwpUtil.composeFilesString(nwpSourceLocation + "/spam", subDirectories,
                                                              "spam[0-9]*.grb"));
            properties.setProperty("GGAS_TIME_SERIES", NwpUtil.createTempFile("ggas", ".nc", true).getPath());
            properties.setProperty("GGAM_TIME_SERIES", NwpUtil.createTempFile("ggam", ".grb", true).getPath());
            properties.setProperty("SPAM_TIME_SERIES", NwpUtil.createTempFile("spam", ".grb", true).getPath());
            properties.setProperty("GGAM_TIME_SERIES_REMAPPED", NwpUtil.createTempFile("ggar", ".nc", true).getPath());
            properties.setProperty("SPAM_TIME_SERIES_REMAPPED", NwpUtil.createTempFile("spar", ".nc", true).getPath());
            properties.setProperty("NWP_TIME_SERIES", NwpUtil.createTempFile("nwp", ".nc", true).getPath());

            final ProcessRunner runner = new ProcessRunner("org.esa.cci.sst");
            final String path = ProcessRunner.writeExecutableScript(CDO_NWP_TEMPLATE, properties).getPath();
            runner.execute(path);

            final NetcdfFile nwpFile = NetcdfFile.open(properties.getProperty("NWP_TIME_SERIES"));
            try {
                getLogger().info(MessageFormat.format("Starting to write NWP MMD file: {0}", nwpTargetLocation));
                mergeAndWrite(sensorMmdFile, nwpFile);
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

    private void mergeAndWrite(NetcdfFile sourceMmd, NetcdfFile sourceNwp) throws IOException {
        final NetcdfFileWriter targetMmd = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4,
                                                                      nwpTargetLocation);

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

            final String sensorBasename = sensorName.replaceAll("\\..+", "");
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
            final Variable targetTimesVariable = NwpUtil.findVariable(sourceMmd,
                                                                      sensorName + ".time",
                                                                      getAlternativeSensorName(sensorName) + ".time");
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
            final String message = MessageFormat.format("Failed to write NWP MMD file: {0} ({1})", nwpTargetLocation,
                                                        e.getMessage());
            getLogger().warning(message);
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
     * @param sensorName    The sensor name.
     * @param sensorPattern The sensor pattern.
     *
     * @return the location of the netCDF file written.
     *
     * @throws java.io.IOException when an error occurred.
     */
    @SuppressWarnings({"ConstantConditions"})
    private String writeSensorMmdFile(String sensorName, int sensorPattern) throws IOException {
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
