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

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Thomas Storm
 */
public class ReaderFactoryTest {

    @Test
    public void testCreateDecoratedProductReader() throws Exception {
        final Reader reader = ReaderFactory.createReader("GunzipDecorator,ProductReader", "");
        assertNotNull(reader);
        assertTrue(reader instanceof GunzipDecorator);
    }

    @Test
    public void testCreateDecoratedAaiProductReader() throws Exception {
        final Reader reader = ReaderFactory.createReader("GunzipDecorator,AaiProductReader", "");
        assertNotNull(reader);
        assertTrue(reader instanceof GunzipDecorator);
    }

    @Test
    public void testCreateAaiProductReader() throws Exception {
        final Reader reader = ReaderFactory.createReader("AaiProductReader", "");
        assertNotNull(reader);
        assertTrue(reader instanceof AaiProductReader);
    }

    @Test
    public void testCreateMdReaders() throws Exception {
        Reader reader = ReaderFactory.createReader("MetopMdReader", "");
        assertNotNull(reader);
        assertTrue(reader instanceof MetopMdReader);

        reader = ReaderFactory.createReader("AtsrMdReader", "");
        assertNotNull(reader);
        assertTrue(reader instanceof AtsrMdReader);

        reader = ReaderFactory.createReader("SeviriMdReader", "");
        assertNotNull(reader);
        assertTrue(reader instanceof SeviriMdReader);

        reader = ReaderFactory.createReader("AvhrrMdReader", "");
        assertNotNull(reader);
        assertTrue(reader instanceof AvhrrMdReader);

        reader = ReaderFactory.createReader("InsituReader", "");
        assertNotNull(reader);
        assertTrue(reader instanceof InsituReader);
    }

    @Test
    public void testCreateInsituReader() throws Exception {
        final Reader reader = ReaderFactory.createReader("InsituReader", "");
        assertNotNull(reader);
        assertTrue(reader instanceof InsituReader);
    }

    @Test
    public void testCreateProductReader() throws Exception {
        final Reader reader = ReaderFactory.createReader("ProductReader", "");
        assertNotNull(reader);
        assertTrue(reader instanceof ProductReader);
    }

    @Test
    public void testCreateProductReader_with_dirty_mask() throws Exception {
        final Reader reader = ReaderFactory.createReader("ProductReader", "", "false");
        assertNotNull(reader);
        assertTrue(reader instanceof ProductReader);
    }
}
