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

package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.TestHelper;
import org.esa.cci.sst.tool.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFileWriter;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Storm
 */
public class MmdUpdaterTest {

    private String fileLocation;

    @Before
    public void setUp() throws Exception {
        fileLocation = TestHelper.getResourcePath(getClass(), "dummy.nc");
    }

    public MmdUpdater createUpdater(final String variableList) throws IOException {
        MmdUpdater updater = new MmdUpdater() {
            @Override
            public Configuration getConfig() {
                final Configuration properties = new Configuration();
                properties.put("mms.mmdupdate.variables", variableList);
                properties.put("mms.mmdupdate.mmd", fileLocation);
                return properties;
            }
        };

        final NetcdfFileWriter fileWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, fileLocation);
        fileWriter.addVariable(null, "insitu.sea_surface_temperature", DataType.INT, "");
        fileWriter.addVariable(null, "matchup.id", DataType.INT, "");
        fileWriter.addVariable(null, "avhrr_brightness_temperature_3b", DataType.INT, "");
        fileWriter.close();

        updater.openMmd();

        return updater;
    }

    @Test
    public void testOpenFile() throws Exception {
        final MmdUpdater updater = createUpdater("");
        updater.openMmd();
    }

    @Test
    @Ignore
    public void testParseOneVariable() throws Exception {
        final MmdUpdater updater = createUpdater("insitu.sea_surface_temperature");
        updater.parseVariables();
        assertEquals(1, updater.variables.size());
        assertEquals("insitu.sea_surface_temperature", updater.variables.get(0).getShortName());
    }

    @Test
    @Ignore
    public void testParseThreeVariables() throws Exception {
        final MmdUpdater updater = createUpdater(
                "insitu.sea_surface_temperature,matchup.id,avhrr_brightness_temperature_3b");
        updater.parseVariables();
        assertEquals(3, updater.variables.size());
        assertEquals("insitu.sea_surface_temperature", updater.variables.get(0).getShortName());
        assertEquals("matchup.id", updater.variables.get(1).getShortName());
        assertEquals("avhrr_brightness_temperature_3b", updater.variables.get(2).getShortName());
    }
}
