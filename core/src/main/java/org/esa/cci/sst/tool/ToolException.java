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

package org.esa.cci.sst.tool;

/**
 * This runtime exception shall thrown by MMS tools when an error has
 * occurred from which a recovery is not possible.
 *
 * @author Ralf Quast
 */
public class ToolException extends RuntimeException {

    public static final int UNKNOWN_ERROR = 1;
    public static final int CONFIGURATION_FILE_NOT_FOUND_ERROR = 2;
    public static final int CONFIGURATION_FILE_IO_ERROR = 3;
    public static final int COMMAND_LINE_ARGUMENTS_PARSE_ERROR = 4;
    public static final int TOOL_CONFIGURATION_ERROR = 11;
    public static final int TOOL_USAGE_ERROR = 12;
    public static final int TOOL_ERROR = 21;
    public static final int TOOL_IO_ERROR = 22;
    public static final int TOOL_DB_ERROR = 31;
    public static final int TOOL_INTERNAL_ERROR = 41;
    public static final int ZERO_MATCHUPS_ERROR = 51;

    private final int exitCode;

    /**
     * Constructs a ne instance of this class.
     *
     * @param message  The error message.
     * @param exitCode The exit code which shall be returned to the system.
     */
    public ToolException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    /**
     * Constructs a ne instance of this class.
     *
     * @param message  The error message.
     * @param cause    The cause of the error.
     * @param exitCode The exit code which shall be returned to the system.
     */
    public ToolException(String message, Throwable cause, int exitCode) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public ToolException(Throwable cause, int exitCode) {
        super(cause);
        this.exitCode = exitCode;
    }

    /**
     * Returns the exit code, which shall be returned to the system.
     *
     * @return the exit code.
     */
    public final int getExitCode() {
        return exitCode;
    }
}
