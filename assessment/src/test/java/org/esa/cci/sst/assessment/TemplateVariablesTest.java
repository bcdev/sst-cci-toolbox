package org.esa.cci.sst.assessment;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.*;

public class TemplateVariablesTest {

    private TemplateVariables variables;

    @Before
    public void setUp() {
        variables = new TemplateVariables();
    }

    @Test
    public void testCreation_emptyVariables() {
        final Map<String, String> wordVariables = variables.getWordVariables();
        assertEquals(0, wordVariables.size());
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

    @Test
    public void testGetWordVariables() throws IOException {
        final String properties = "word.schnick=juchee\nword.schnack=blablabla";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.getBytes());

        variables.load(inputStream);

        final Map<String, String> wordVariables = variables.getWordVariables();
        assertEquals(2, wordVariables.size());

        assertEquals("juchee", wordVariables.get("word.schnick"));
        assertEquals("blablabla", wordVariables.get("word.schnack"));
    }

    @Test
    public void testGetWordVariables_withDefaults() throws IOException {
        final String properties = "word.schnick=juchee\nword.schnick.default=hurra\nword.schnack.default=bla_default";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.getBytes());

        variables.load(inputStream);

        final Map<String, String> wordVariables = variables.getWordVariables();
        assertEquals(2, wordVariables.size());

        assertEquals("juchee", wordVariables.get("word.schnick"));
        assertEquals("bla_default", wordVariables.get("word.schnack"));
    }

    @Test
    public void testIsDefaultProperty() {
        assertTrue(TemplateVariables.isDefaultProperty("the.property.default")) ;

        assertFalse(TemplateVariables.isDefaultProperty("the.property.scale")) ;
        assertFalse(TemplateVariables.isDefaultProperty("the.property")) ;
    }

    @Test
    public void testGetPropertyNameFromDefault() {
        assertEquals("the.property", TemplateVariables.getPropertyNameFromDefault("the.property.default"));

        assertEquals("the.property.scale", TemplateVariables.getPropertyNameFromDefault("the.property.scale"));
        assertEquals("the.property", TemplateVariables.getPropertyNameFromDefault("the.property"));
    }
}
