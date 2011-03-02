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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class MmdFormatGeneratorTest {

    private static final File M2_REPO = new File("/Users/ralf/Public/repository");
    private static final String TEST_FILE = "test.nc";
    private NetcdfFileWriteable file;
    private MmdFormatGenerator generator;

    @Before
    public void setUp() throws Exception {
        generator = new MmdFormatGenerator();
        file = generator.generateMmdFile(TEST_FILE);
    }

    @BeforeClass
    public static void loadAgent() throws IOException, AttachNotSupportedException, AgentInitializationException,
                                          AgentLoadException {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        String pid = nameOfRunningVM.substring(0, p);

        VirtualMachine vm = VirtualMachine.attach(pid);
        vm.loadAgent(new File(M2_REPO, "org/apache/openjpa/openjpa-all/2.0.0/openjpa-all-2.0.0.jar").getPath());
        vm.detach();
    }

    @Test
    public void testAddContent() throws Exception {
        generator.addContent(file);
        final Variable variable = file.getVariables().get(0);
        assertTrue(variable.read().getSize() > 0);
    }

    @Test
    public void testCreateOriginArray() throws Exception {
        final org.esa.cci.sst.data.Variable variable = new org.esa.cci.sst.data.Variable();
        variable.setDimensions("ni nj");
        variable.setDimensionRoles("ni nj");
        int[] originArray = generator.createOriginArray(0, variable);
        assertEquals(3, originArray.length);

        variable.setDimensions("match_up ni nj");
        variable.setDimensionRoles("match_up ni nj");
        originArray = generator.createOriginArray(0, variable);
        assertEquals(3, originArray.length);

        variable.setDimensions("time");
        variable.setDimensionRoles("time");
        originArray = generator.createOriginArray(0, variable);
        assertEquals(2, originArray.length);

        variable.setDimensions("match_up time");
        variable.setDimensionRoles("match_up time");
        originArray = generator.createOriginArray(0, variable);
        assertEquals(2, originArray.length);

        variable.setDimensions("n ni nj");
        variable.setDimensionRoles("match_up ni nj");
        originArray = generator.createOriginArray(0, variable);
        assertEquals(3, originArray.length);

        variable.setDimensions("n ni nj");
        variable.setDimensionRoles("n ni nj");
        originArray = generator.createOriginArray(0, variable);
        assertEquals(4, originArray.length);
    }

    @After
    public void tearDown() throws Exception {
        file.close();
//        new File(TEST_FILE).delete();
    }
}
