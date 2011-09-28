package org.esa.cci.sst.regavg.regavg;

import org.esa.cci.sst.regavg.RegionMaskList;
import org.esa.cci.sst.regavg.RegionMask;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class RegionListTest {

    @Test
    public void testGlobal() throws Exception {
        RegionMaskList regionMaskList = RegionMaskList.parse("Global=-180,90,180,-90");
        assertEquals(1, regionMaskList.size());
        RegionMask global = regionMaskList.get(0);
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
        RegionMaskList regionMaskList = RegionMaskList.parse("NW=-180,90,0,0;SE=0,0,180,-90");
        assertEquals(2, regionMaskList.size());
        RegionMask nw = regionMaskList.get(0);
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
        RegionMask se = regionMaskList.get(1);
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
        RegionMaskList regionMaskList = RegionMaskList.parse("Pacific=170,10,-170,-10");
        assertEquals(1, regionMaskList.size());
        RegionMask pacific = regionMaskList.get(0);
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
