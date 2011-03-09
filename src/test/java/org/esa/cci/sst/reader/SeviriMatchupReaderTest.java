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
import org.esa.cci.sst.data.DataSchema;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Variable;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class SeviriMatchupReaderTest {

    private NetcdfObservationReader reader;

    @Before
    public void setUp() throws Exception {
        reader = new SeviriMatchupReader();
        final DataFile dataFile = new DataFile();
        dataFile.setId(0);
        dataFile.setPath("testdata/SEVIRI_MD/sstmdb1_meteosat09_20100602.nc");
        final DataSchema dataSchema = new DataSchema();
        dataSchema.setId(1);
        dataSchema.setName("dataSchema");
        dataSchema.setSensorType("sensorType");
        dataFile.setDataSchema(dataSchema);
        reader.init(dataFile);
    }

    @Test
    public void testFetch() {
        try {
            for (int i = 0; i < 4435; i++) {
                reader.fetch(i);
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testWrite() {
        try {
            final List<Variable> variables = reader.getNcFile().getVariables();
            for (int i = 0; i < 5741; i++) {
                for (Variable variable : variables) {
                    reader.getData(i, variable.getName());
                }
            }
        } catch (Exception e) {
            fail();
        }
    }
}
