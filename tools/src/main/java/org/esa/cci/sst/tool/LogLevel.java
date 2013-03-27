/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.tool;

import java.util.logging.Level;

/**
 * All possible log levels.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public enum LogLevel {
    OFF(Level.OFF),
    ERROR(Level.SEVERE),
    WARNING(Level.WARNING),
    INFO(Level.INFO),
    ALL(Level.ALL);

    private final Level value;

    private LogLevel(Level value) {
        this.value = value;
    }

    public Level getValue() {
        return value;
    }
}
