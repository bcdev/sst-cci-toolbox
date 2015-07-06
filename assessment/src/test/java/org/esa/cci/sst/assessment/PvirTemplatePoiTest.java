package org.esa.cci.sst.assessment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PvirTemplatePoiTest {

    private PoiWordDocument document;
    private Properties properties;

    @Before
    public void setUp() throws Exception {

        document = new PoiWordDocument(CarTemplateTest.class.getResource("PVIR_appendix_example_v3.docx"));
        properties = new Properties();
        properties.load(CarTemplateTest.class.getResourceAsStream("pvir-template.properties"));

    }

    @After
    public void tearDown() throws Exception {
        final File targetFile = new File("pvir.docx");
        document.save(targetFile);

        if (targetFile.isFile()) {
            if (!targetFile.delete()) {
                fail("Unable to delete test file");
            }
        }
    }

    @Test
    public void testReplaceTextVariables() throws Exception {
        final String sensor = properties.getProperty("word.sensor");

        assertTrue(document.containsVariable("${word.sensor}"));
        document.replaceWithText("${word.sensor}", sensor);
        assertFalse(document.containsVariable("${word.sensor}"));

        final String insitu = properties.getProperty("word.insitu");

        assertTrue(document.containsVariable("${word.insitu}"));
        document.replaceWithText("${word.insitu}", insitu);
        assertFalse(document.containsVariable("${word.insitu}"));

        final String averaging = properties.getProperty("word.averaging");
        assertTrue(document.containsVariable("${word.averaging}"));
        document.replaceWithText("${word.averaging}", averaging);
        assertFalse(document.containsVariable("${word.averaging}"));

        final String depthOrSkin = properties.getProperty("word.depth_or_skin");
        assertTrue(document.containsVariable("${word.depth_or_skin}"));
        document.replaceWithText("${word.depth_or_skin}", depthOrSkin);
        assertFalse(document.containsVariable("${word.depth_or_skin}"));
    }
}
