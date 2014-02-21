package org.esa.cci.sst.util;/*
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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.cci.sst.common.cellgrid.GridDef;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;

public final class Watermask {

    private static final String RESOURCE_NAME = "water.png";

    private final GridDef gridDef;
    private final Raster imageRaster;

    public Watermask() {
        final BufferedImage waterImage;
        try {
            final URL url = getClass().getResource(RESOURCE_NAME);
            if (url != null) {
                waterImage = ImageIO.read(url);
            } else {
                throw new IllegalStateException(MessageFormat.format(
                        "Cannot find resource for water mask image ''{0}''.", RESOURCE_NAME));
            }
        } catch (IOException e) {
            throw new IllegalStateException(MessageFormat.format(
                    "Cannot read resource for water mask image ''{0}''.", RESOURCE_NAME), e);
        }
        imageRaster = waterImage.getRaster();
        gridDef = GridDef.createGlobal(0.01);
    }

    public boolean isWater(double lon, double lat) {
        final int x = gridDef.getGridX(lon, true);
        final int y = gridDef.getGridY(lat, true);

        return imageRaster.getSample(x, y, 0) != 0;
    }

    public byte getWaterFraction(GeoCoding geoCoding, int pixelX, int pixelY) {
        final GeoPos g = new GeoPos();
        final PixelPos p = new PixelPos();

        final int stepCountX = 11;
        final int stepCountY = 11;
        final double deltaX = 1.0 / (stepCountX - 1);
        final double deltaY = 1.0 / (stepCountY - 1);

        int waterCount = 0;
        int invalidCount = 0;

        for (int i = 0; i < stepCountY; i++) {
            final double y = pixelY + i * deltaY;
            for (int j = 0; j < stepCountX; j++) {
                final double x = pixelX + j * deltaX;

                p.setLocation(x, y);
                geoCoding.getGeoPos(p, g);

                if (g.isValid()) {
                    if (isWater(g.getLon(), g.getLat())) {
                        waterCount++;
                    }
                } else {
                    invalidCount++;
                }
            }
        }

        final int count = stepCountX * stepCountY;
        if (invalidCount == count) {
            return Byte.MIN_VALUE;
        }
        return (byte) ((100.0 * waterCount) / count);
    }

}
