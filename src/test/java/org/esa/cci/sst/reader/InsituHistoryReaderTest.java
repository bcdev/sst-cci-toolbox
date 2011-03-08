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

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class InsituHistoryReaderTest {


    private InsituHistoryReader reader;

    @Before
    public void setUp() throws Exception {
        reader = new InsituHistoryReader();
    }

    @Test
    public void testGetTime() throws Exception {
        InsituHistoryReader.TimeInterval time = reader.getTime("insitu_WMOID_11851_20100101_20100101.nc");
        Date test = new GregorianCalendar(2010, 0, 1).getTime();
        assertEquals(test.getTime(), time.centralTime.getTime());
        assertEquals(0, time.timeRadius);

        time = reader.getTime("insitu_WMOID_11851_20100101_20100103.nc");
        test = new GregorianCalendar(2010, 0, 2).getTime();
        assertEquals(test.getTime(), time.centralTime.getTime());
        long oneDay = 1000 * 60 * 60 * 24;
        assertEquals(oneDay, time.timeRadius);

        time = reader.getTime("insitu_WMOID_11851_20110101_20130101.nc");
        test = new GregorianCalendar(2012, 0, 1, 12, 0).getTime();
        assertEquals(test.getTime(), time.centralTime.getTime());

        time = reader.getTime("insitu_WMOID_11851_20110101_20110111.nc");
        long threeHundredAndSixtyFiveDays = oneDay * 5;
        assertEquals(threeHundredAndSixtyFiveDays, time.timeRadius);
    }
}
