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

import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertNotNull;

/**
 * @author Ralf Quast
 */
@Ignore
public class DocxTest {

    private static WordprocessingMLPackage wordMLPackage;

    @BeforeClass
    public static void setUp() throws Exception {
        wordMLPackage = WordprocessingMLPackage.createPackage();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        wordMLPackage.save(new File("test.docx"));
    }

    @Test
    public void testAddTitle() throws Exception {
        final P p = addTitle(wordMLPackage, "This text shall have Title style.");

        assertNotNull(p);
    }

    public static P addTitle(WordprocessingMLPackage wordMLPackage, String titleText) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Title", titleText);
    }

    @Test
    public void testAddHeading1() throws Exception {
        final P p = addHeading1(wordMLPackage, "This text shall have Heading 1 style.");

        assertNotNull(p);
    }

    public static P addHeading1(WordprocessingMLPackage wordMLPackage, String headingText) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Heading1", headingText);
    }

    @Test
    public void testAddNormal() throws Exception {
        final P p = addNormal(wordMLPackage, "This text shall have Normal style.");

        assertNotNull(p);
    }

    public static P addNormal(WordprocessingMLPackage wordMLPackage, String text) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Normal", text);
    }

    @Test
    public void testAddImage() throws Exception {
        final URL resource = getClass().getResource("newton-away-zoom.png");
        assertNotNull(resource);

        final P p = addImage(wordMLPackage, resource);
        assertNotNull(p);
    }

    public static P addImage(WordprocessingMLPackage wordMLPackage, URL resource) throws Exception {
        final File imageFile = new File(resource.toURI());
        return addImage(wordMLPackage, imageFile);
    }

    public static P addImage(WordprocessingMLPackage wordMLPackage, File imageFile) throws Exception {
        final BinaryPartAbstractImage imagePart = BinaryPartAbstractImage.createImagePart(wordMLPackage, imageFile);
        final Inline inline = imagePart.createImageInline(imageFile.getName(), imageFile.getName(), 0, 0, true);

        // add the inline in w:p/w:r/w:drawing
        final ObjectFactory factory = Context.getWmlObjectFactory();
        final P p = factory.createP();
        final R r = factory.createR();
        p.getContent().add(r);
        final Drawing drawing = factory.createDrawing();
        r.getContent().add(drawing);
        drawing.getAnchorOrInline().add(inline);

        wordMLPackage.getMainDocumentPart().addObject(p);
        return p;
    }

}
