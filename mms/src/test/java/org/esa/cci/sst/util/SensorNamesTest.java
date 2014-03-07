package org.esa.cci.sst.util;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SensorNamesTest {

    @Test
    public void testIsOrbitName() throws Exception {
        assertTrue(SensorNames.isOrbitName("atsr_orb.1"));
        assertTrue(SensorNames.isOrbitName("atsr_orb.2"));
        assertTrue(SensorNames.isOrbitName("atsr_orb.3"));

        assertTrue(SensorNames.isOrbitName("avhrr_orb.n10"));
        assertTrue(SensorNames.isOrbitName("avhrr_orb.n11"));
        assertTrue(SensorNames.isOrbitName("avhrr_orb.n12"));
        assertTrue(SensorNames.isOrbitName("avhrr_orb.n13"));
        assertTrue(SensorNames.isOrbitName("avhrr_orb.n14"));
        assertTrue(SensorNames.isOrbitName("avhrr_orb.n15"));
        assertTrue(SensorNames.isOrbitName("avhrr_orb.n16"));
        assertTrue(SensorNames.isOrbitName("avhrr_orb.n17"));
        assertTrue(SensorNames.isOrbitName("avhrr_orb.n18"));
        assertTrue(SensorNames.isOrbitName("avhrr_orb.n19"));
        assertTrue(SensorNames.isOrbitName("avhrr_orb.m02"));

        assertFalse(SensorNames.isOrbitName("aai_orb"));
        assertFalse(SensorNames.isOrbitName("seaice_orb"));
    }

    @Test
    public void testIsStandardName() throws Exception {
        assertTrue(SensorNames.isStandardName("atsr.1"));
        assertTrue(SensorNames.isStandardName("atsr.2"));
        assertTrue(SensorNames.isStandardName("atsr.3"));

        assertTrue(SensorNames.isStandardName("avhrr.n10"));
        assertTrue(SensorNames.isStandardName("avhrr.n11"));
        assertTrue(SensorNames.isStandardName("avhrr.n12"));
        assertTrue(SensorNames.isStandardName("avhrr.n13"));
        assertTrue(SensorNames.isStandardName("avhrr.n14"));
        assertTrue(SensorNames.isStandardName("avhrr.n15"));
        assertTrue(SensorNames.isStandardName("avhrr.n16"));
        assertTrue(SensorNames.isStandardName("avhrr.n17"));
        assertTrue(SensorNames.isStandardName("avhrr.n18"));
        assertTrue(SensorNames.isStandardName("avhrr.n19"));
        assertTrue(SensorNames.isStandardName("avhrr.m02"));

        assertFalse(SensorNames.isStandardName("atsr_orb.1"));
        assertFalse(SensorNames.isStandardName("atsr_orb.2"));
        assertFalse(SensorNames.isStandardName("atsr_orb.3"));

        assertFalse(SensorNames.isStandardName("avhrr_orb.n10"));
        assertFalse(SensorNames.isStandardName("avhrr_orb.n11"));
        assertFalse(SensorNames.isStandardName("avhrr_orb.n12"));
        assertFalse(SensorNames.isStandardName("avhrr_orb.n13"));
        assertFalse(SensorNames.isStandardName("avhrr_orb.n14"));
        assertFalse(SensorNames.isStandardName("avhrr_orb.n15"));
        assertFalse(SensorNames.isStandardName("avhrr_orb.n16"));
        assertFalse(SensorNames.isStandardName("avhrr_orb.n17"));
        assertFalse(SensorNames.isStandardName("avhrr_orb.n18"));
        assertFalse(SensorNames.isStandardName("avhrr_orb.n19"));
        assertFalse(SensorNames.isStandardName("avhrr_orb.m02"));

        assertFalse(SensorNames.isStandardName("aai"));
        assertFalse(SensorNames.isStandardName("seaice"));
    }

    @Test
    public void testEnsureOrbitName() throws Exception {
        String sensorName;

        sensorName = "atsr_orb.1";
        assertSame(sensorName, SensorNames.ensureOrbitName(sensorName));

        sensorName = "atsr_orb.2";
        assertSame(sensorName, SensorNames.ensureOrbitName(sensorName));

        sensorName = "atsr_orb.3";
        assertSame(sensorName, SensorNames.ensureOrbitName(sensorName));

        assertEquals("atsr_orb.1", SensorNames.ensureOrbitName("atsr.1"));
        assertEquals("atsr_orb.2", SensorNames.ensureOrbitName("atsr.2"));
        assertEquals("atsr_orb.3", SensorNames.ensureOrbitName("atsr.3"));

        assertEquals("avhrr_orb.n10", SensorNames.ensureOrbitName("avhrr.n10"));
        assertEquals("avhrr_orb.m02", SensorNames.ensureOrbitName("avhrr.m02"));

        try {
            SensorNames.ensureOrbitName("aai");
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            SensorNames.ensureOrbitName("seaice");
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testEnsureStandardName() throws Exception {
        String sensorName;

        sensorName = "atsr.1";
        assertSame(sensorName, SensorNames.ensureStandardName(sensorName));

        sensorName = "atsr.2";
        assertSame(sensorName, SensorNames.ensureStandardName(sensorName));

        sensorName = "atsr.3";
        assertSame(sensorName, SensorNames.ensureStandardName(sensorName));

        assertEquals("atsr.1", SensorNames.ensureStandardName("atsr_orb.1"));
        assertEquals("atsr.2", SensorNames.ensureStandardName("atsr_orb.2"));
        assertEquals("atsr.3", SensorNames.ensureStandardName("atsr_orb.3"));

        assertEquals("avhrr.n10", SensorNames.ensureStandardName("avhrr_orb.n10"));
        assertEquals("avhrr.m02", SensorNames.ensureStandardName("avhrr_orb.m02"));

        try {
            SensorNames.ensureStandardName("aai");
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            SensorNames.ensureStandardName("seaice");
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

}
