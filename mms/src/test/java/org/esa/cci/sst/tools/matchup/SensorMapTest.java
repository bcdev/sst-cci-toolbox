package org.esa.cci.sst.tools.matchup;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SensorMapTest {

    @Test
    public void testIdForName() {
        assertEquals(3, SensorMap.idForName("history"));
        assertEquals(24, SensorMap.idForName("orb_avhrr.n16"));
        assertEquals(56, SensorMap.idForName("iasi.m02"));
    }

    @Test
    public void testIdForName_throwsOnIllegalSensorName() {
        try {
            SensorMap.idForName("Tinnef");
            fail("IllegalArgumentException expected");
        }   catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testNameForId() {
        assertEquals("orb_atsr.1", SensorMap.nameForId(7));
        assertEquals("amsre", SensorMap.nameForId(29));
        assertEquals("avhrr.n18", SensorMap.nameForId(47));
    }

    @Test
    public void testNameForId_throwsOnInvalidId() {
        System.out.println(new Date().getTime());
        try {
            SensorMap.nameForId(-17);
            fail("IllegalArgumentException expected");
        }   catch (IllegalArgumentException expected) {
        }
    }
}
