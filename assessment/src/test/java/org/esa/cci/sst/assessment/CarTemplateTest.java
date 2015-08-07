package org.esa.cci.sst.assessment;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.esa.beam.util.io.WildcardMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CarTemplateTest {

    private File dataDir;
    private PoiWordDocument document;
    private Properties properties;

    @Before
    public void setUp() throws Exception {
        document = new PoiWordDocument(CarTemplateTest.class.getResource("car-template.docx"));
        properties = new Properties();
        properties.load(CarTemplateTest.class.getResourceAsStream("car-template.properties"));

        dataDir = new File(System.getProperty("user.home"), "scratch/car/figures");
    }

    @After
    public void tearDown() throws Exception {
        final File targetFile = new File("car.docx");
        document.save(targetFile);

        if (targetFile.isFile()) {
            if (!targetFile.delete()) {
                fail("Unable to delete test file");
            }
        }
    }

    @Test
    public void testReplaceComments() throws Exception {
        String comment = properties.getProperty("comment.Figure_2");
        assertTrue(document.containsVariable("${comment.Figure_2}"));
        document.replaceParagraphText("${comment.Figure_2}", comment);
        assertFalse(document.containsVariable("${comment.Figure_2}"));

        comment = properties.getProperty("comment.Figure_3");
        assertTrue(document.containsVariable("${comment.Figure_3}"));
        document.replaceParagraphText("${comment.Figure_3}", comment);
        assertFalse(document.containsVariable("${comment.Figure_3}"));
    }

    @Test
    public void testReplaceParagraphs() throws Exception {
        // @todo 2 tb/tb the same functionality is already covered by the test above. Iterate with NR and GC about the
        // variable names. Concequently all keys that start with paragraph replace a paragraph text. The comment.* keys then either
        // are removed or should have another functionality. tb 2015-07-10

        String paragraph = properties.getProperty("paragraph.summary_text");
        assertTrue(document.containsVariable("${paragraph.summary_text}"));
        document.replaceParagraphText("${paragraph.summary_text}", paragraph);
        assertFalse(document.containsVariable("${paragraph.summary_text}"));
    }

    @Test
    public void testReplaceVariableWithFigure() throws IOException, InvalidFormatException {
        if (!dataDir.isDirectory()) {
            System.out.println("data directory nor found, skipping `testReplaceVariableWithFigure`");
            return;
        }

        final String dependenceImage = properties.getProperty("figure.region_map");
        File imageFile = new File(dataDir, dependenceImage);
        assertTrue(document.containsVariable("${figure.region_map}"));
        document.replaceWithFigure("${figure.region_map}", imageFile);
        assertFalse(document.containsVariable("${figure.region_map}"));

        final String spatialImage = properties.getProperty("figure.Figure_2");
        imageFile = new File(dataDir, spatialImage);
        assertTrue(document.containsVariable("${figure.Figure_2}"));
        document.replaceWithFigure("${figure.Figure_2}", imageFile);
        assertFalse(document.containsVariable("${figure.Figure_2}"));
    }

    @Test
    public void testReplaceVariableWithFigure_andScaling() throws IOException, InvalidFormatException {
        if (!dataDir.isDirectory()) {
            System.out.println("data directory nor found, skipping `testReplaceVariableWithFigure_andScaling`");
            return;
        }

        final String dependenceImage = properties.getProperty("figure.Figure_4");
        final String scaleProperty = properties.getProperty("figure.Figure_4.scale");
        File imageFile = new File(dataDir, dependenceImage);

        assertTrue(document.containsVariable("${figure.Figure_4}"));
        document.replaceWithFigure("${figure.Figure_4}", imageFile, Double.parseDouble(scaleProperty));
        assertFalse(document.containsVariable("${figure.Figure_4}"));
    }

    @Test
    public void testReplaceFigures() throws Exception {
        if (!dataDir.isDirectory()) {
            System.out.println("Data directory for test is not installed. Test data is located at /fs1/projects/ongoing/SST-CCI/wp50");
            return;
        }

        final String filePattern = properties.getProperty("figures.dec_plot_temp_strip_rel_to_first");
        assertNotNull(filePattern);

        final File[] files = WildcardMatcher.glob(new File(dataDir, filePattern).getPath());
        assertTrue(files.length > 0);

        assertTrue(document.containsVariable("${figures.dec_plot_temp_strip_rel_to_first}"));
        document.replaceWithFigures("${figures.dec_plot_temp_strip_rel_to_first}", files);
        assertFalse(document.containsVariable("${figures.dec_plot_temp_strip_rel_to_first}"));
    }
}
