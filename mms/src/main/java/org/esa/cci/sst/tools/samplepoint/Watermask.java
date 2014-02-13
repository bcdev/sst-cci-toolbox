package org.esa.cci.sst.tools.samplepoint;/*
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

import org.esa.cci.sst.common.cellgrid.GridDef;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;

final class Watermask {

    private static final String RESOURCE_NAME = "water.png";

    private final GridDef gridDef;
    private final Raster imageRaster;

    Watermask() {
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

    boolean isWater(double lon, double lat) {
        final int x = gridDef.getGridX(lon, true);
        final int y = gridDef.getGridY(lat, true);

        return imageRaster.getSample(x, y, 0) != 0;
    }
}
