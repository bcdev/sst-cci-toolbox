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

import org.junit.Test;

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
}