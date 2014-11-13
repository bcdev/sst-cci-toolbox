package org.esa.cci.sst.common.file;

import org.esa.cci.sst.file.FileType;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CciL2FileTypeTest {
    FileType fileType = CciL2FileType.INSTANCE;

    @Test
    public void testFilenameRegex_AVHRR() throws Exception {
        final String filename = "20100701012400-ESACCI-L2P_GHRSST-SSTskin-AVHRR18_G-LT-v02.0-fv01.0.nc";

        assertTrue(filename.matches(fileType.getFilenameRegex()));
    }
}
