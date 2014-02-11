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

import org.esa.cci.sst.tools.Configuration;
import org.junit.Before;
import org.junit.Test;
import sun.security.krb5.Config;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class MmdUpdaterTest {

    private String msg;
    private String fileLocation;

    @Before
    public void setUp() throws Exception {
        fileLocation = getClass().getResource("dummy.nc").getFile();
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

            @Override
            public Logger getLogger() {
                return new Logger("", null) {
                    @Override
                    public void warning(String msg) {
                        MmdUpdaterTest.this.msg = msg;
                    }
                };
            }
        };
        updater.mmd = NetcdfFileWriteable.createNew(fileLocation);
        updater.mmd.addVariable("insitu.sea_surface_temperature", DataType.INT, "");
        updater.mmd.addVariable("matchup.id", DataType.INT, "");
        updater.mmd.addVariable("avhrr_brightness_temperature_3b", DataType.INT, "");
        return updater;
    }

    @Test
    public void testOpenFile() throws Exception {
        final MmdUpdater updater = createUpdater("");
        updater.openMmd();
        assertTrue(updater.mmd != null);
        assertTrue(updater.mmd.getLocation().endsWith(fileLocation));
    }

    @Test
    public void testParseOneVariable() throws Exception {
        final MmdUpdater updater = createUpdater("insitu.sea_surface_temperature");
        updater.parseVariables();
        assertEquals(1, updater.variables.size());
        assertEquals("insitu.sea_surface_temperature", updater.variables.get(0).getShortName());
    }

    @Test
    public void testParseThreeVariables() throws Exception {
        final MmdUpdater updater = createUpdater("insitu.sea_surface_temperature,matchup.id,avhrr_brightness_temperature_3b");
        updater.parseVariables();
        assertEquals(3, updater.variables.size());
        assertEquals("insitu.sea_surface_temperature", updater.variables.get(0).getShortName());
        assertEquals("matchup.id", updater.variables.get(1).getShortName());
        assertEquals("avhrr_brightness_temperature_3b", updater.variables.get(2).getShortName());
    }

    @Test
    public void testLoggedWarning() throws Exception {
        final String bogusVariableName = "bogus_variable";
        final MmdUpdater updater = createUpdater(bogusVariableName);
        updater.parseVariables();
        assertEquals("Variable '" + bogusVariableName + "' not found in mmd file.", msg);
    }
}
