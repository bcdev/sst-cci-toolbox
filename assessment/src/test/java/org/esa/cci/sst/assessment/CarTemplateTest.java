/*
 * Copyright (c) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.assessment;

import org.esa.beam.util.io.WildcardMatcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public class CarTemplateTest {

    private static Properties properties;
    private static WordDocument wordDocument;

    @BeforeClass
    public static void setUp() throws Exception {
        properties = new Properties();
        properties.load(CarTemplateTest.class.getResourceAsStream("car-template.properties"));
        wordDocument = new WordDocument(CarTemplateTest.class.getResource("car-template.docx"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        wordDocument.save(new File("car.docx"));
    }

    @Test
    public void testReplaceCommentVariables() throws Exception {
        String text;

        text = properties.getProperty("comment.Figure_2");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${comment.Figure_2}", text));

        text = properties.getProperty("comment.Figure_3");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${comment.Figure_3}", text));

        text = properties.getProperty("comment.Figure_4");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${comment.Figure_4}", text));

        text = properties.getProperty("comment.Figure_5");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${comment.Figure_5}", text));

        text = properties.getProperty("comment.dec_plot_temp_strip_rel_to_first");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${comment.dec_plot_temp_strip_rel_to_first}", text));

        text = properties.getProperty("comment.decadal_selection");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${comment.decadal_selection}", text));

        text = properties.getProperty("comment.decadal_wall_1991");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${comment.decadal_wall_1991}", text));

        text = properties.getProperty("comment.decadal_wall_2001");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${comment.decadal_wall_2001}", text));

        text = properties.getProperty("comment.plot_lagcorr");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${comment.plot_lagcorr}", text));
    }

    @Test
    public void testReplaceParagraphVariables() throws Exception {
        String text;

        text = properties.getProperty("paragraph.summary_text");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${paragraph.summary_text}", text));

        text = properties.getProperty("paragraph.plot_selection_COLOC");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${paragraph.plot_selection_COLOC}", text));

        text = properties.getProperty("paragraph.plot_selection");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${paragraph.plot_selection}", text));
    }

    @Test
    public void testReplaceFigure_2() throws Exception {
        final String path = properties.getProperty("figure.Figure_2");

        assertNotNull(path);

        assertNotNull(wordDocument.replaceWithImage("${figure.Figure_2}", path));
    }

    @Test
    public void testReplaceFigure_3() throws Exception {
        final String path = properties.getProperty("figure.Figure_3");

        assertNotNull(path);

        assertNotNull(wordDocument.replaceWithImage("${figure.Figure_3}", path));
    }

    @Test
    public void testReplaceFigure_4() throws Exception {
        final String path = properties.getProperty("figure.Figure_4");

        assertNotNull(path);

        assertNotNull(wordDocument.replaceWithImage("${figure.Figure_4}", path));
    }


    @Test
    public void testReplaceFigure_5() throws Exception {
        final String path = properties.getProperty("figure.Figure_5");

        assertNotNull(path);

        assertNotNull(wordDocument.replaceWithImage("${figure.Figure_5}", path));
    }

    @Test
    public void testReplaceFigures_lagcorr() throws Exception {
        final String filePattern = properties.getProperty("figures.plot_lagcorr");

        assertNotNull(filePattern);

        final File[] files = WildcardMatcher.glob(filePattern);

        assertNotNull(wordDocument.replaceWithImages("${figures.plot_lagcorr}", files));
    }

}
