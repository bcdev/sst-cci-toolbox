package org.esa.cci.sst.assessment;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ReplacemeTest {

    private PoiWordDocument document;
    private URL docxResource;
    private File targetFile;

    @Before
    public void setUp() throws Exception {
        docxResource = CarTemplateTest.class.getResource("P-C-Replace-template.docx");
        document = new PoiWordDocument(docxResource);
        targetFile = new File("temp.docx");
    }

    @After
    public void tearDown() throws Exception {
        if (targetFile.isFile()) {
            if (!targetFile.delete()) {
                fail("Unable to delete test file");
            }
        }
    }

    @Test
    public void replaceParagraphAndComment() throws IOException {
        XWPFDocument xwpf;
        List<XWPFParagraph> paragraphs;

        xwpf = new XWPFDocument(docxResource.openStream());
        paragraphs = xwpf.getParagraphs();
        assertThat(paragraphs.size(), is(2));
        assertThat(paragraphs.get(0).getParagraphText(), is(equalTo("Expected ${comment.replace} Text.")));
        assertThat(paragraphs.get(1).getParagraphText(), is(equalTo("Expected ${paragraph.replace} Text.")));

        document.replaceParagraphText("${comment.replace}", "AAAA");
        document.replaceParagraphText("${paragraph.replace}", "BBBB");
        document.save(targetFile);

        xwpf = new XWPFDocument(targetFile.toURL().openStream());
        paragraphs = xwpf.getParagraphs();
        assertThat(paragraphs.size(), is(2));
        assertThat(paragraphs.get(0).getParagraphText(), is(equalTo("AAAA")));
        assertThat(paragraphs.get(1).getParagraphText(), is(equalTo("BBBB")));
    }

    @Test
    public void replaceWord() throws IOException {
        XWPFDocument xwpf;
        List<XWPFParagraph> paragraphs;

        xwpf = new XWPFDocument(docxResource.openStream());
        paragraphs = xwpf.getParagraphs();
        assertThat(paragraphs.size(), is(2));
        assertThat(paragraphs.get(0).getParagraphText(), is(equalTo("Expected ${comment.replace} Text.")));
        assertThat(paragraphs.get(1).getParagraphText(), is(equalTo("Expected ${paragraph.replace} Text.")));

        document.replaceWithText("${comment.replace}", "AAAA");
        document.replaceWithText("${paragraph.replace}", "BBBB");
        document.save(targetFile);

        xwpf = new XWPFDocument(targetFile.toURL().openStream());
        paragraphs = xwpf.getParagraphs();
        assertThat(paragraphs.size(), is(2));
        assertThat(paragraphs.get(0).getParagraphText(), is(equalTo("Expected AAAA Text.")));
        assertThat(paragraphs.get(1).getParagraphText(), is(equalTo("Expected BBBB Text.")));
    }
}
