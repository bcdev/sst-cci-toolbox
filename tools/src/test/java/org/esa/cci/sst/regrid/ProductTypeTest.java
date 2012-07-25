package org.esa.cci.sst.regrid;

import org.esa.cci.sst.regrid.filetypes.CciL3CFileType;
import org.esa.cci.sst.regrid.filetypes.CciL3UFileType;
import org.esa.cci.sst.regrid.filetypes.CciL4FileType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Bettina Scholze
 */
public class ProductTypeTest {

    @Test
    public void testProductTypes() throws Exception {
        assertEquals(3, ProductType.values().length);

        assertSame(CciL3UFileType.INSTANCE, ProductType.CCI_L3U.getFileType());
        assertSame(CciL3CFileType.INSTANCE, ProductType.CCI_L3C.getFileType());
        assertSame(CciL4FileType.INSTANCE, ProductType.CCI_L4.getFileType());
    }
}
