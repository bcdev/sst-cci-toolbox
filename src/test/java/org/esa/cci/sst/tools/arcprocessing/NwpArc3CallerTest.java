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

import org.esa.cci.sst.tools.Constants;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class NwpArc3CallerTest {

    @Test
    public void testGetArc3OutputName() throws Exception {
        final Properties configuration = new Properties();
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_OUTPUT, "arc3_mmd_atsr.nc");
        final SplittedNwpArc3Caller caller = new SplittedNwpArc3Caller(configuration);
        final String arc3OutputName = caller.getUniqueOutputName(Constants.PROPERTY_MMS_ARC3_OUTPUT);
        final Date date = new Date();
        final String time = new SimpleDateFormat("yyyyMMddHHmm").format(date);
        assertEquals(String.format("arc3_mmd_atsr_%s.nc", time), arc3OutputName);
    }
}
