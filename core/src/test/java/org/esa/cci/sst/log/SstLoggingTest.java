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

package org.esa.cci.sst.log;

import org.junit.Test;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SstLoggingTest {

    @Test
    public void testGetDefaultLevel() {
        assertEquals(LogLevel.INFO, SstLogging.getDefaultLevel());
    }

    @Test
    public void testGetLogger() {
        final Logger logger = SstLogging.getLogger();
        assertNotNull(logger);
        assertEquals("org.esa.cci.sst", logger.getName());
        assertEquals(Level.INFO, logger.getLevel());

        final Handler[] handlers = logger.getHandlers();
        assertNotNull(handlers);
        assertEquals(1, handlers.length);
        assertEquals(Level.ALL, handlers[0].getLevel());
    }

    @Test
    public void testGetLogger_withLogLevel() {
        final Logger logger = SstLogging.getLogger(LogLevel.OFF);

        try {
            assertEquals(Level.OFF, logger.getLevel());
        } finally {
            // reset to default level tb 2014-11-12
            SstLogging.getLogger();
        }
    }

    @Test
    public void testSetLevelDebug() {
        final Logger logger = SstLogging.getLogger();

        try {
            SstLogging.setLevelDebug();

            assertEquals(Level.ALL, logger.getLevel());

        } finally {
            // reset to default level tb 2014-11-12
            SstLogging.getLogger();
        }
    }

    @Test
    public void testSetLevelSilent() {
        final Logger logger = SstLogging.getLogger();

        try {
            SstLogging.setLevelSilent();

            assertEquals(Level.WARNING, logger.getLevel());

        } finally {
            // reset to default level tb 2014-11-12
            SstLogging.getLogger();
        }
    }
}
