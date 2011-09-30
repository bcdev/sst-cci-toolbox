package org.esa.cci.sst.regavg.regavg;

import org.esa.cci.sst.regavg.ProductType;
import org.esa.cci.sst.util.UTC;
import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;

/**
* @author Norman Fomferra
*/
public class ProductTypeTest {
    @Test
    public void testAT2() throws Exception {
        DateFormat format = UTC.getDateFormat("yyyy-MM-dd");
        Assert.assertEquals(format.parse("2002-01-12"), ProductType.ARC_L3U.getDate("AT2_AVG_3PAARC20020112_D_dN2b.nc.gz"));  // day/nadir
        Assert.assertEquals(format.parse("2002-04-16"), ProductType.ARC_L3U.getDate("AT2_AVG_3PAARC20020416_D_dN2b.nc.gz"));  // day/dual
        Assert.assertEquals(format.parse("2002-01-20"), ProductType.ARC_L3U.getDate("AT2_AVG_3PAARC20020120_D_nN2b.nc.gz"));  // night/nadir
    }
}