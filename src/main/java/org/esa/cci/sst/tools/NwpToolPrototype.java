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
import java.util.Properties;

public class NwpToolPrototype {

    private static final String CDO_AN_TEMPLATE =
            "#! /bin/sh \n" +
            "${CDO} -f nc mergetime ${GGAS_TIMESTEPS} ${GGAS_TIME_SERIES} \n" +
            "${CDO} -f grb mergetime ${GGAM_TIMESTEPS} ${GGAM_TIME_SERIES} \n" +
            "${CDO} -f grb mergetime ${SPAM_TIMESTEPS} ${SPAM_TIME_SERIES} \n" +
            "${CDO} -s -f nc -R -t ecmwf merge -remapbil,${GEO} ${GGAS_TIME_SERIES} -selvar,Q,O3 -remapbil,${GEO} ${GGAM_TIME_SERIES} -selvar,LNSP,T -remapbil,${GEO} -sp2gp ${SPAM_TIME_SERIES} ${AN_TIME_SERIES}";

    private static final String CDO_FC_TEMPLATE =
            "#! /bin/sh \n" +
            "${CDO} -f nc mergetime ${GAFS_TIMESTEPS} ${GAFS_TIME_SERIES} \n" +
            "${CDO} -f nc mergetime ${GGFS_TIMESTEPS} ${GGFS_TIME_SERIES} \n" +
            "${CDO} -s -f nc merge -remapbil,${GEO} ${GAFS_TIME_SERIES} -remapbil,${GEO} ${GGFS_TIME_SERIES} ${FC_TIME_SERIES}";

    public static void main(String[] args) throws IOException, InterruptedException {
        writeGeoFile("geo.nc", "mmd.nc");

        final Properties properties = new Properties();
        properties.setProperty("CDO", "/usr/local/bin/cdo");
        properties.setProperty("GEO", "geo.nc");
        properties.setProperty("GGAS_TIMESTEPS", files("testdata/nwp", "ggas[0-9]*.nc"));
        properties.setProperty("GGAM_TIMESTEPS", files("testdata/nwp", "ggam[0-9]*.grb"));
        properties.setProperty("SPAM_TIMESTEPS", files("testdata/nwp", "spam[0-9]*.grb"));
        properties.setProperty("GGAS_TIME_SERIES", File.createTempFile("ggas", ".nc").getPath());
        properties.setProperty("GGAM_TIME_SERIES", File.createTempFile("ggam", ".nc").getPath());
        properties.setProperty("SPAM_TIME_SERIES", File.createTempFile("spam", ".nc").getPath());
        properties.setProperty("AN_TIME_SERIES", "an.nc");

        final ProcessRunner runner = new ProcessRunner("org.esa.cci.sst");
        runner.execute(writeCdoScript(CDO_AN_TEMPLATE, properties).getPath());

        properties.setProperty("GAFS_TIMESTEPS", files("testdata/nwp", "gafs[0-9]*.nc"));
        properties.setProperty("GGFS_TIMESTEPS", files("testdata/nwp", "ggfs[0-9]*.nc"));
        properties.setProperty("GAFS_TIME_SERIES", File.createTempFile("gafs", ".nc").getPath());
        properties.setProperty("GGFS_TIME_SERIES", File.createTempFile("ggfs", ".nc").getPath());
        properties.setProperty("FC_TIME_SERIES", "fc.nc");

        runner.execute(writeCdoScript(CDO_FC_TEMPLATE, properties).getPath());

        printVariables("an.nc");
        printVariables("fc.nc");

        // todo - temporal interpolation of AN
    }

    private static void printVariables(String location) throws IOException {
        final NetcdfFile anFile = NetcdfFile.open(location);
        try {
            for (final Variable v : anFile.getVariables()) {
                System.out.println("variable = " + v.getName());
            }
        } finally {
            try {
                anFile.close();
            } catch (IOException ignored) {
            }
        }
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
    private static void writeGeoFile(String geoFileLocation, String mmdFileLocation) throws IOException {
        final NetcdfFile mmdFile = NetcdfFile.open(mmdFileLocation);
        try {
            final Dimension matchupDimension = mmdFile.findDimension("matchup");
            final Dimension nyDimension = mmdFile.findDimension("metop.ny");
            final Dimension nxDimension = mmdFile.findDimension("metop.nx");

            final int matchupCount = matchupDimension.getLength();
            final int ny = nyDimension.getLength();
            final int nx = nxDimension.getLength();

            final NetcdfFileWriteable geoFile = defineGeoFile(geoFileLocation, matchupCount, ny, nx);
            try {
                geoFile.write("grid_dims", Array.factory(new int[]{nx, ny * matchupCount}));

                final int[] sourceShape = {1, ny, nx};
                final int[] sourceStart = {0, 0, 0};
                final int[] targetStart = {0};
                final int[] targetShape = {ny * nx};
                final Array maskData = Array.factory(DataType.INT, targetShape);

                final Variable sourceLat = mmdFile.findVariable(NetcdfFile.escapeName("metop.latitude"));
                final Variable sourceLon = mmdFile.findVariable(NetcdfFile.escapeName("metop.longitude"));

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
        } finally {
            try {
                mmdFile.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static NetcdfFileWriteable defineGeoFile(String location, int matchupCount, int ny, int nx) throws
                                                                                                        IOException {
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
}
