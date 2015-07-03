package org.esa.cci.sst.assessment;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PvirTemplateTest {

    private static Properties properties;
    private static WordDocument wordDocument;
    private static File dataDir;

    @BeforeClass
    public static void setUp() throws Exception {
        wordDocument = new WordDocument(CarTemplateTest.class.getResource("PVIR_appendix_example_v3.docx"));

        properties = new Properties();
        properties.load(CarTemplateTest.class.getResourceAsStream("pvir-template.properties"));

        dataDir = new File(System.getProperty("user.home"), "scratch/pvir/figures");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        final File targetFile = new File("pvir.docx");
        wordDocument.save(targetFile);

        if (targetFile.isFile()) {
            if (!targetFile.delete()) {
                fail("Unable to delete test file");
            }
        }
    }

    @Test
    public void testReplaceTextVariables() throws Exception {
        final String sensor = properties.getProperty("word.sensor");

        assertNotNull(wordDocument.findVariable("${word.sensor}"));
        wordDocument.replaceWithText("word.sensor", sensor);
        assertNull(wordDocument.findVariable("${word.sensor}"));

        // @todo 1 tb/tb continue here 2015-07-02
//        final String insitu = properties.getProperty("word.insitu");
//
//        assertNotNull(wordDocument.findVariable("${word.insitu}"));
//        wordDocument.replaceWithText("word.insitu", insitu);
//        assertNull(wordDocument.findVariable("${word.insitu}"));
    }
}
