package org.esa.cci.sst.product;

import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * @author Norman Fomferra
 */
public class ProductTypeTest {

    @Test
    public void testFileTypes() throws Exception {
        assertSame(CciL2FileType.INSTANCE, ProductType.CCI_L2P.getFileType());
        assertSame(ArcL3FileType.INSTANCE, ProductType.ARC_L3U.getFileType());
        assertSame(CciL3FileType.INSTANCE, ProductType.CCI_L3U.getFileType());
        assertSame(CciL3FileType.INSTANCE, ProductType.CCI_L3C.getFileType());
        assertSame(CciL4FileType.INSTANCE, ProductType.CCI_L4.getFileType());
    }
}