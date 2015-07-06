package org.esa.cci.sst.assessment;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;

import static org.junit.Assert.fail;

public class PoiWordDocumentTest {

    private static PoiWordDocument wordDocument;

    @BeforeClass
    public static void setUp() {
        wordDocument = new PoiWordDocument();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        final File targetFile = new File("test.docx");
        wordDocument.save(targetFile);

        if (targetFile.isFile()) {
            if (!targetFile.delete()) {
                fail("Unable to delete word file");
            }
        }
    }
}
