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

package org.esa.cci.sst.tools;

import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Ralf Quast
 */
public class JobRunnerTest {

    @Test
    public void testResponse_NoLineExpected() throws Exception {
        final File executable = new File("/bin/sleep");
        if (executable.canExecute()) {
            final JobRunner runner = new JobRunner();
            final String jobname = "test";
            final String command = "/bin/sleep 1";

            final List<String> lines = runner.submit(jobname, command);
            assertNotNull(lines);
            assertTrue(lines.isEmpty());
        }
    }

    @Test
    public void testResponse_SingleLineExpected() throws Exception {
        final File executable = new File("/bin/echo");
        if (executable.canExecute()) {
            final JobRunner runner = new JobRunner();
            final String jobname = "test";
            final String command = "/bin/echo Job <1711> is submitted successfully";

            final List<String> lines = runner.submit(jobname, command);
            assertNotNull(lines);
            assertEquals(1, lines.size());
            assertEquals("Job <1711> is submitted successfully", lines.get(0));
        }
    }

}
