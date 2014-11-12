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
