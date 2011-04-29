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
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

/**
 * @author Thomas Storm
 */
public class SeviriIOHandlerTest {

    private MdIOHandler ioHandler;

    @Before
    public void setUp() throws Exception {
        ioHandler = new SeviriIOHandler("seviri");
        final DataFile dataFile = new DataFile();
        dataFile.setId(0);
        dataFile.setPath("testdata/SEVIRI_MD/sstmdb1_meteosat09_20100602.nc");
        ioHandler.init(dataFile);
    }

    @Test
    public void testFetch() throws IOException {
        for (int i = 0; i < 4435; i++) {
            ioHandler.fetch(i);
        }
    }

    @Test
    public void testWrite() throws IOException {
        final List<Variable> variables = ioHandler.getNcFile().getVariables();
        for (int i = 0; i < 5741; i++) {
            for (Variable variable : variables) {
                ioHandler.getData(variable.getName(), i);
            }
        }
    }
}
