package org.esa.cci.sst.common.file;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * {@author Bettina Scholze}
 * Date: 16.10.12 11:20
 */
public class CciL2FileTypeTest {
    FileType fileType = CciL2PFileType.INSTANCE;

    @Test
    public void testGetFileNameRegex() throws Exception {
        String exp = "\\d{14}-ESACCI-L2P_GHRSST-SST((skin)|(subskin)|(depth)|(fnd))[-]((ATSR1)|(ATSR2)|(AATSR)|(AMSRE)|(SEVIRI_SST)|(TMI))[-]((LT)|(DM))-v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";
        assertEquals(exp, fileType.getFilenameRegex());
    }
}
