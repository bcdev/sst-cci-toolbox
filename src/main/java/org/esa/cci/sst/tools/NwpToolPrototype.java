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

package org.esa.cci.sst.tools;

import org.esa.beam.util.math.FracIndex;
import org.esa.cci.sst.util.ProcessRunner;
import org.esa.cci.sst.util.TemplateResolver;
import ucar.ma2.Array;
import ucar.ma2.DataType;
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
import java.util.Properties;

/**
 * NWP extraction prototype.
 *
 * @author Ralf Quast
 */
public class NwpToolPrototype {

    private static final String CDO_AN_TEMPLATE =
            "#! /bin/sh\n" +
            "${CDO} -f nc mergetime ${GGAS_TIMESTEPS} ${GGAS_TIME_SERIES} && " +
            "${CDO} -f grb mergetime ${GGAM_TIMESTEPS} ${GGAM_TIME_SERIES} && " +
            "${CDO} -f grb mergetime ${SPAM_TIMESTEPS} ${SPAM_TIME_SERIES} && " +
            // attention: chaining the operations below results in a loss of the y dimension in the result file
            "${CDO} -f nc -R -t ecmwf setreftime,${REFTIME} -remapbil,${GEO} -selname,Q,O3 ${GGAM_TIME_SERIES} ${GGAM_TIME_SERIES_REMAPPED} && " +
            "${CDO} -f nc -t ecmwf setreftime,${REFTIME} -remapbil,${GEO} -sp2gp -selname,LNSP,T ${SPAM_TIME_SERIES} ${SPAM_TIME_SERIES_REMAPPED} && " +
            "${CDO} -f nc merge -setreftime,${REFTIME} -remapbil,${GEO} -selname,CI,ASN,SSTK,TCWV,MSL,TCC,U10,V10,T2,D2,AL,SKT ${GGAS_TIME_SERIES} ${GGAM_TIME_SERIES_REMAPPED} ${SPAM_TIME_SERIES_REMAPPED} ${AN_TIME_SERIES}";

    private static final String CDO_FC_TEMPLATE =
            "#! /bin/sh\n" +
            "${CDO} -f nc mergetime ${GAFS_TIMESTEPS} ${GAFS_TIME_SERIES} && " +
            "${CDO} -f nc mergetime ${GGFS_TIMESTEPS} ${GGFS_TIME_SERIES} && " +
            // attention: chaining the operations below results in a loss of the y dimension in the result file
            "${CDO} -f nc setreftime,${REFTIME} -remapbil,${GEO} -selname,SSTK,MSL,BLH,U10,V10,T2,D2 ${GGFS_TIME_SERIES} ${GGFS_TIME_SERIES_REMAPPED} && " +
            "${CDO} -f nc merge -setreftime,${REFTIME} -remapbil,${GEO} -selname,SSHF,SLHF,SSRD,STRD,SSR,STR,EWSS,NSSS,E,TP ${GAFS_TIME_SERIES} ${GGFS_TIME_SERIES_REMAPPED} ${FC_TIME_SERIES}";

    @SuppressWarnings({"ConstantConditions"})
    public static void main(String[] args) throws IOException, InterruptedException {
        final NetcdfFile mmdFile = NetcdfFile.open("mmd.nc");
        try {
            final NetcdfFileWriteable geoFile = writeGeoFile(mmdFile);

            final Properties properties = new Properties();
            properties.setProperty("CDO", "/usr/local/bin/cdo");
            properties.setProperty("REFTIME", "1978-01-01,00:00:00,seconds");

            properties.setProperty("GEO", geoFile.getLocation());
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

            properties.setProperty("GAFS_TIMESTEPS", files("testdata/nwp", "gafs[0-9]*.nc"));
            properties.setProperty("GGFS_TIMESTEPS", files("testdata/nwp", "ggfs[0-9]*.nc"));
            properties.setProperty("GAFS_TIME_SERIES", createTempFile("gafs", ".nc", true).getPath());
            properties.setProperty("GGFS_TIME_SERIES", createTempFile("ggfs", ".nc", true).getPath());
            properties.setProperty("GGFS_TIME_SERIES_REMAPPED", createTempFile("ggfr", ".nc", true).getPath());
            properties.setProperty("FC_TIME_SERIES", createTempFile("forecast", ".nc", true).getPath());

            runner.execute(writeCdoScript(CDO_FC_TEMPLATE, properties).getPath());

            final NetcdfFile anFile = NetcdfFile.open(properties.getProperty("AN_TIME_SERIES"));
            try {
                writeAnalysisMmdFile(mmdFile, anFile);
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

    private static void writeAnalysisMmdFile(NetcdfFile mmd, NetcdfFile analysisFile) throws IOException {
        final Dimension matchupDimension = getDimension(mmd, "matchup");
        final Dimension nyDimension = getDimension(mmd, "metop.ny");
        final Dimension nxDimension = getDimension(mmd, "metop.nx");

        final NetcdfFileWriteable amd = NetcdfFileWriteable.createNew("an.nc", true);
        amd.addDimension(matchupDimension.getName(), matchupDimension.getLength());
        amd.addDimension("metop.nwp.nz", getDimension(analysisFile, "lev").getLength());
        amd.addDimension("metop.nwp.ny", nyDimension.getLength());
        amd.addDimension("metop.nwp.nx", nxDimension.getLength());

        final Variable matchupId = mmd.findVariable("matchup.id");

        amd.addVariable(matchupId.getName(), matchupId.getDataType(), matchupId.getDimensionsString());

        for (final Variable v : analysisFile.getVariables()) {
            if (v.getRank() == 4) {
                if (v.getDimension(1).getLength() == 1) {
                    amd.addVariable(v.getName(), v.getDataType(),
                                    "matchup metop.nwp.ny metop.nwp.nx");
                } else {
                    amd.addVariable(v.getName(), v.getDataType(),
                                    "matchup metop.nwp.nz metop.nwp.ny metop.nwp.nx");
                }
            }
        }

        amd.create();

        final int matchupCount = matchupDimension.getLength();
        final int ny = nyDimension.getLength();
        final int nx = nxDimension.getLength();

        final Array targetTimes = getVariable(mmd, "metop.time").read();
        final Array sourceTimes = getVariable(analysisFile, "time").read();

        try {
            for (int i = 0; i < matchupCount; i++) {
                final int[] sourceStart = {0, 0, i * ny, 0};
                final int[] sourceShape = {1, 0, ny, nx};

                final double targetTime = targetTimes.getDouble(i);
                final FracIndex fi = computeFracIndex(sourceTimes, targetTime);

                for (final Variable targetVariable : amd.getVariables()) {
                    final Variable sourceVariable = getVariable(analysisFile, targetVariable.getName());
                    sourceStart[0] = fi.i;
                    sourceShape[1] = sourceVariable.getShape(1);
                    final Array slice1 = sourceVariable.read(sourceStart, sourceShape);
                    sourceStart[0] = fi.i + 1;
                    final Array slice2 = sourceVariable.read(sourceStart, sourceShape);

                    for (int k = 0; k < slice1.getSize(); k++) {
                        slice2.setDouble(k, fi.f * slice1.getDouble(k) + (1.0 - fi.f) * slice2.getDouble(k));
                    }
                    final int[] targetShape = targetVariable.getShape();
                    targetShape[0] = 1;
                    final int[] targetStart = new int[targetShape.length];
                    targetStart[0] = i;
                    amd.write(targetVariable.getNameEscaped(), targetStart, slice2.reshape(targetShape));
                }
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }

    private static FracIndex computeFracIndex(Array sourceTimes, double targetTime) {
        for (int i = 1; i < sourceTimes.getSize(); i++) {
            final double maxTime = sourceTimes.getDouble(i);
            final double minTime = sourceTimes.getDouble(i - 1);
            if (targetTime >= minTime && targetTime <= maxTime) {
                final FracIndex fracIndex = new FracIndex();
                fracIndex.i = i - 1;
                fracIndex.f = (targetTime - minTime) / (maxTime - minTime);
                return fracIndex;
            }
        }
        throw new ToolException("Not enough time steps in NWP time series.", ToolException.TOOL_ERROR);
    }

    private static File createTempFile(String prefix, String suffix, boolean deleteOnExit) throws IOException {
        final File tempFile = File.createTempFile(prefix, suffix);
        if (deleteOnExit) {
            tempFile.deleteOnExit();
        }
        return tempFile;
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

    @SuppressWarnings({"ConstantConditions"})
    private static NetcdfFileWriteable writeGeoFile(NetcdfFile mmd) throws IOException {
        final Dimension matchupDimension = getDimension(mmd, "matchup");
        final Dimension nyDimension = getDimension(mmd, "metop.ny");
        final Dimension nxDimension = getDimension(mmd, "metop.nx");

        final int matchupCount = matchupDimension.getLength();
        final int ny = nyDimension.getLength();
        final int nx = nxDimension.getLength();

        final NetcdfFileWriteable geoFile = createGeoFile(matchupCount, ny, nx);
        try {
            geoFile.write("grid_dims", Array.factory(new int[]{nx, ny * matchupCount}));

            final int[] sourceShape = {1, ny, nx};
            final int[] sourceStart = {0, 0, 0};
            final int[] targetStart = {0};
            final int[] targetShape = {ny * nx};
            final Array maskData = Array.factory(DataType.INT, targetShape);

            final Variable sourceLat = getVariable(mmd, "metop.latitude");
            final Variable sourceLon = getVariable(mmd, "metop.longitude");

            for (int i = 0; i < matchupCount; i++) {
                sourceStart[0] = i;
                targetStart[0] = i * nx * ny;
                final Array latData = sourceLat.read(sourceStart, sourceShape);
                final Array lonData = sourceLon.read(sourceStart, sourceShape);
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
        return geoFile;
    }

    private static NetcdfFileWriteable createGeoFile(int matchupCount, int ny, int nx) throws IOException {
        final String location = createTempFile("geo", ".nc", true).getPath();
        final NetcdfFileWriteable geoFile = NetcdfFileWriteable.createNew(location, true);

        geoFile.addDimension("grid_size", matchupCount * ny * nx);
        geoFile.addDimension("grid_matchup", matchupCount);
        geoFile.addDimension("grid_ny", ny);
        geoFile.addDimension("grid_nx", nx);
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

        return geoFile;
    }

    private static Dimension getDimension(NetcdfFile file, String name) throws IOException {
        final Dimension d = file.findDimension(name);
        if (d == null) {
            throw new IOException(MessageFormat.format("Expected dimension ''{0}''.", name));
        }
        return d;
    }

    private static Variable getVariable(NetcdfFile file, String name) throws IOException {
        final Variable v = file.findVariable(NetcdfFile.escapeName(name));
        if (v == null) {
            throw new IOException(MessageFormat.format("Expected variable ''{0}''.", name));
        }
        return v;
    }
}
