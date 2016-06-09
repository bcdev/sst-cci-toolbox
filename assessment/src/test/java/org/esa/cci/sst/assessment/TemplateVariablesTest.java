package org.esa.cci.sst.assessment;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.*;

@SuppressWarnings("ThrowFromFinallyBlock")
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

        final Map<String, String> paragraphVariables = variables.getParagraphVariables();
        assertEquals(0, paragraphVariables.size());

        assertNull(variables.getFiguresDirectory());
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
        final String properties = "word.schnick=juchee\n" +
                "word.schnack=blablabla\n" +
                "something.else=weDoNotSeeThis";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.getBytes());

        variables.load(inputStream);

        final Map<String, String> wordVariables = variables.getWordVariables();
        assertEquals(2, wordVariables.size());

        assertEquals("juchee", wordVariables.get("word.schnick"));
        assertEquals("blablabla", wordVariables.get("word.schnack"));
    }

    @Test
    public void testGetWordVariables_withDefaults() throws IOException {
        final String properties = "word.schnick=juchee\n" +
                "word.schnick.default=hurra\n" +
                "word.schnack.default=bla_default";
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

    @Test
    public void testGetParagraphVariables() throws IOException {
        final String properties = "paragraph.full=this is a complete paragraph\n" +
                "strange.key=willBeSkipped\n" +
                "comment.schnack=me too";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.getBytes());

        variables.load(inputStream);

        final Map<String, String> paragraphVariables = variables.getParagraphVariables();
        assertEquals(2, paragraphVariables.size());

        assertEquals("this is a complete paragraph", paragraphVariables.get("paragraph.full"));
        assertEquals("me too", paragraphVariables.get("comment.schnack"));
    }

    @Test
    public void testGetParagraphVariables_withDefault() throws IOException {
        final String properties = "paragraph.full=this is a complete paragraph\n" +
                "sparagraph.full.default=willBeSkipped\n" +
                "comment.schnack.default=willBeDisplayed";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.getBytes());

        variables.load(inputStream);

        final Map<String, String> paragraphVariables = variables.getParagraphVariables();
        assertEquals(2, paragraphVariables.size());

        assertEquals("this is a complete paragraph", paragraphVariables.get("paragraph.full"));
        assertEquals("willBeDisplayed", paragraphVariables.get("comment.schnack"));
    }

    @Test
    public void testIsParagraphProperty() {
        assertFalse(TemplateVariables.isParagraphProperty("image.some"));
        assertFalse(TemplateVariables.isParagraphProperty("word.written"));

        assertTrue(TemplateVariables.isParagraphProperty("paragraph.really"));
        assertTrue(TemplateVariables.isParagraphProperty("comment.really"));
    }

    @Test
    public void testIsFigureProperty() {
        assertFalse(TemplateVariables.isFigureProperty("image"));
        assertFalse(TemplateVariables.isFigureProperty("figure.bla.scale"));

        assertTrue(TemplateVariables.isFigureProperty("figure.really"));
    }

    @Test
    public void testGetFiguresDirectory() throws IOException {
        final String properties = "figures.directory=where_it_is\n" +
                "sparagraph.full.default=willBeSkipped";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.getBytes());

        variables.load(inputStream);

        assertEquals("where_it_is", variables.getFiguresDirectory());
    }

    @Test
    public void testGetFigureVariables() throws IOException {
        final String properties = "figure.one=a_valid_figure\n" +
                "strange.key=somethingElse\n" +
                "figure.second=another_figure";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.getBytes());

        variables.load(inputStream);

        final Map<String, String> figureVariables = variables.getFigureVariables();
        assertEquals(2, figureVariables.size());

        assertEquals("a_valid_figure", figureVariables.get("figure.one"));
        assertEquals("another_figure", figureVariables.get("figure.second"));
    }
}
