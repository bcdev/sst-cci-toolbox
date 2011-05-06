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

package org.esa.cci.sst.data;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataFileTest {

    private DataFile dataFile;

    @Test
    public void testSetGetPath() {
        assertNull(dataFile.getPath());

        dataFile.setPath("a\\b");
        assertEquals("a/b", dataFile.getPath());

        dataFile.setPath(null);
        assertNull(dataFile.getPath());
    }

    @Before
    public void init() {
        dataFile = new DataFile();
    }
}
