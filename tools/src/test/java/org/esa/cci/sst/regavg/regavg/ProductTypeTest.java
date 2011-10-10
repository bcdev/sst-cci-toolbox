package org.esa.cci.sst.regavg.regavg;

import org.esa.cci.sst.regavg.ProductType;
import org.esa.cci.sst.util.UTC;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.text.DateFormat;

/**
* @author Norman Fomferra
*/
public class ProductTypeTest {
    @Test
    public void test_ARC_L3U() throws Exception {
        DateFormat format = UTC.getDateFormat("yyyy-MM-dd");
        Assert.assertEquals(format.parse("2002-01-12"), ProductType.ARC_L3U.getDate(new File("AT2_AVG_3PAARC20020112_D_dN2b.nc.gz")));  // day/nadir
        Assert.assertEquals(format.parse("2002-04-16"), ProductType.ARC_L3U.getDate(new File("AT2_AVG_3PAARC20020416_D_dN2b.nc.gz")));  // day/dual
        Assert.assertEquals(format.parse("2002-01-20"), ProductType.ARC_L3U.getDate(new File("AT2_AVG_3PAARC20020120_D_nN2b.nc.gz")));  // night/nadir
        Assert.assertEquals(format.parse("2002-09-15"), ProductType.ARC_L3U.getDate(new File("ATS_AVG_3PAARC20020915_D_nD3b.nc.gz")));  // night/dual

    }
}