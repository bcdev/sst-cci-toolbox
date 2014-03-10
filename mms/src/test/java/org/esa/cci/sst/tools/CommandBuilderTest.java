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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ralf Quast
 */
public class CommandBuilderTest {

    private CommandBuilder commandBuilder;

    @Before
    public void setUp() throws Exception {
        commandBuilder = new CommandBuilder("bsub");
        commandBuilder.option("-q", "lotus");
        commandBuilder.option("-P", "esacci_sst");
        commandBuilder.option("-oo", "test" + ".out");
        commandBuilder.option("-eo", "test" + ".err");
        commandBuilder.option("-J", "test");
        commandBuilder.option("-n", "1");
        commandBuilder.option("-cwd", ".");
        commandBuilder.option("-W", "05:00");
    }

    @Test
    public void testBuildCommand_SingleArgument() throws Exception {
        final String command = commandBuilder.build("sleep 100");
        assertEquals("bsub -J test -P esacci_sst -W 05:00 -cwd . -eo test.err -n 1 -oo test.out -q lotus sleep 100",
                     command);
    }

    @Test
    public void testBuildCommand_TwoArguments() throws Exception {
        final String command = commandBuilder.build("sleep", "100");
        assertEquals("bsub -J test -P esacci_sst -W 05:00 -cwd . -eo test.err -n 1 -oo test.out -q lotus sleep 100",
                     command);
    }

}
