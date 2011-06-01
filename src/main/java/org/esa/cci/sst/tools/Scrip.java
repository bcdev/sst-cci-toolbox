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

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;

public class Scrip {

    public static void main(String[] args) throws IOException, InvalidRangeException {
        NetcdfFile source = null;
        try {
            source = NetcdfFile.open("mmd.nc");
            final Dimension matchupDimension = source.findDimension("matchup");
            final Dimension nyDimension = source.findDimension("metop.ny");
            final Dimension nxDimension = source.findDimension("metop.nx");

            final int matchupCount = matchupDimension.getLength();
            final int ny = nyDimension.getLength();
            final int nx = nxDimension.getLength();

            NetcdfFileWriteable target = null;
            try {
                target = createTargetFile(matchupCount, ny, nx);
                target.write("grid_dims", Array.factory(new int[]{nx, ny * matchupCount}));

                final int[] sourceShape = {1, ny, nx};
                final int[] sourceStart = {0, 0, 0};
                final int[] targetStart = {0};
                final int[] targetShape = {ny * nx};
                final Array maskData = Array.factory(DataType.INT, targetShape);

                final Variable sourceLat = source.findVariable(NetcdfFile.escapeName("metop.latitude"));
                final Variable sourceLon = source.findVariable(NetcdfFile.escapeName("metop.longitude"));

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
                    target.write("grid_center_lat", targetStart, latData.reshape(targetShape));
                    target.write("grid_center_lon", targetStart, lonData.reshape(targetShape));
                    target.write("grid_imask", targetStart, maskData);
                }
            } finally {
                if (target != null) {
                    target.close();
                }
            }
        } finally {
            if (source != null) {
                source.close();
            }
        }
    }

    private static NetcdfFileWriteable createTargetFile(int matchupCount, int ny, int nx) throws IOException {
        final NetcdfFileWriteable target = NetcdfFileWriteable.createNew("geo.nc", true);
        target.addDimension("grid_size", matchupCount * ny * nx);
        target.addDimension("grid_matchup", matchupCount);
        target.addDimension("grid_ny", ny);
        target.addDimension("grid_nx", nx);
        target.addDimension("grid_corners", 4);
        target.addDimension("grid_rank", 2);

        target.addVariable("grid_dims", DataType.INT, "grid_rank");
        target.addVariable("grid_center_lat", DataType.FLOAT, "grid_size").addAttribute(
                new Attribute("units", "degrees"));
        target.addVariable("grid_center_lon", DataType.FLOAT, "grid_size").addAttribute(
                new Attribute("units", "degrees"));
        target.addVariable("grid_imask", DataType.INT, "grid_size");
        target.addVariable("grid_corner_lat", DataType.FLOAT, "grid_size grid_corners");
        target.addVariable("grid_corner_lon", DataType.FLOAT, "grid_size grid_corners");

        target.addGlobalAttribute("title", "MMD geo-location in SCRIP format");
        target.create();

        return target;
    }
}
