package org.esa.cci.sst.tool;

import java.util.logging.Level;

/**
 * All possible log levels.
 *
 * @author Norman Fomferra
 */
public enum LogLevel {
    off(Level.OFF),
    error(Level.SEVERE),
    warning(Level.WARNING),
    info(Level.INFO),
    all(Level.ALL);

    private final Level value;

    private LogLevel(Level value) {
        this.value = value;
    }

    public Level getValue() {
        return value;
    }
}
