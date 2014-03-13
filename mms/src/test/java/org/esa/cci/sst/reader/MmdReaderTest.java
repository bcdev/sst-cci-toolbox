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

import org.esa.cci.sst.TestHelper;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.tools.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Thomas Storm
 */
public class MmdReaderTest {

    public static final String TEST_WITH_ACTUAL_DATA = "test_with_actual_data.nc";

    private MmdReader mmdReader;

    @Before
    public void setUp() throws Exception {
        final Configuration configuration = new Configuration();
        configuration.put("mms.reingestion.sensor", "avhrr.n15");
        configuration.put("mms.reingestion.pattern", "10000");
        configuration.put(Configuration.KEY_MMS_REINGESTION_LOCATED, "true");

        mmdReader = new MmdReader("avhrr.n15");
        mmdReader.setConfiguration(configuration);
    }

    @After
    public void tearDown() throws Exception {
        try {
            mmdReader.close();
        } catch (IllegalStateException ignore) {
            // ok
        }
        mmdReader = null;
    }

    @Test(expected = IOException.class)
    public void testFailingInit() throws Exception {
        final DataFile dataFile = new DataFile();
        dataFile.setPath("pom.xml");
        mmdReader.init(dataFile, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailingClose() throws Exception {
        mmdReader.close();
    }

    @Test
    public void testInit() throws Exception {
        initMmdReader(TEST_WITH_ACTUAL_DATA);
        final Field mmd = mmdReader.getClass().getDeclaredField("ncFile");
        mmd.setAccessible(true);
        final NetcdfFile mmdObj = (NetcdfFile) mmd.get(mmdReader);

        assertNotNull(mmdObj);
        final String location = mmdObj.getLocation();
        int index = location.lastIndexOf('/');
        if (index == -1) {
            index = location.lastIndexOf('\\');
        }
        assertEquals(TEST_WITH_ACTUAL_DATA, location.substring(index + 1, location.length()));
    }

    private void initMmdReader(final String filename) throws IOException {
        final DataFile dataFile = new DataFile();
        final String filePath = TestHelper.getResourcePath(getClass(), filename);
        dataFile.setPath(filePath);
        mmdReader.init(dataFile, null);
    }
}
