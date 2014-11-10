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

import org.esa.cci.sst.common.Grid;
import org.esa.cci.sst.common.GridDef;
import org.esa.cci.sst.common.cellgrid.YFlip;
import org.esa.cci.sst.netcdf.NcTools;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;

final class ClearSkyProbability {

    private static final String RESOURCE_NAME = "AATSR_prior_run081222.nc";

    private final GridDef gridDef;
    private Grid grid;

    ClearSkyProbability() {
        final NetcdfFile file;
        try {
            final URL url = getClass().getResource(RESOURCE_NAME);
            if (url == null) {
                throw new IllegalStateException(MessageFormat.format(
                        "Cannot find resource data for clear-sky probabilities ''{0}''..", RESOURCE_NAME));
            }
            file = NetcdfFile.openInMemory(url.toURI());
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(MessageFormat.format(
                    "Cannot open resource data for clear-sky probabilities ''{0}''..", RESOURCE_NAME), e);
        }

        gridDef = GridDef.createGlobal(1.0);
        try {
            grid = YFlip.create(NcTools.readGrid(file, "clr_prior", gridDef));
        } catch (IOException e) {
            throw new IllegalStateException(MessageFormat.format(
                    "Cannot read resource data for clear-sky probabilities ''{0}''..", RESOURCE_NAME), e);
        } finally {
            try {
                file.close();
            } catch (IOException ignored) {
            }
        }
    }

    double getSample(double lon, double lat) {
        final int x = gridDef.getGridX(lon, true);
        final int y = gridDef.getGridY(lat, true);

        return grid.getSampleDouble(x, y);
    }
}
