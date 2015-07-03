/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.docx4j.wml.Drawing;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * @author Ralf Quast
 */
public class WordDocumentTest {

    private static WordDocument wordDocument;

    @BeforeClass
    public static void setUp() throws Exception {
        wordDocument = new WordDocument();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        final File targetFile = new File("test.docx");
        wordDocument.save(targetFile);

        if (targetFile.isFile()) {
            if (!targetFile.delete()) {
                fail("Unable to delete word file");
            }
        }
    }

    @Test
    public void testAddTitle() throws Exception {
        final P p = wordDocument.addTitle("This Shall Be a Title.");

        assertNotNull(p);
    }

    @Test
    public void testAddHeading1() throws Exception {
        final P p = wordDocument.addHeading1("This shall be a Heading 1.");

        assertNotNull(p);
    }

    @Test
    public void testAddHeading2() throws Exception {
        final P p = wordDocument.addHeading2("This shall be a Heading 2.");

        assertNotNull(p);
    }

    @Test
    public void testAddParagraph() throws Exception {
        final P p = wordDocument.addParagraph("This shall be normal text.");

        assertNotNull(p);
    }

    @Test
    public void testAddFigure() throws Exception {
        final Drawing drawing = wordDocument.createDrawing(getClass().getResource("newton-away.png"));
        final P p = wordDocument.addFigure(drawing);
        assertNotNull(p);
    }

    @Test
    public void testAddCaption() throws Exception {
        final P p = wordDocument.addCaption("Figure", "1", "This shall be a Caption.");

        assertNotNull(p);
    }

    @Test
    public void testAddAnotherCaption() throws Exception {
        final P p = wordDocument.addCaption("Figure", "2", "This shall be another Caption.");

        assertNotNull(p);
    }

    @Test
    public void testFindVariable() throws Exception {
        final P p = wordDocument.addVariable("${find.me}");

        assertSame(p, wordDocument.findVariable("${find.me}"));
        assertSame(p, wordDocument.findVariable("${FIND.ME}"));
    }

    @Test
    public void testRemoveVariable() throws Exception {
        final P p = wordDocument.addVariable("${remove.me}");

        assertSame(p, wordDocument.removeVariable("${remove.me}"));
        assertNull(wordDocument.findVariable("${remove.me}"));
    }

    @Test
    public void testReplaceVariableWithDrawing() throws Exception {
        final P p = wordDocument.addVariable("${replace.me}");

        assertSame(p, wordDocument.findVariable("${replace.me}"));

        final Drawing drawing = wordDocument.createDrawing(getClass().getResource("newton-home.png"));

        assertSame(p, wordDocument.replaceWithDrawing("${replace.me}", drawing));
        assertNull(wordDocument.findVariable("${replace.me}"));

        final R r = (R) p.getContent().get(0);

        assertSame(drawing, r.getContent().get(0));

        wordDocument.addCaption("Figure", "3", "The figure above is a replacement.");
    }
}
