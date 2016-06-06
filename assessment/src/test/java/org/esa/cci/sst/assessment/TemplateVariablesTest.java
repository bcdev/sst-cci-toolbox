package org.esa.cci.sst.assessment;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TemplateVariablesTest {

    private TemplateVariables variables;

    @Before
    public void setUp() {
        variables = new TemplateVariables();
    }

    @Test
    public void testCreation_emptyVariables() {
        // @todo 2 tb/tb add check for empiness 2016-06-06
    }

    @Test
    public void testLoad() throws IOException {
        final InputStream propertiesStream = TemplateVariablesTest.class.getResourceAsStream("pvir-template.properties");
        assertNotNull(propertiesStream);

        try {
            variables.load(propertiesStream);
            final Map<String, String> wordVariables = variables.getWordVariables();

            assertEquals(4, wordVariables.size());
        } finally {
            propertiesStream.close();
        }
    }
}
