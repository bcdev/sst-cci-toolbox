/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.MmsTool;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Storm
 */
public class MmdReaderTest {

    public static final String TEST_WITH_ACTUAL_DATA = "test_with_actual_data.nc";
    private MmdReader mmdReader;
    private MmdIOHandler mmdIOHandler;

    @Before
    public void setUp() throws Exception {
        final MmsTool tool = new MmsTool("dummy", "version");
        tool.setCommandLineArgs(new String[]{"-csrc/test/config/mms-config.properties"});
        tool.initialize();
        final PersistenceManager persistenceManager = tool.getPersistenceManager();
        final String sensor = tool.getConfiguration().getProperty("mms.reingestion.sensor");
        final String schemaName = tool.getConfiguration().getProperty("mms.reingestion.schemaname");
        mmdIOHandler = new MmdIOHandler(tool, sensor, schemaName);
        final NetcdfFile mmd = NetcdfFile.open(getClass().getResource(TEST_WITH_ACTUAL_DATA).getFile());
        mmdReader = new MmdReader(mmdIOHandler, persistenceManager, mmd, sensor, schemaName);
    }

    @Test
    public void testGetNumRecords() throws Exception {
        initMmdReader(TEST_WITH_ACTUAL_DATA);
        assertEquals(10, mmdReader.getNumRecords());
    }

    private void initMmdReader(final String filename) throws IOException {
        final DataFile dataFile = new DataFile();
        final File file = new File(getClass().getResource(filename).getFile());
        dataFile.setPath(file.getPath());
        mmdIOHandler.init(dataFile);
    }

}