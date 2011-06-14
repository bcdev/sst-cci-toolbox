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

package org.esa.cci.sst.reader;

import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * @author Thomas Storm
 */
public class ReaderFactoryTest {

    @SuppressWarnings({"ReuseOfLocalVariable"})
    @Test
    public void testCreateReader() throws Exception {
        Reader handler = ReaderFactory.createReader("GunzipDecorator,ProductReader", "");
        assertNotNull(handler);
        assertTrue(handler instanceof GunzipDecorator);

        handler = ReaderFactory.createReader("GunzipDecorator,AaiProductReader", "");
        assertNotNull(handler);
        assertTrue(handler instanceof GunzipDecorator);

        handler = ReaderFactory.createReader("ProductReader", "");
        assertNotNull(handler);
        assertTrue(handler instanceof AbstractProductReader);

        handler = ReaderFactory.createReader("AaiProductReader", "");
        assertNotNull(handler);
        assertTrue(handler instanceof AaiProductReader);

        handler = ReaderFactory.createReader("MetopReader", "");
        assertNotNull(handler);
        assertTrue(handler instanceof MetopReader);

        handler = ReaderFactory.createReader("AtsrMdReader", "");
        assertNotNull(handler);
        assertTrue(handler instanceof AtsrMdReader);

        handler = ReaderFactory.createReader("SeviriReader", "");
        assertNotNull(handler);
        assertTrue(handler instanceof SeviriReader);

        handler = ReaderFactory.createReader("AvhrrMdReader", "");
        assertNotNull(handler);
        assertTrue(handler instanceof AvhrrMdReader);

    }

}
