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

package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Thomas Storm
 */
public class MmdIOHandlerTest {

    public static final String TEST_WITH_ACTUAL_DATA = "test_with_actual_data.nc";

    private MmdIOHandler mmdIOHandler;

    @Before
    public void setUp() throws Exception {
        final Properties configuration = new Properties();
        configuration.setProperty("mms.reingestion.sensor", "avhrr.n15");
        configuration.setProperty("mms.reingestion.pattern", "10000");
        configuration.setProperty("mms.reingestion.located", "yes");

        mmdIOHandler = new MmdIOHandler(configuration);
    }

    @After
    public void tearDown() throws Exception {
        try {
            mmdIOHandler.close();
        } catch (IllegalStateException ignore) {
            // ok
        }
        mmdIOHandler = null;
    }

    @Test(expected = IOException.class)
    public void testFailingInit() throws Exception {
        final DataFile dataFile = new DataFile();
        dataFile.setPath("pom.xml");
        mmdIOHandler.init(dataFile);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailingClose() throws Exception {
        mmdIOHandler.close();
    }

    @Test
    public void testInit() throws Exception {
        initMmdReader(TEST_WITH_ACTUAL_DATA);
        final Field mmd = mmdIOHandler.getClass().getDeclaredField("ncFile");
        mmd.setAccessible(true);
        final NetcdfFile mmdObj = (NetcdfFile) mmd.get(mmdIOHandler);

        assertNotNull(mmdObj);
        final String location = mmdObj.getLocation();
        assertEquals(TEST_WITH_ACTUAL_DATA, location.substring(location.lastIndexOf("/") + 1, location.length()));
    }

    private void initMmdReader(final String filename) throws IOException {
        final DataFile dataFile = new DataFile();
        final File file = new File(getClass().getResource(filename).getFile());
        dataFile.setPath(file.getPath());
        mmdIOHandler.init(dataFile);
    }

}
