package org.esa.cci.sst.assessment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DoubleVariableTest {

    private PoiWordDocument document;
    private Properties properties;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        document = new PoiWordDocument(DoubleVariableTest.class.getResource("double_variable_template.docx"));
        properties = new Properties();
        properties.load(CarTemplateTest.class.getResourceAsStream("double_variable.properties"));
    }

    @After
    public void tearDown() throws Exception {
        final File targetFile = new File("double_variable.docx");
        document.save(targetFile);

        if (targetFile.isFile()) {
            if (!targetFile.delete()) {
                fail("Unable to delete test file");
            }
        }
    }

    @Test
    public void testReplaceDoubleVariable() throws Exception {
        final String variable = properties.getProperty("word.multiple");

        assertTrue(document.containsVariable("${word.multiple}"));
        document.replaceWithText("${word.multiple}", variable);
        assertFalse(document.containsVariable("${word.multiple}"));
    }

}
