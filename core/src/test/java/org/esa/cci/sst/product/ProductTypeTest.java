/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.product;

import org.esa.cci.sst.common.ProcessingLevel;
import org.esa.cci.sst.grid.GridDef;
import org.junit.Test;

import java.io.File;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Norman Fomferra
 */
public class ProductTypeTest {

    @Test
    public void testFileTypes() throws Exception {
        assertSame(CciL2FileType.INSTANCE, ProductType.CCI_L2P.getFileType());
        assertSame(ArcL3FileType.INSTANCE, ProductType.ARC_L3U.getFileType());
        assertSame(CciL3FileType.INSTANCE, ProductType.CCI_L3U.getFileType());
        assertSame(CciL3FileType.INSTANCE, ProductType.CCI_L3C.getFileType());
        assertSame(CciL4FileType.INSTANCE, ProductType.CCI_L4.getFileType());
    }

    @SuppressWarnings("OctalInteger")
    @Test
    public void testParseDate() throws ParseException {
        final Date date = ProductType.CCI_L4.parseDate(new File("20070622_we_dont_care"));

        final Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        assertEquals(2007, calendar.get(Calendar.YEAR));
        assertEquals(06, calendar.get(Calendar.MONTH) + 1);
        assertEquals(22, calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testGetDefaultFileNameRegexp() {
        final String regex = ProductType.CCI_L3C.getDefaultFilenameRegex();

        assertEquals("\\d{14}-ESACCI-L3[CU]_GHRSST-SST((skin)|(subskin)|(depth)|(fnd))-((ATSR1)|(ATSR2)|(AATSR)|(AMSRE)|(AVHRR_MTA)|(SEVIRI_SST)|(TMI))-((LT)|(DM))-v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc", regex);
    }

    @Test
    public void estGetGriDef() {
        final GridDef gridDef = ProductType.CCI_L3U.getGridDef();

        assertEquals(3600, gridDef.getHeight());
        assertEquals(7200, gridDef.getWidth());
        assertEquals(1, gridDef.getTime());
    }

    @Test
    public void testGetProcessingLevel() {
        final ProcessingLevel processingLevel = ProductType.ARC_L3U.getProcessingLevel();

        assertEquals(ProcessingLevel.L3U, processingLevel);
    }
}