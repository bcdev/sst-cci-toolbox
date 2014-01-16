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

package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Storm
 */
public class AvhrrPodProductReaderPlugInTest {

    @Test
    @Ignore
    public void testGetDecodeQualification() throws Exception {

        // @todo 2 tb/** these test files are not checked in as resources - therefore I switched this test to ignore - tb 2014-01-16
        final AvhrrPodProductReaderPlugIn plugIn = new AvhrrPodProductReaderPlugIn();
        final DecodeQualification decodeQualification91 = plugIn.getDecodeQualification(getClass().getResource("NSS.GHRR.NH.D91246.S0013.E0154.B1514546.GC").getFile());
        final DecodeQualification decodeQualification94 = plugIn.getDecodeQualification(getClass().getResource("NSS.GHRR.ND.D94062.S0103.E0257.B1454446.GC").getFile());
        final DecodeQualification decodeQualification95 = plugIn.getDecodeQualification(getClass().getResource("NSS.GHRR.ND.D95095.S0050.E0244.B2020204.GC").getFile());
        final DecodeQualification decodeQualification96 = plugIn.getDecodeQualification(getClass().getResource("NSS.GHRR.NJ.D96153.S1331.E1514.B0732425.GC").getFile());
        final DecodeQualification decodeQualification10 = plugIn.getDecodeQualification(getClass().getResource("NSS.GHRR.NP.D10312.S0739.E0921.B0902324.GC").getFile());

        assertEquals(DecodeQualification.INTENDED, decodeQualification91);
        assertEquals(DecodeQualification.INTENDED, decodeQualification94);
        assertEquals(DecodeQualification.INTENDED, decodeQualification95);
        assertEquals(DecodeQualification.INTENDED, decodeQualification96);
        assertEquals(DecodeQualification.UNABLE, decodeQualification10);
    }
}
