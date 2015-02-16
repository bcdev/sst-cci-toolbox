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
        assertTrue(SensorNames.isOrbitName("orb_atsr.1"));
        assertTrue(SensorNames.isOrbitName("orb_atsr.2"));
        assertTrue(SensorNames.isOrbitName("orb_atsr.3"));

        assertTrue(SensorNames.isOrbitName("orb_avhrr.n10"));
        assertTrue(SensorNames.isOrbitName("orb_avhrr.n11"));
        assertTrue(SensorNames.isOrbitName("orb_avhrr.n12"));
        assertTrue(SensorNames.isOrbitName("orb_avhrr.n13"));
        assertTrue(SensorNames.isOrbitName("orb_avhrr.n14"));
        assertTrue(SensorNames.isOrbitName("orb_avhrr.n15"));
        assertTrue(SensorNames.isOrbitName("orb_avhrr.n16"));
        assertTrue(SensorNames.isOrbitName("orb_avhrr.n17"));
        assertTrue(SensorNames.isOrbitName("orb_avhrr.n18"));
        assertTrue(SensorNames.isOrbitName("orb_avhrr.n19"));
        assertTrue(SensorNames.isOrbitName("orb_avhrr.m01"));
        assertTrue(SensorNames.isOrbitName("orb_avhrr.m02"));

        assertTrue(SensorNames.isOrbitName("orb_avhrr.m01f"));
        assertTrue(SensorNames.isOrbitName("orb_avhrr.m02f"));

        assertFalse(SensorNames.isOrbitName("orb_aai"));
        assertFalse(SensorNames.isOrbitName("orb_seaice"));
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
        assertTrue(SensorNames.isStandardName("avhrr.m01"));
        assertTrue(SensorNames.isStandardName("avhrr.m02"));

        assertTrue(SensorNames.isStandardName("avhrr.m01f"));
        assertTrue(SensorNames.isStandardName("avhrr.m02f"));

        assertFalse(SensorNames.isStandardName("orb_atsr.1"));
        assertFalse(SensorNames.isStandardName("orb_atsr.2"));
        assertFalse(SensorNames.isStandardName("orb_atsr.3"));

        assertFalse(SensorNames.isStandardName("orb_avhrr.n10"));
        assertFalse(SensorNames.isStandardName("orb_avhrr.n11"));
        assertFalse(SensorNames.isStandardName("orb_avhrr.n12"));
        assertFalse(SensorNames.isStandardName("orb_avhrr.n13"));
        assertFalse(SensorNames.isStandardName("orb_avhrr.n14"));
        assertFalse(SensorNames.isStandardName("orb_avhrr.n15"));
        assertFalse(SensorNames.isStandardName("orb_avhrr.n16"));
        assertFalse(SensorNames.isStandardName("orb_avhrr.n17"));
        assertFalse(SensorNames.isStandardName("orb_avhrr.n18"));
        assertFalse(SensorNames.isStandardName("orb_avhrr.n19"));
        assertFalse(SensorNames.isStandardName("orb_avhrr.m01"));
        assertFalse(SensorNames.isStandardName("orb_avhrr.m02"));

        assertFalse(SensorNames.isStandardName("orb_avhrr.m01f"));
        assertFalse(SensorNames.isStandardName("orb_avhrr.m02f"));

        assertFalse(SensorNames.isStandardName("aai"));
        assertFalse(SensorNames.isStandardName("seaice"));
    }

    @Test
    public void testGetOrbitName() throws Exception {
        String sensorName = "orb_atsr.1";
        assertSame(sensorName, SensorNames.getOrbitName(sensorName));

        sensorName = "orb_atsr.2";
        assertSame(sensorName, SensorNames.getOrbitName(sensorName));

        sensorName = "orb_atsr.3";
        assertSame(sensorName, SensorNames.getOrbitName(sensorName));

        assertEquals("orb_atsr.1", SensorNames.getOrbitName("atsr.1"));
        assertEquals("orb_atsr.2", SensorNames.getOrbitName("atsr.2"));
        assertEquals("orb_atsr.3", SensorNames.getOrbitName("atsr.3"));

        assertEquals("orb_avhrr.n10", SensorNames.getOrbitName("avhrr.n10"));
        assertEquals("orb_avhrr.m01", SensorNames.getOrbitName("avhrr.m01"));
        assertEquals("orb_avhrr.m02", SensorNames.getOrbitName("avhrr.m02"));

        assertEquals("orb_avhrr.m01f", SensorNames.getOrbitName("avhrr.m01f"));
        assertEquals("orb_avhrr.m02f", SensorNames.getOrbitName("avhrr.m02f"));

        try {
            SensorNames.getOrbitName("aai");
            fail();
        } catch (IllegalArgumentException expected) {
            //
        }

        try {
            SensorNames.getOrbitName("seaice");
            fail();
        } catch (IllegalArgumentException expected) {
            //
        }
    }

    @Test
    public void testGetStandardName() throws Exception {
        String sensorName;

        sensorName = "atsr.1";
        assertSame(sensorName, SensorNames.getStandardName(sensorName));

        sensorName = "atsr.2";
        assertSame(sensorName, SensorNames.getStandardName(sensorName));

        sensorName = "atsr.3";
        assertSame(sensorName, SensorNames.getStandardName(sensorName));

        assertEquals("atsr.1", SensorNames.getStandardName("orb_atsr.1"));
        assertEquals("atsr.2", SensorNames.getStandardName("orb_atsr.2"));
        assertEquals("atsr.3", SensorNames.getStandardName("orb_atsr.3"));

        assertEquals("avhrr.n10", SensorNames.getStandardName("orb_avhrr.n10"));
        assertEquals("avhrr.m01", SensorNames.getStandardName("orb_avhrr.m01"));
        assertEquals("avhrr.m02", SensorNames.getStandardName("orb_avhrr.m02"));

        assertEquals("avhrr.m01f", SensorNames.getStandardName("orb_avhrr.m01f"));
        assertEquals("avhrr.m02f", SensorNames.getStandardName("orb_avhrr.m02f"));

        try {
            SensorNames.getStandardName("aai");
            fail();
        } catch (IllegalArgumentException expected) {
            //
        }

        try {
            SensorNames.getStandardName("seaice");
            fail();
        } catch (IllegalArgumentException expected) {
            //
        }
    }

    @Test
    public void testGetBasename() throws Exception {
        assertEquals("atsr", SensorNames.getBasename("atsr.1"));
        assertEquals("atsr", SensorNames.getBasename("atsr.2"));
        assertEquals("atsr", SensorNames.getBasename("atsr.3"));
        assertEquals("atsr", SensorNames.getBasename("orb_atsr.1"));
        assertEquals("atsr", SensorNames.getBasename("orb_atsr.2"));
        assertEquals("atsr", SensorNames.getBasename("orb_atsr.3"));

        assertEquals("avhrr", SensorNames.getBasename("orb_avhrr.n10"));
        assertEquals("avhrr", SensorNames.getBasename("orb_avhrr.m01"));
        assertEquals("avhrr", SensorNames.getBasename("orb_avhrr.m02"));

        assertEquals("avhrr", SensorNames.getBasename("orb_avhrr.m01f"));
        assertEquals("avhrr", SensorNames.getBasename("orb_avhrr.m02f"));

        try {
            SensorNames.getBasename("aai");
            fail();
        } catch (IllegalArgumentException expected) {
            //
        }

        try {
            SensorNames.getBasename("seaice");
            fail();
        } catch (IllegalArgumentException expected) {
            //
        }
    }

    @Test
    public void testGetDimensionNameX() throws Exception {
        assertEquals("avhrr.nx", SensorNames.getDimensionNameX("avhrr.n10"));
        assertEquals("avhrr.nx", SensorNames.getDimensionNameX("avhrr.n11"));
        assertEquals("avhrr.nx", SensorNames.getDimensionNameX("avhrr.n12"));
    }

    @Test
    public void testGetDimensionNameY() throws Exception {
        assertEquals("avhrr.ny", SensorNames.getDimensionNameY("avhrr.n10"));
        assertEquals("avhrr.ny", SensorNames.getDimensionNameY("avhrr.n11"));
        assertEquals("avhrr.ny", SensorNames.getDimensionNameY("avhrr.n12"));
    }
}
