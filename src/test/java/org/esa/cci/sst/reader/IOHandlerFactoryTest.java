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
public class IOHandlerFactoryTest {

    @Test
    public void testCreateReader() throws Exception {
        IOHandler handler;
        handler = IOHandlerFactory.createHandler("GunzipDecorator,ProductHandler", "");
        assertNotNull(handler);
        assertTrue(handler instanceof GunzipDecorator);

        handler = IOHandlerFactory.createHandler("GunzipDecorator,AaiProductHandler", "");
        assertNotNull(handler);
        assertTrue(handler instanceof GunzipDecorator);

        handler = IOHandlerFactory.createHandler("ProductHandler", "");
        assertNotNull(handler);
        assertTrue(handler instanceof AbstractProductHandler);

        handler = IOHandlerFactory.createHandler("AaiProductHandler", "");
        assertNotNull(handler);
        assertTrue(handler instanceof AaiProductHandler);

        handler = IOHandlerFactory.createHandler("MetopIOHandler", "");
        assertNotNull(handler);
        assertTrue(handler instanceof MetopIOHandler);

        handler = IOHandlerFactory.createHandler("AtsrMdIOHandler", "");
        assertNotNull(handler);
        assertTrue(handler instanceof AtsrMdIOHandler);

        handler = IOHandlerFactory.createHandler("SeviriIOHandler", "");
        assertNotNull(handler);
        assertTrue(handler instanceof SeviriIOHandler);

    }

}
