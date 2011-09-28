package org.esa.cci.sst.regavg.regavg;

import org.esa.cci.sst.regavg.RegionList;
import org.esa.cci.sst.regavg.RegionMask;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class RegionListTest {

    @Test
    public void testGlobal() throws Exception {
        RegionList regionList = RegionList.parse("Global=-180,90,180,-90");
        assertEquals(1, regionList.size());
        RegionMask global = regionList.get(0);
        assertEquals("Global", global.getName());
        assertEquals(true, global.getSample(-180, 90));
        assertEquals(true, global.getSample(0, 90));
        assertEquals(true, global.getSample(+180, 90));
        assertEquals(true, global.getSample(-180, 0));
        assertEquals(true, global.getSample(0, 0));
        assertEquals(true, global.getSample(+180, 0));
        assertEquals(true, global.getSample(-180, -90));
        assertEquals(true, global.getSample(0, -90));
        assertEquals(true, global.getSample(+180, -90));
    }

    @Test
    public void testTwo() throws Exception {
        RegionList regionList = RegionList.parse("NW=-180,90,0,0;SE=0,0,180,-90");
        assertEquals(2, regionList.size());
        RegionMask nw = regionList.get(0);
        assertEquals("NW", nw.getName());
        assertEquals(true, nw.getSample(-180, 90));
        assertEquals(false, nw.getSample(0, 90));
        assertEquals(false, nw.getSample(+180, 90));
        assertEquals(false, nw.getSample(-180, 0));
        assertEquals(false, nw.getSample(0, 0));
        assertEquals(false, nw.getSample(+180, 0));
        assertEquals(false, nw.getSample(-180, -90));
        assertEquals(false, nw.getSample(0, -90));
        assertEquals(false, nw.getSample(+180, -90));
        RegionMask se = regionList.get(1);
        assertEquals("SE", se.getName());
        assertEquals(false, se.getSample(-180, 90));
        assertEquals(false, se.getSample(0, 90));
        assertEquals(false, se.getSample(+180, 90));
        assertEquals(false, se.getSample(-180, 0));
        assertEquals(true, se.getSample(0, 0));
        assertEquals(true, se.getSample(+180, 0));
        assertEquals(false, se.getSample(-180, -90));
        assertEquals(true, se.getSample(0, -90));
        assertEquals(true, se.getSample(+180, -90));
    }

    @Test
    public void testAntiMerdian() throws Exception {
        RegionList regionList = RegionList.parse("Pacific=170,10,-170,-10");
        assertEquals(1, regionList.size());
        RegionMask pacific = regionList.get(0);
        assertEquals("Pacific", pacific.getName());
        assertEquals(true, pacific.getSample(-180, 0));
        assertEquals(true, pacific.getSample(-175, 0));
        assertEquals(false, pacific.getSample(-170, 0));
        assertEquals(false, pacific.getSample(-160, 0));
        assertEquals(false, pacific.getSample(-90, 0));
        assertEquals(false, pacific.getSample(-0, 0));
        assertEquals(false, pacific.getSample(90, 0));
        assertEquals(false, pacific.getSample(160, 0));
        assertEquals(true, pacific.getSample(170, 0));
        assertEquals(true, pacific.getSample(175, 0));
        assertEquals(true, pacific.getSample(180, 0));
    }
}
