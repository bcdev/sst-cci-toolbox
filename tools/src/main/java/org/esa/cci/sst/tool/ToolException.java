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

/**
 * An exception that will cause a tool to exit.
 *
 * @author Norman Fomferra
 */
public class ToolException extends Exception {
    private final ExitCode exitCode;

    public ToolException(ExitCode exitCode) {
        this.exitCode = exitCode;
    }

    public ToolException(Throwable cause, ExitCode exitCode) {
        super(cause);
        this.exitCode = exitCode;
    }

    public ToolException(String message, ExitCode exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public ToolException(String message, Throwable cause, ExitCode exitCode) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public ExitCode getExitCode() {
        return exitCode;
    }
}
