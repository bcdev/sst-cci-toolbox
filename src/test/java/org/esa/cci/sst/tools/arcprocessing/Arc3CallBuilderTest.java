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

package org.esa.cci.sst.tools.arcprocessing;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class Arc3CallBuilderTest {

    @Test
    public void testGetDefaultTargetFileName() throws Exception {
        final String defaultTargetFileName = Arc3CallBuilder.getDefaultTargetFileName("mmd.nc");
        assertEquals("mmd_ARC3.nc", defaultTargetFileName);
    }

    @SuppressWarnings({"InstanceMethodNamingConvention"})
    @Test(expected = IllegalStateException.class)
    public void testValidateSourceFile_FailingWithNull() throws Exception {
        Arc3CallBuilder.validateSourceFilename(null);
    }

    @SuppressWarnings({"InstanceMethodNamingConvention"})
    @Test(expected = IllegalStateException.class)
    public void testValidateSourceFile_FailingWithCannotOpen() throws Exception {
        Arc3CallBuilder.validateSourceFilename(getClass().getResource("no_netcdf_file").getFile());
    }

    @Test
    public void testValidateSourceFile() throws Exception {
        try {
            Arc3CallBuilder.validateSourceFilename(getClass().getResource("empty_test.nc").getFile());
        } catch (Throwable t) {
            fail();
        }
    }
}
