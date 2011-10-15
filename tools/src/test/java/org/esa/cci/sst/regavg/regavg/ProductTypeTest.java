package org.esa.cci.sst.regavg.regavg;

import org.esa.cci.sst.regavg.ProductType;
import org.esa.cci.sst.regavg.filetypes.*;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * @author Norman Fomferra
 */
public class ProductTypeTest {
    @Test
    public void testFileTypes() throws Exception {
        assertSame(ArcL2PFileType.INSTANCE, ProductType.ARC_L2P.getFileType());
        assertSame(ArcL3UFileType.INSTANCE, ProductType.ARC_L3U.getFileType());
        assertSame(CciL3UFileType.INSTANCE, ProductType.CCI_L3U.getFileType());
        assertSame(CciL3CFileType.INSTANCE, ProductType.CCI_L3C.getFileType());
        assertSame(CciL4FileType.INSTANCE, ProductType.CCI_L4.getFileType());
    }
}