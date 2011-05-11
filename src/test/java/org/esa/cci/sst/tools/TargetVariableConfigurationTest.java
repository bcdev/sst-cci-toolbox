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

import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.rules.RuleException;
import org.esa.cci.sst.util.IoUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TargetVariableConfigurationTest {

    private final ColumnRegistry registry = new ColumnRegistry();

    @Before
    public void initRegistry() throws IOException, URISyntaxException {
        registerSourceColumns("seviri.nc", "seviri");
        registerSourceColumns("metop.nc", "metop");
        registerSourceColumns("aatsr_md.nc", "aatsr_md");
        registerSourceColumns("ams.nc", "amsre");
        registerSourceColumns("tmi.nc", "tmi");
        registerSourceColumns("atsr.1.nc", "atsr1");
        registerSourceColumns("atsr.2.nc", "atsr2");
        registerSourceColumns("atsr.3.nc", "atsr3");
    }

    @After
    public void clearRegistry() {
        registry.clear();
    }

    @Test
    public void testRegisterColumns() throws ParseException, RuleException {
        final InputStream is = getClass().getResourceAsStream("mmd-variables.config");

        assertNotNull(is);

        final List<String> nameList;
        try {
            nameList = registry.registerColumns(is);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }

        assertNotNull(nameList);
        assertEquals(81, nameList.size());

        assertEquals("aatsr_md.atsr.L2_confidence_word", nameList.get(0));
        assertEquals("tmi.wind_speed", nameList.get(nameList.size() - 1));

        testMetopColumn();
    }

    private void testMetopColumn() {
        final Item targetColumn = registry.getColumn("metop.brightness_temperature.037");

        assertEquals("matchup metop.ny metop.nx", targetColumn.getDimensions());
        assertNotNull(registry.getConverter(targetColumn));
        assertNotNull("metop.IR037", registry.getSourceColumn(targetColumn).getName());
    }

    private void registerSourceColumns(String fileName, String sensor) throws IOException,
                                                                              URISyntaxException {
        NetcdfFile netcdfFile = null;
        try {
            final File sensorFile = new File(TargetVariableConfigurationTest.class.getResource(fileName).toURI());
            netcdfFile = NetcdfFile.open(sensorFile.getPath());
            for (final Variable variable : netcdfFile.getVariables()) {
                final Item column = IoUtil.createColumnBuilder(variable, sensor).build();
                registry.register(column);
            }
        } finally {
            if (netcdfFile != null) {
                netcdfFile.close();
            }
        }
    }

}
