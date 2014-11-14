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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("ThrowableInstanceNeverThrown")
public class ToolExceptionTest {

    @Test
    public void testCreateWithCauseAndExitCode() {
        final String message = "we test this";
        final Throwable throwable = new Throwable(message);

        final ToolException toolException = new ToolException(throwable, ToolException.TOOL_ERROR);
        assertEquals(ToolException.TOOL_ERROR, toolException.getExitCode());
        final Throwable cause = toolException.getCause();
        assertNotNull(cause);
        assertEquals(message, cause.getMessage());
    }
}
