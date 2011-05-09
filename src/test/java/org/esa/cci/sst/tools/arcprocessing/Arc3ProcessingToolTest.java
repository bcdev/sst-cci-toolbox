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

package org.esa.cci.sst.tools.arcprocessing;

import org.esa.cci.sst.tools.Constants;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class Arc3ProcessingToolTest {

    private Arc3ProcessingTool arc3ProcessingTool;

    @Before
    public void setUp() throws Exception {
        arc3ProcessingTool = new Arc3ProcessingTool();
    }

    @Test
    public void testFullyConfiguredArc3Call() throws Exception {
        final Properties configuration = arc3ProcessingTool.getConfiguration();
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE, "sourceMmd.nc");
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_EXECUTABLE, "ARC3_FOR_AVHRR");
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_NWPFILE, "some_nwp.nc");
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_TARGETFILE, "target.nc");
        final String arc3Call = arc3ProcessingTool.createArc3Call();
        final StringBuilder builder = new StringBuilder();
        builder.append("scp sourceMmd.nc eddie.ecdf.ed.ac.uk:tmp/\n");
        builder.append("ssh eddie.ecdf.ed.ac.uk ");
        builder.append("./ARC3_FOR_AVHRR MDB.INP sourceMmd.nc some_nwp.nc target.nc");

        assertEquals(builder.toString(), arc3Call);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailingDefaultArc3Call() {
        // set no parameters
        arc3ProcessingTool.createArc3Call();
    }

    @Test
    public void testDefaultArc3Call() {
        arc3ProcessingTool.getConfiguration().setProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE, "mmd.nc");
        final String arc3Call = arc3ProcessingTool.createArc3Call();
        final StringBuilder builder = new StringBuilder();
        builder.append("scp mmd.nc eddie.ecdf.ed.ac.uk:tmp/\n");
        builder.append("ssh eddie.ecdf.ed.ac.uk ");
        builder.append("./MMD_SCREEN_Linux MDB.INP mmd.nc test_nwp.nc mmd_ARC3.nc");

        assertEquals(builder.toString(), arc3Call);
    }



    @Test(expected = IllegalStateException.class)
    public void testFailingReingestCall() throws Exception {
        arc3ProcessingTool.createReingestionCall();
    }

    @Test
    public void testDefaultReingestCall() throws Exception {
        arc3ProcessingTool.getConfiguration().setProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE, "mmd.nc");
        arc3ProcessingTool.getConfiguration().setProperty(Constants.PROPERTY_MMS_ARC3_PATTERN, "20000");
        final String reingestCall = arc3ProcessingTool.createReingestionCall();
        final StringBuilder builder = new StringBuilder();
        builder.append("ssh eddie.ecdf.ed.ac.uk ");
        final String targetFileName = arc3ProcessingTool.getDefaultTargetFileName("mmd.nc");
        builder.append(String.format("bin/mmsreingestmmd.sh -Dmms.reingestion.filename=%s\n" +
                                      " -Dmms.reingestion.located=no \\\n" +
                                      " -Dmms.reingestion.sensor=ARC3 \\\n" +
                                      " -Dmms.reingestion.pattern=20000 \\\n" +
                                      " -c config/mms-config-eddie1.properties", targetFileName));


        assertEquals(builder.toString(), reingestCall);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailingCleanupCall() throws Exception {
        arc3ProcessingTool.createCleanupCall();
    }

    @Test
    public void testCleanupCall() throws Exception {
        arc3ProcessingTool.getConfiguration().setProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE, "mmd.nc");
        final String cleanupCall = arc3ProcessingTool.createCleanupCall();
        assertEquals("ssh eddie.ecdf.ed.ac.uk rm mmd.nc", cleanupCall);
    }

    @Test
    public void testGetDefaultTargetFileName() throws Exception {
        final String defaultTargetFileName = arc3ProcessingTool.getDefaultTargetFileName("mmd.nc");
        assertEquals("mmd_ARC3.nc", defaultTargetFileName);
    }
}
