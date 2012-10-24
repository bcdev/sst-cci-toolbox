package org.esa.cci.sst.common;

import org.esa.cci.sst.common.file.ArcL3UFileType;
import org.esa.cci.sst.common.file.CciL2PFileType;
import org.esa.cci.sst.common.file.CciL3FileType;
import org.esa.cci.sst.common.file.CciL4FileType;
import org.esa.cci.sst.util.ProductType;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * @author Norman Fomferra
 */
public class ProductTypeTest {
    @Test
    public void testFileTypes() throws Exception {
        assertSame(CciL2PFileType.INSTANCE, ProductType.CCI_L2P.getFileType());
        assertSame(ArcL3UFileType.INSTANCE, ProductType.ARC_L3U.getFileType());
        assertSame(CciL3FileType.INSTANCE, ProductType.CCI_L3U.getFileType());
        assertSame(CciL3FileType.INSTANCE, ProductType.CCI_L3C.getFileType());
        assertSame(CciL4FileType.INSTANCE, ProductType.CCI_L4.getFileType());
    }
}