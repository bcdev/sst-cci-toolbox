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
import org.esa.cci.sst.util.ProcessRunner;
import org.esa.cci.sst.util.TemplateResolver;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
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
import java.util.Properties;

/**
 * NWP extraction prototype.
 *
 * @author Ralf Quast
 */
public class NwpToolPrototype {

    private static final String CDO_AN_TEMPLATE =
            "#! /bin/sh\n" +
            "${CDO} ${CDO_OPTS} -f nc mergetime ${GGAS_TIMESTEPS} ${GGAS_TIME_SERIES} && " +
            "${CDO} ${CDO_OPTS} -f grb mergetime ${GGAM_TIMESTEPS} ${GGAM_TIME_SERIES} && " +
            "${CDO} ${CDO_OPTS} -f grb mergetime ${SPAM_TIMESTEPS} ${SPAM_TIME_SERIES} && " +
            // attention: chaining the operations below results in a loss of the y dimension in the result file
            "${CDO} ${CDO_OPTS} -f nc -R -t ecmwf setreftime,${REFTIME} -remapbil,${GEO} -selname,Q,O3 ${GGAM_TIME_SERIES} ${GGAM_TIME_SERIES_REMAPPED} && " +
            "${CDO} ${CDO_OPTS} -f nc -t ecmwf setreftime,${REFTIME} -remapbil,${GEO} -sp2gp -selname,LNSP,T ${SPAM_TIME_SERIES} ${SPAM_TIME_SERIES_REMAPPED} && " +
            "${CDO} ${CDO_OPTS} -f nc merge -setreftime,${REFTIME} -remapbil,${GEO} -selname,CI,ASN,SSTK,TCWV,MSL,TCC,U10,V10,T2,D2,AL,SKT ${GGAS_TIME_SERIES} ${GGAM_TIME_SERIES_REMAPPED} ${SPAM_TIME_SERIES_REMAPPED} ${AN_TIME_SERIES}";

    private static final String CDO_FC_TEMPLATE =
            "#! /bin/sh\n" +
            "${CDO} ${CDO_OPTS} -f nc mergetime ${GAFS_TIMESTEPS} ${GAFS_TIME_SERIES} && " +
            "${CDO} ${CDO_OPTS} -f nc mergetime ${GGFS_TIMESTEPS} ${GGFS_TIME_SERIES} && " +
            // attention: chaining the operations below results in a loss of the y dimension in the result file
            "${CDO} ${CDO_OPTS} -f nc setreftime,${REFTIME} -remapbil,${GEO} -selname,SSTK,MSL,BLH,U10,V10,T2,D2 ${GGFS_TIME_SERIES} ${GGFS_TIME_SERIES_REMAPPED} && " +
            "${CDO} ${CDO_OPTS} -f nc merge -setreftime,${REFTIME} -remapbil,${GEO} -selname,SSHF,SLHF,SSRD,STRD,SSR,STR,EWSS,NSSS,E,TP ${GAFS_TIME_SERIES} ${GGFS_TIME_SERIES_REMAPPED} ${FC_TIME_SERIES}";

    private static final String MMD_SOURCE_LOCATION = "mmd.nc";
    private static final String AMD_TARGET_LOCATION = "amd.nc";
    private static final String FMD_TARGET_LOCATION = "fmd.nc";
    private static final int AN_NX = 11 / 2;
    private static final int AN_NY = 11 / 2;
    private static final int AN_STRIDE_X = 2;
    private static final int AN_STRIDE_Y = 2;
    private static final int FC_NX = 1;
    private static final int FC_NY = 1;
    private static final int PAST_TIME_STEP_COUNT = 5;
    private static final int FUTURE_TIME_STEP_COUNT = 3;
    private static final String SENSOR_NAME = "metop";
    private static final int SENSOR_PATTERN = 2;

    @SuppressWarnings({"ConstantConditions"})
    public static void main(String[] args) throws IOException, InterruptedException {
        final NetcdfFile sensorMmdFile = NetcdfFile.open(writeSensorMmdFile(MMD_SOURCE_LOCATION));

        try {
            final String subsceneGeoFileLocation = writeGeoFile(sensorMmdFile, AN_NX, AN_NY, AN_STRIDE_X, AN_STRIDE_Y);

            final Properties properties = new Properties();
            properties.setProperty("CDO", "/usr/local/bin/cdo");
            properties.setProperty("CDO_OPTS", "-M");
            properties.setProperty("REFTIME", "1978-01-01,00:00:00,seconds");

            properties.setProperty("GEO", subsceneGeoFileLocation);
            properties.setProperty("GGAS_TIMESTEPS", files("testdata/nwp", "ggas[0-9]*.nc"));
            properties.setProperty("GGAM_TIMESTEPS", files("testdata/nwp", "ggam[0-9]*.grb"));
            properties.setProperty("SPAM_TIMESTEPS", files("testdata/nwp", "spam[0-9]*.grb"));
            properties.setProperty("GGAS_TIME_SERIES", createTempFile("ggas", ".nc", true).getPath());
            properties.setProperty("GGAM_TIME_SERIES", createTempFile("ggam", ".nc", true).getPath());
            properties.setProperty("SPAM_TIME_SERIES", createTempFile("spam", ".nc", true).getPath());
            properties.setProperty("GGAM_TIME_SERIES_REMAPPED", createTempFile("ggar", ".nc", true).getPath());
            properties.setProperty("SPAM_TIME_SERIES_REMAPPED", createTempFile("spar", ".nc", true).getPath());
            properties.setProperty("AN_TIME_SERIES", createTempFile("analysis", ".nc", true).getPath());

            final ProcessRunner runner = new ProcessRunner("org.esa.cci.sst");
            runner.execute(writeCdoScript(CDO_AN_TEMPLATE, properties).getPath());

            final String matchupGeoFileLocation = writeGeoFile(sensorMmdFile, FC_NX, FC_NY, 1, 1);
            properties.setProperty("GEO", matchupGeoFileLocation);
            properties.setProperty("GAFS_TIMESTEPS", files("testdata/nwp", "gafs[0-9]*.nc"));
            properties.setProperty("GGFS_TIMESTEPS", files("testdata/nwp", "ggfs[0-9]*.nc"));
            properties.setProperty("GAFS_TIME_SERIES", createTempFile("gafs", ".nc", true).getPath());
            properties.setProperty("GGFS_TIME_SERIES", createTempFile("ggfs", ".nc", true).getPath());
            properties.setProperty("GGFS_TIME_SERIES_REMAPPED", createTempFile("ggfr", ".nc", true).getPath());
            properties.setProperty("FC_TIME_SERIES", createTempFile("forecast", ".nc", true).getPath());

            runner.execute(writeCdoScript(CDO_FC_TEMPLATE, properties).getPath());

            final NetcdfFile anFile = NetcdfFile.open(properties.getProperty("AN_TIME_SERIES"));
            try {
                writeAnalysisMmdFile(sensorMmdFile, anFile);
            } finally {
                try {
                    anFile.close();
                } catch (IOException ignored) {
                }
            }
            final NetcdfFile fcFile = NetcdfFile.open(properties.getProperty("FC_TIME_SERIES"));
            try {
                writeForecastMmdFile(sensorMmdFile, fcFile, PAST_TIME_STEP_COUNT, FUTURE_TIME_STEP_COUNT);
            } finally {
                try {
                    anFile.close();
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

    private static void writeAnalysisMmdFile(NetcdfFile mmd, NetcdfFile analysisFile) throws IOException {
        final Dimension matchupDimension = findDimension(mmd, "matchup");
        final Dimension yDimension = findDimension(analysisFile, "y");
        final Dimension xDimension = findDimension(analysisFile, "x");

        final int matchupCount = matchupDimension.getLength();
        final int gy = yDimension.getLength() / matchupCount;
        final int gx = xDimension.getLength();

        final NetcdfFileWriteable amd = NetcdfFileWriteable.createNew(AMD_TARGET_LOCATION, true);
        amd.addDimension(matchupDimension.getName(), matchupCount);
        amd.addDimension("nwp.nz", findDimension(analysisFile, "lev").getLength());
        amd.addDimension("nwp.ny", gy);
        amd.addDimension("nwp.nx", gx);

        final Variable matchupId = findVariable(mmd, "matchup.id");
        amd.addVariable(matchupId.getName(), matchupId.getDataType(), matchupId.getDimensionsString());

        for (final Variable s : analysisFile.getVariables()) {
            if (s.getRank() == 4) {
                final Variable t;
                if (s.getDimension(1).getLength() == 1) {
                    t = amd.addVariable(s.getName(), s.getDataType(), "matchup nwp.ny nwp.nx");
                } else {
                    t = amd.addVariable(s.getName(), s.getDataType(), "matchup nwp.nz nwp.ny nwp.nx");
                }
                for (final Attribute a : s.getAttributes()) {
                    t.addAttribute(a);
                }
            }
        }

        amd.create();

        final Array matchupIds = findVariable(mmd, "matchup.id").read();
        final Array sourceTimes = findVariable(analysisFile, "time").read();
        final Array targetTimes = findVariable(mmd, SENSOR_NAME + ".time").read();

        try {
            amd.write(NetcdfFile.escapeName("matchup.id"), matchupIds);

            for (int i = 0; i < matchupCount; i++) {
                final int[] sourceStart = {0, 0, i * gy, 0};
                final int[] sourceShape = {1, 0, gy, gx};

                final int targetTime = targetTimes.getInt(i);
                final FracIndex fi = interpolationIndex(sourceTimes, targetTime);

                for (final Variable t : amd.getVariables()) {
                    if ("matchup.id".equals(t.getName())) {
                        continue;
                    }
                    final Variable s = findVariable(analysisFile, t.getName());
                    final float fillValue = getAttribute(s, "_FillValue", 2.0E+20F);
                    final float validMin = getAttribute(s, "valid_min", Float.NEGATIVE_INFINITY);
                    final float validMax = getAttribute(s, "valid_max", Float.POSITIVE_INFINITY);

                    sourceStart[0] = fi.i;
                    sourceShape[1] = s.getShape(1);

                    final Array slice1 = s.read(sourceStart, sourceShape);
                    sourceStart[0] = fi.i + 1;
                    final Array slice2 = s.read(sourceStart, sourceShape);
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
                            slice2.setDouble(k, fi.f * v1 + (1.0 - fi.f) * v2);
                        }
                    }

                    final int[] targetShape = t.getShape();
                    targetShape[0] = 1;
                    final int[] targetStart = new int[targetShape.length];
                    targetStart[0] = i;
                    amd.write(t.getNameEscaped(), targetStart, slice2.reshape(targetShape));
                }
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            try {
                amd.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static float getAttribute(Variable s, String name, float defaultValue) {
        final Attribute a = s.findAttribute(name);
        if (a == null) {
            return defaultValue;
        }
        return a.getNumericValue().floatValue();
    }

    private static FracIndex interpolationIndex(Array sourceTimes, int targetTime) {
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
        throw new ToolException("Not enough time steps in NWP time series.", ToolException.TOOL_ERROR);
    }

    private static void writeForecastMmdFile(NetcdfFile mmd, NetcdfFile forecastFile,
                                             int pastTimeStepCount, int futureTimeStepCount) throws IOException {
        final Dimension matchupDimension = findDimension(mmd, "matchup");
        final Dimension yDimension = findDimension(forecastFile, "y");
        final Dimension xDimension = findDimension(forecastFile, "x");

        final int matchupCount = matchupDimension.getLength();
        final int gy = yDimension.getLength() / matchupCount;
        final int gx = xDimension.getLength();

        final NetcdfFileWriteable fmd = NetcdfFileWriteable.createNew(FMD_TARGET_LOCATION, true);
        fmd.addDimension(matchupDimension.getName(), matchupCount);

        final int timeStepCount = pastTimeStepCount + futureTimeStepCount + 1;
        fmd.addDimension("nwp.time", timeStepCount);
        fmd.addDimension("nwp.ny", gy);
        fmd.addDimension("nwp.nx", gx);

        final Variable matchupId = findVariable(mmd, "matchup.id");
        fmd.addVariable(matchupId.getName(), matchupId.getDataType(), matchupId.getDimensionsString());

        for (final Variable s : forecastFile.getVariables()) {
            if (s.getRank() == 4) {
                if (s.getDimension(1).getLength() == 1) {
                    final Variable t = fmd.addVariable(s.getName(), s.getDataType(),
                                                       "matchup nwp.time nwp.ny nwp.nx");
                    for (final Attribute a : s.getAttributes()) {
                        t.addAttribute(a);
                    }
                }
            }
        }

        fmd.create();

        final Array matchupIds = findVariable(mmd, "matchup.id").read();
        final Array targetTimes = findVariable(mmd, SENSOR_NAME + ".time").read();
        final Array sourceTimes = findVariable(forecastFile, "time").read();

        try {
            fmd.write(NetcdfFile.escapeName("matchup.id"), matchupIds);

            final int[] sourceShape = {timeStepCount, 1, gy, gx};
            for (int i = 0; i < matchupCount; i++) {
                final int targetTime = targetTimes.getInt(i);
                final int timeStep = nearestTimeStep(sourceTimes, targetTime);

                if (timeStep - pastTimeStepCount < 0 || timeStep + futureTimeStepCount > sourceTimes.getSize() - 1) {
                    throw new ToolException("Not enough time steps in NWP time series.", ToolException.TOOL_ERROR);
                }

                final int[] sourceStart = {timeStep - pastTimeStepCount, 0, i * gy, 0};

                for (final Variable t : fmd.getVariables()) {
                    if ("matchup.id".equals(t.getName())) {
                        continue;
                    }
                    final Variable s = findVariable(forecastFile, t.getName());
                    final Array sourceData = s.read(sourceStart, sourceShape);

                    final int[] targetShape = t.getShape();
                    targetShape[0] = 1;
                    final int[] targetStart = new int[targetShape.length];
                    targetStart[0] = i;
                    fmd.write(t.getNameEscaped(), targetStart, sourceData.reshape(targetShape));
                }
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            try {
                fmd.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static int nearestTimeStep(Array sourceTimes, int targetTime) {
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

    private static File createTempFile(String prefix, String suffix, boolean deleteOnExit) throws IOException {
        final File tempFile = File.createTempFile(prefix, suffix);
        if (deleteOnExit) {
            tempFile.deleteOnExit();
        }
        return tempFile;
    }

    private static String files(final String dirPath, final String pattern) {
        final File dir = new File(dirPath);
        final File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(pattern);
            }
        });
        final StringBuilder sb = new StringBuilder();
        for (final File file : files) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(file.getPath());
        }
        return sb.toString();
    }

    private static File writeCdoScript(String template, Properties properties) throws IOException {
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

    /**
     * Extracts the records from an MMD file that correspond to a certain sensor.
     *
     * @param mmdLocation The location of the MMD file.
     *
     * @return the location of the netCDF file written.
     *
     * @throws java.io.IOException when an error occurred.
     */
    @SuppressWarnings({"ConstantConditions"})
    private static String writeSensorMmdFile(String mmdLocation) throws IOException {
        final NetcdfFile mmd = NetcdfFile.open(mmdLocation);

        try {
            final Dimension matchupDimension = findDimension(mmd, "matchup");
            final Dimension nyDimension = findDimension(mmd, SENSOR_NAME + ".ny");
            final Dimension nxDimension = findDimension(mmd, SENSOR_NAME + ".nx");

            final Array sensorPatterns = findVariable(mmd, "matchup.sensor_list").read();
            final int matchupCount = matchupCount(sensorPatterns);
            final String sensorMmdLocation = createTempFile("mmd", ".nc", true).getPath();
            final NetcdfFileWriteable sensorMmd = NetcdfFileWriteable.createNew(sensorMmdLocation, true);

            final int ny = nyDimension.getLength();
            final int nx = nxDimension.getLength();

            sensorMmd.addDimension(matchupDimension.getName(), matchupCount);
            sensorMmd.addDimension(nyDimension.getName(), ny);
            sensorMmd.addDimension(nxDimension.getName(), nx);

            addVariable(sensorMmd, findVariable(mmd, "matchup.id"));
            addVariable(sensorMmd, findVariable(mmd, SENSOR_NAME + ".latitude"));
            addVariable(sensorMmd, findVariable(mmd, SENSOR_NAME + ".longitude"));
            addVariable(sensorMmd, findVariable(mmd, SENSOR_NAME + ".time"));

            sensorMmd.create();

            try {
                for (final Variable v : mmd.getVariables()) {
                    final int[] sourceStart = new int[v.getRank()];
                    final int[] sourceShape = v.getShape();
                    final int[] targetStart = new int[v.getRank()];
                    if (sensorMmd.findVariable(v.getNameEscaped()) != null) {
                        for (int m = 0, n = 0; m < matchupDimension.getLength(); m++) {
                            if ((sensorPatterns.getInt(m) & SENSOR_PATTERN) == SENSOR_PATTERN) {
                                sourceStart[0] = m;
                                sourceShape[0] = 1;
                                targetStart[0] = n;
                                final Array data = v.read(sourceStart, sourceShape);
                                sensorMmd.write(v.getNameEscaped(), targetStart, data);
                                n++;
                            }
                        }
                    }
                }
            } catch (InvalidRangeException e) {
                throw new IOException(e);
            } finally {
                try {
                    sensorMmd.close();
                } catch (IOException ignored) {
                }
            }
            return sensorMmd.getLocation();
        } finally {
            try {
                mmd.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void addVariable(NetcdfFileWriteable netcdfFile, Variable s) {
        final Variable t = netcdfFile.addVariable(s.getName(), s.getDataType(), s.getDimensionsString());
        for (final Attribute a : s.getAttributes()) {
            t.addAttribute(a);
        }
    }

    /**
     * Writes the geo-coordinates from the MMD file to a SCRIP compatible file.
     *
     * @param mmd     The MMD file.
     * @param gx      The the number of tie points in x direction.
     * @param gy      The the number of tie points in y direction.
     * @param strideX The tie point stride in x direction.
     * @param strideY The tie point stride in y direction.
     *
     * @return the location of netCDF file written.
     *
     * @throws java.io.IOException when an error occurred.
     */
    @SuppressWarnings({"ConstantConditions"})
    private static String writeGeoFile(NetcdfFile mmd, int gx, int gy, int strideX, int strideY) throws
                                                                                                 IOException {
        final Dimension matchupDimension = findDimension(mmd, "matchup");
        final Dimension nyDimension = findDimension(mmd, SENSOR_NAME + ".ny");
        final Dimension nxDimension = findDimension(mmd, SENSOR_NAME + ".nx");

        final String location = createTempFile("geo", ".nc", true).getPath();
        final NetcdfFileWriteable geoFile = NetcdfFileWriteable.createNew(location, true);

        final int matchupCount = matchupDimension.getLength();
        final int ny = nyDimension.getLength();
        final int nx = nxDimension.getLength();

        geoFile.addDimension("grid_size", matchupCount * gy * gx);
        geoFile.addDimension("grid_matchup", matchupCount);
        geoFile.addDimension("grid_ny", gy);
        geoFile.addDimension("grid_nx", gx);
        geoFile.addDimension("grid_corners", 4);
        geoFile.addDimension("grid_rank", 2);

        geoFile.addVariable("grid_dims", DataType.INT, "grid_rank");
        geoFile.addVariable("grid_center_lat", DataType.FLOAT, "grid_size").addAttribute(
                new Attribute("units", "degrees"));
        geoFile.addVariable("grid_center_lon", DataType.FLOAT, "grid_size").addAttribute(
                new Attribute("units", "degrees"));
        geoFile.addVariable("grid_imask", DataType.INT, "grid_size");
        geoFile.addVariable("grid_corner_lat", DataType.FLOAT, "grid_size grid_corners");
        geoFile.addVariable("grid_corner_lon", DataType.FLOAT, "grid_size grid_corners");

        geoFile.addGlobalAttribute("title", "MMD geo-location in SCRIP format");

        geoFile.create();

        try {
            geoFile.write("grid_dims", Array.factory(new int[]{gx, gy * matchupCount}));

            final int[] sourceStart = {0, (ny >> 1) - (gy >> 1) * strideY, (nx >> 1) - (gx >> 1) * strideX};
            final int[] sourceShape = {1, gy * strideY, gx * strideX};
            final int[] sourceStride = {1, strideY, strideX};
            final int[] targetStart = {0};
            final int[] targetShape = {gy * gx};
            final Array maskData = Array.factory(DataType.INT, targetShape);

            final Variable sourceLat = findVariable(mmd, SENSOR_NAME + ".latitude");
            final Variable sourceLon = findVariable(mmd, SENSOR_NAME + ".longitude");

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
                geoFile.write("grid_center_lat", targetStart, latData.reshape(targetShape));
                geoFile.write("grid_center_lon", targetStart, lonData.reshape(targetShape));
                geoFile.write("grid_imask", targetStart, maskData);
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            try {
                geoFile.close();
            } catch (IOException ignored) {
            }
        }
        return geoFile.getLocation();
    }

    private static int matchupCount(Array sensorPatterns) {
        int matchupCount = 0;
        for (int i = 0; i < sensorPatterns.getSize(); ++i) {
            if ((sensorPatterns.getInt(i) & SENSOR_PATTERN) != 0) {
                matchupCount++;
            }
        }
        return matchupCount;
    }

    private static Dimension findDimension(NetcdfFile file, String name) throws IOException {
        final Dimension d = file.findDimension(name);
        if (d == null) {
            throw new IOException(MessageFormat.format("Expected dimension ''{0}''.", name));
        }
        return d;
    }

    private static Variable findVariable(NetcdfFile file, String name) throws IOException {
        final Variable v = file.findVariable(NetcdfFile.escapeName(name));
        if (v == null) {
            throw new IOException(MessageFormat.format("Expected variable ''{0}''.", name));
        }
        return v;
    }
}
