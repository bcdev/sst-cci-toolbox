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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.NetcdfFileWriteable;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class SubsceneArc3CallBuilderTest {

    private NetcdfFileWriteable target;

    @Before
    public void setUp() throws Exception {
        final String someNetcdfResource = getClass().getResource("empty_test.nc").getFile();
        final File file = new File(new File(someNetcdfResource).getParent(), "test_writable.nc");
        target = NetcdfFileWriteable.createNew(file.getAbsolutePath());
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @After
    public void tearDown() throws Exception {
        new File(target.getLocation()).delete();
    }

    @Test
    public void testCreateSubsceneMmdFilename() throws Exception {
        final String subsceneMmdFilename = SubsceneArc3CallBuilder.createSubsceneMmdFilename("mmd.nc");
        assertEquals("mmd_subscenes.nc", subsceneMmdFilename);
    }
}
