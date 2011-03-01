/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.util;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import static org.junit.Assert.assertTrue;

/**
 * TODO fill out or delete
 *
 * @author Thomas Storm
 */
@Ignore
public class MmdFormatGeneratorTest {

    @BeforeClass
    public static void loadAgent() throws IOException, AttachNotSupportedException, AgentInitializationException,
                                     AgentLoadException {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        String pid = nameOfRunningVM.substring(0, p);

        VirtualMachine vm = VirtualMachine.attach(pid);
        vm.loadAgent("/Users/ralf/Public/repository/org/apache/openjpa/openjpa-all/2.0.0/openjpa-all-2.0.0.jar");
        vm.detach();
    }

    @Test
    public void testAddContent() throws Exception {
        final MmdFormatGenerator generator = new MmdFormatGenerator();
        final NetcdfFileWriteable file = NetcdfFileWriteable.createNew("");
        file.addDimension(MmdFormatGenerator.DIMENSION_NAME_MATCHUP, 0, false, true, true);
        final String var1 = "var1";
        file.addVariable(var1, DataType.LONG, MmdFormatGenerator.DIMENSION_NAME_MATCHUP);
        generator.addContent(file);
        assertTrue(file.findVariable(var1).read().getSize() > 0);
    }
}
