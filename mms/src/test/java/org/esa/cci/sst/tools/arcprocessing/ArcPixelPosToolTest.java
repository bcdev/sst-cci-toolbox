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

package org.esa.cci.sst.tools.arcprocessing;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.Raster;
import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class ArcPixelPosToolTest {

    private ArcPixelPosTool tool;

    @Before
    public void setUp() throws Exception {
        tool = new ArcPixelPosTool();
        final URL resource = getClass().getResource("mms-config.properties");
        final File resourceFile = new File(resource.toURI());
        tool.setCommandLineArgs(new String[]{"-c" + resourceFile.getPath()});
    }

    @Test
    public void testCreatePixelPosFile() throws Exception {
        final String file = getClass().getResource("NSS.GHRR.NP.D10365.S2336.E2359.B0978080.SV.LOC.nc").getFile();
        final Product product = tool.readProduct(file);
        assertNotNull(product);
        assertNotNull(product.getGeoCoding());
    }

    @Test
    public void testParseGeoPos() throws Exception {
        String[] testStrings = new String[]{
                "   171914  39.7359008789062   -23.683500289917",
                "   170942  -32.7700004577637   54.1438980102539",
        };

        GeoPos geoPos = tool.parseGeoPos(testStrings[0]);
        assertEquals(-23.683500289917, geoPos.lat, 0.01);
        assertEquals(39.7359008789062, geoPos.lon, 0.01);

        geoPos = tool.parseGeoPos(testStrings[1]);
        assertEquals(54.1438980102539, geoPos.lat, 0.01);
        assertEquals(-32.7700004577637, geoPos.lon, 0.01);
    }

    @Test
    public void testParseMatchupId() throws Exception {
        String[] testStrings = new String[]{
                "   171914  39.7359008789062   -23.683500289917",
                "   170942  -32.7700004577637   54.1438980102539",
        };

        String matchupId = tool.parseMatchupId(testStrings[0]);
        assertEquals("171914", matchupId);

        matchupId = tool.parseMatchupId(testStrings[1]);
        assertEquals("170942", matchupId);
    }

    @Test
    public void testNormaliseProduct() throws Exception {
        final String file = getClass().getResource("NSS.GHRR.NP.D10365.S2336.E2359.B0978080.SV.LOC.nc").getFile();
        final Product product = tool.readProduct(file);
        final Band lon = product.getBand("lon");
        final Raster raster = lon.getSourceImage().getData();
        for (int x = raster.getMinX(); x < raster.getWidth(); x++) {
            for (int y = raster.getMinY(); y < raster.getHeight(); y++) {
                final float value = raster.getSampleFloat(x, y, 0);
                assertTrue(value < 180.0001 || value > -180.0001);
            }
        }
    }
}
