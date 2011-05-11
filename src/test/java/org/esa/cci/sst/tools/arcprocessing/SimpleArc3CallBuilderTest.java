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
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class SimpleArc3CallBuilderTest {

    @Test
    public void testFullyConfiguredArc3Call() throws Exception {
        final Properties configuration = new Properties();
        final String sourceFile = getClass().getResource("empty_test.nc").getFile();
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE, sourceFile);
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_EXECUTABLE, "ARC3_FOR_AVHRR");
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_NWPFILE, "some_nwp.nc");
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_TARGETFILE, "target.nc");
        final SimpleArc3CallBuilder simpleArc3CallBuilder = new SimpleArc3CallBuilder(configuration);

        final String arc3Call = simpleArc3CallBuilder.createArc3Call();
        final StringBuilder builder = new StringBuilder();
        builder.append("scp ");
        builder.append(sourceFile);
        builder.append(" eddie.ecdf.ed.ac.uk:tmp/\n");
        builder.append("ssh eddie.ecdf.ed.ac.uk ");
        builder.append("./ARC3_FOR_AVHRR MDB.INP ");
        builder.append(sourceFile);
        builder.append(" some_nwp.nc target.nc");

        assertEquals(builder.toString(), arc3Call);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailingDefaultArc3Call() {
        // set no parameters
        new SimpleArc3CallBuilder(null).createArc3Call();
    }

    @Test
    public void testDefaultArc3Call() throws Exception {
        final Properties configuration = new Properties();
        final String sourceFile = getClass().getResource("empty_test.nc").getFile();
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE, sourceFile);
        final String arc3Call = new SimpleArc3CallBuilder(configuration).createArc3Call();
        final StringBuilder builder = new StringBuilder();
        builder.append("scp ");
        builder.append(sourceFile);
        builder.append(" eddie.ecdf.ed.ac.uk:tmp/\n");
        builder.append("ssh eddie.ecdf.ed.ac.uk ");
        builder.append("./MMD_SCREEN_Linux MDB.INP ");
        builder.append(sourceFile);
        builder.append(" test_nwp.nc ");
        builder.append(Arc3CallBuilder.getDefaultTargetFileName(sourceFile));

        assertEquals(builder.toString(), arc3Call);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailingReingestCall() throws Exception {
        new SimpleArc3CallBuilder(null).createReingestionCall();
    }

    @Test
    public void testDefaultReingestCall() throws Exception {
        final Properties configuration = new Properties();
        final String sourceFile = getClass().getResource("empty_test.nc").getFile();
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE, sourceFile);
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_PATTERN, "20000");
        final SimpleArc3CallBuilder simpleArc3CallBuilder = new SimpleArc3CallBuilder(configuration);
        final String reingestCall = simpleArc3CallBuilder.createReingestionCall();
        final StringBuilder builder = new StringBuilder();
        builder.append("ssh eddie.ecdf.ed.ac.uk ");
        final String targetFileName = Arc3CallBuilder.getDefaultTargetFileName(sourceFile);
        builder.append(String.format("bin/mmsreingestmmd.sh -Dmms.reingestion.filename=%s\n" +
                                     " -Dmms.reingestion.located=no \\\n" +
                                     " -Dmms.reingestion.sensor=ARC3 \\\n" +
                                     " -Dmms.reingestion.pattern=20000 \\\n" +
                                     " -c config/mms-config-eddie1.properties", targetFileName));


        assertEquals(builder.toString(), reingestCall);
    }

    @Test
    public void testCleanupCall() throws Exception {
        final Properties configuration = new Properties();
        final String sourceFile = getClass().getResource("empty_test.nc").getFile();
        configuration.setProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE, sourceFile);
        final SimpleArc3CallBuilder simpleArc3CallBuilder = new SimpleArc3CallBuilder(configuration);
        final String cleanupCall = simpleArc3CallBuilder.createCleanupCall("arc3CallScript", "reingestionCallScript", "cleanupScript");
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("ssh eddie.ecdf.ed.ac.uk rm arc3CallScript");
        resultBuilder.append("ssh eddie.ecdf.ed.ac.uk rm reingestionCallScript");
        resultBuilder.append("ssh eddie.ecdf.ed.ac.uk rm cleanupScript");
        assertEquals(resultBuilder.toString(), cleanupCall);
    }
}
