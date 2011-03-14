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

package org.esa.cci.sst.util;

import org.junit.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class TimeUtilTest {

    @Test
    public void testToJulian() throws Exception {
        Date date = TimeUtil.dateOfJulianDate(2454428.185);
        Date testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20071123_172624_000");
        assertEquals(testTime, date);

        date = TimeUtil.dateOfJulianDate(2454477.394);
        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20080111_222721_599");
        assertEquals(testTime.getTime(), date.getTime());

        date = TimeUtil.dateOfJulianDate(2454115.05486);
        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20070114_141859_904");
        assertEquals(testTime.getTime(), date.getTime());
    }

}
