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

import org.esa.beam.common.PixelLocator;
import org.esa.cci.sst.grid.GridDef;

import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;

public final class Watermask {

    public static final byte INVALID_WATER_FRACTION = Byte.MIN_VALUE;
    private static final String RESOURCE_NAME = "water.png";

    private final GridDef gridDef;

    public Watermask() {
        gridDef = GridDef.createGlobal(0.01);
    }

    public boolean isWater(double lon, double lat) {
        final int x = gridDef.getGridX(lon, true);
        final int y = gridDef.getGridY(lat, true);

        return Container.WATERMASK_IMAGE.getRaster().getSample(x, y, 0) != 0;
    }

    public byte getWaterFraction(int x, int y, PixelLocator locator, int stepCountX, int stepCountY) {
        final Point2D g = new Point2D.Double();

        final double deltaX = 1.0 / (stepCountX - 1);
        final double deltaY = 1.0 / (stepCountY - 1);

        int waterCount = 0;
        int invalidCount = 0;

        for (int i = 0; i < stepCountX; i++) {
            final double u = i == 0 ? x : x + i * deltaX;
            for (int j = 0; j < stepCountY; j++) {
                final double v = j == 0 ? y : y + j * deltaY;
                if (locator.getGeoLocation(u, v, g)) {
                    if (isWater(g.getX(), g.getY())) {
                        waterCount++;
                    }
                } else {
                    invalidCount++;
                }
            }
        }

        final int count = stepCountX * stepCountY;
        if (invalidCount == count) {
            return INVALID_WATER_FRACTION;
        }
        return (byte) ((100.0 * waterCount) / count);
    }

    private static BufferedImage createWatermaskImage() {
        final BufferedImage watermaskImage;
        try {
            final URL url = Watermask.class.getResource(RESOURCE_NAME);
            if (url != null) {
                watermaskImage = ImageIO.read(url);
            } else {
                throw new IllegalStateException(MessageFormat.format(
                        "Resource for watermask image ''{0}'' not found.", RESOURCE_NAME));
            }
        } catch (IOException e) {
            throw new IllegalStateException(MessageFormat.format(
                    "Unable to read resource for watermask image ''{0}''.", RESOURCE_NAME), e);
        }
        return watermaskImage;
    }

    private static final class Container {

        private static final BufferedImage WATERMASK_IMAGE = createWatermaskImage();
    }
}
