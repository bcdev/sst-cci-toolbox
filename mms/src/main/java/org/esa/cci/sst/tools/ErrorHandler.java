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

package org.esa.cci.sst.tools;

import org.esa.cci.sst.tool.ToolException;

/**
 * Error handler interface.
 */
public interface ErrorHandler {

    /**
     * Handles the occurrence of a {@link org.esa.cci.sst.tool.ToolException}, which eventually shall
     * always terminate the executable.
     *
     * @param e The tool exception.
     */
    public void terminate(ToolException e);

    /**
     * Treats the occurrence of a {@link Throwable} as a warning, which must not
     * terminate the executable.
     *
     * @param t       The throwable.
     * @param message The warning message.
     */
    public void warn(Throwable t, String message);
}
