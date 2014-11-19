package org.esa.cci.sst.assessment;/*
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

import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.CTBookmark;
import org.docx4j.wml.CTMarkupRange;
import org.docx4j.wml.CTSimpleField;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Text;

import javax.xml.bind.JAXBElement;
import java.io.File;
import java.math.BigInteger;
import java.net.URL;

/**
 * A facade representing a Microsoft Word document.
 *
 * @author Ralf Quast
 */
public class WordDocument {

    private final WordprocessingMLPackage wordMLPackage;

    public WordDocument() throws Exception {
        this.wordMLPackage = WordprocessingMLPackage.createPackage();
    }

    public void save(File file) throws Exception {
        wordMLPackage.save(file);
    }

    public P addTitle(String titleText) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Title", titleText);
    }

    public P addHeading1(String headingText) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Heading1", headingText);
    }

    public P addHeading2(String headingText) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Heading2", headingText);
    }

    public P addParagraph(String text) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Normal", text);
    }

    public P addFigure(URL resource) throws Exception {
        final File imageFile = new File(resource.toURI());
        return addFigure(imageFile);
    }

    public P addFigure(File imageFile) throws Exception {
        final BinaryPartAbstractImage imagePart = BinaryPartAbstractImage.createImagePart(wordMLPackage, imageFile);
        final Inline inline = imagePart.createImageInline(imageFile.getName(), imageFile.getName(), 0, 0, true);

        // Add the inline in w:p/w:r/w:drawing
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

    public P addCaption(String label, String number, String captionText) {
        final ObjectFactory factory = new ObjectFactory();
        final P p = factory.createP();
        // Create object for pPr
        final PPr ppr = factory.createPPr();
        p.setPPr(ppr);
        // Create object for pStyle
        final PPrBase.PStyle pPrBasePStyle = factory.createPPrBasePStyle();
        ppr.setPStyle(pPrBasePStyle);
        pPrBasePStyle.setVal("Caption");
        // Create object for r
        final R r = factory.createR();
        p.getContent().add(r);
        // Create object for t (wrapped in JAXBElement)
        final Text text = factory.createText();
        final JAXBElement<Text> textWrapped = factory.createRT(text);
        r.getContent().add(textWrapped);
        text.setValue(label + " ");
        text.setSpace("preserve");
        // Create object for fldSimple (wrapped in JAXBElement)
        final CTSimpleField simpleField = factory.createCTSimpleField();
        final JAXBElement<CTSimpleField> simpleFieldWrapped = factory.createPFldSimple(simpleField);
        p.getContent().add(simpleFieldWrapped);
        simpleField.setInstr(" SEQ Figure \\* ARABIC ");
        // Create object for r
        final R r2 = factory.createR();
        simpleField.getContent().add(r2);
        // Create object for rPr
        final RPr rpr = factory.createRPr();
        r2.setRPr(rpr);
        // Create object for noProof
        final BooleanDefaultTrue booleanDefaultTrue = factory.createBooleanDefaultTrue();
        rpr.setNoProof(booleanDefaultTrue);
        // Create object for t (wrapped in JAXBElement)
        final Text text2 = factory.createText();
        final JAXBElement<Text> textWrapped2 = factory.createRT(text2);
        r2.getContent().add(textWrapped2);
        text2.setValue(number);
        // Create object for r
        final R r3 = factory.createR();
        p.getContent().add(r3);
        // Create object for t (wrapped in JAXBElement)
        final Text text3 = factory.createText();
        final JAXBElement<Text> textWrapped3 = factory.createRT(text3);
        r3.getContent().add(textWrapped3);
        text3.setValue(": " + captionText);
        // Create object for bookmarkStart (wrapped in JAXBElement)
        final CTBookmark bookmark = factory.createCTBookmark();
        final JAXBElement<CTBookmark> bookmarkWrapped = factory.createPBookmarkStart(bookmark);
        p.getContent().add(bookmarkWrapped);
        bookmark.setName("_GoBack");
        bookmark.setId(BigInteger.valueOf(0));
        // Create object for bookmarkEnd (wrapped in JAXBElement)
        final CTMarkupRange markupRange = factory.createCTMarkupRange();
        final JAXBElement<CTMarkupRange> markupRangeWrapped = factory.createPBookmarkEnd(markupRange);
        p.getContent().add(markupRangeWrapped);
        markupRange.setId(BigInteger.valueOf(0));

        wordMLPackage.getMainDocumentPart().addObject(p);
        return p;
    }
}
