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
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CarTemplateTest {

    private static Properties properties;
    private static WordDocument wordDocument;
    private static File dataDir;

    @BeforeClass
    public static void setUp() throws Exception {
        wordDocument = new WordDocument(CarTemplateTest.class.getResource("car-template.docx"));

        properties = new Properties();
        properties.load(CarTemplateTest.class.getResourceAsStream("car-template.properties"));

        dataDir = new File(System.getProperty("user.home"), "scratch/car/figures");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        wordDocument.save(new File("car.docx"));
    }

    @Test
    public void testReplaceComments() throws Exception {
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

        text = properties.getProperty("comment.plot_lag_corr");

        assertNotNull(text);

        assertNotNull(wordDocument.replaceWithParagraph("${comment.plot_lag_corr}", text));
    }

    @Test
    public void testReplaceParagraphs() throws Exception {
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
    public void testReplaceRegionMap() throws Exception {
        if (dataDir.exists()) {
            final String path = properties.getProperty("figure.region_map");

            assertNotNull(path);

            final File file = new File(dataDir, path);

            assertTrue(file.exists());

            assertNotNull(wordDocument.replaceWithImage("${figure.region_map}", file));
        }
    }

    @Test
    public void testReplaceFigure_2() throws Exception {
        if (dataDir.exists()) {
            final String path = properties.getProperty("figure.Figure_2");

            assertNotNull(path);

            final File file = new File(dataDir, path);

            assertTrue(file.exists());

            assertNotNull(wordDocument.replaceWithImage("${figure.Figure_2}", file));
        }
    }

    @Test
    public void testReplaceFigure_3() throws Exception {
        if (dataDir.exists()) {
            final String path = properties.getProperty("figure.Figure_3");

            assertNotNull(path);

            final File file = new File(dataDir, path);

            assertTrue(file.exists());

            assertNotNull(wordDocument.replaceWithImage("${figure.Figure_3}", file));
        }
    }

    @Test
    public void testReplaceFigure_4() throws Exception {
        if (dataDir.exists()) {
            final String path = properties.getProperty("figure.Figure_4");

            assertNotNull(path);

            final File file = new File(dataDir, path);

            assertTrue(file.exists());

            assertNotNull(wordDocument.replaceWithImage("${figure.Figure_4}", file));
        }
    }


    @Test
    public void testReplaceFigure_5() throws Exception {
        if (dataDir.exists()) {
            final String path = properties.getProperty("figure.Figure_5");

            assertNotNull(path);

            final File file = new File(dataDir, path);

            assertTrue(file.exists());

            assertNotNull(wordDocument.replaceWithImage("${figure.Figure_5}", file));
        }
    }

    @Test
    public void testReplaceFigures_dec_plot() throws Exception {
        if (dataDir.exists()) {
            final String filePattern = properties.getProperty("figures.dec_plot_temp_strip_rel_to_first");

            assertNotNull(filePattern);

            final File[] files = WildcardMatcher.glob(new File(dataDir, filePattern).getPath());

            assertTrue(files.length > 0);

            assertNotNull(wordDocument.replaceWithImages("${figures.dec_plot_temp_strip_rel_to_first}", files));
        }
    }

    @Test
    public void testReplaceFigures_decadal_selection() throws Exception {
        if (dataDir.exists()) {
            final String filePattern = properties.getProperty("figures.decadal_selection");

            assertNotNull(filePattern);

            final File[] files = WildcardMatcher.glob(new File(dataDir, filePattern).getPath());

            assertTrue(files.length > 0);

            assertNotNull(wordDocument.replaceWithImages("${figures.decadal_selection}", files));
        }
    }

    @Test
    public void testReplaceFigures_plot_selection_COLOC() throws Exception {
        if (dataDir.exists()) {
            final String filePattern = properties.getProperty("figures.plot_selection_COLOC");

            assertNotNull(filePattern);

            final File[] files = WildcardMatcher.glob(new File(dataDir, filePattern).getPath());

            assertTrue(files.length > 0);

            assertNotNull(wordDocument.replaceWithImages("${figures.plot_selection_COLOC}", files));
        }
    }

    @Test
    public void testReplaceFigures_plot_selection() throws Exception {
        if (dataDir.exists()) {
            final String filePattern = properties.getProperty("figures.plot_selection");

            assertNotNull(filePattern);

            final File[] files = WildcardMatcher.glob(new File(dataDir, filePattern).getPath());

            assertTrue(files.length > 0);

            assertNotNull(wordDocument.replaceWithImages("${figures.plot_selection}", files));
        }
    }

    @Test
    public void testReplaceFigures_lag_corr() throws Exception {
        if (dataDir.exists()) {
            final String filePattern = properties.getProperty("figures.plot_lag_corr");

            assertNotNull(filePattern);

            final File[] files = WildcardMatcher.glob(new File(dataDir, filePattern).getPath());

            assertTrue(files.length > 0);

            assertNotNull(wordDocument.replaceWithImages("${figures.plot_lag_corr}", files));
        }
    }

}
