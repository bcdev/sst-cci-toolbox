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

import org.docx4j.TraversalUtil;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.finders.ClassFinder;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
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
import java.util.List;

/**
 * A facade representing a Microsoft Word document.
 *
 * @author Ralf Quast
 */
public class WordDocument {

    private final WordprocessingMLPackage wordMLPackage;

    /**
     * Creates a new empty Word document.
     *
     * @throws Exception if an error occurred.
     */
    public WordDocument() throws Exception {
        this.wordMLPackage = WordprocessingMLPackage.createPackage();
    }

    /**
     * Saves this Word document to a target file.
     *
     * @param targetFile The target file.
     *
     * @throws Exception if on error occurred.
     */
    public void save(File targetFile) throws Exception {
        wordMLPackage.save(targetFile);
    }

    /**
     * Adds a title to the Word document.
     *
     * @param text The title text.
     *
     * @return the enclosing "paragraph" element.
     */
    public P addTitle(String text) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Title", text);
    }

    /**
     * Adds a first-level heading to the Word document.
     *
     * @param text The heading text.
     *
     * @return the enclosing "paragraph" element.
     */
    public P addHeading1(String text) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Heading1", text);
    }

    /**
     * Adds a second-level heading to the Word document.
     *
     * @param text The heading text.
     *
     * @return the enclosing "paragraph" element.
     */
    public P addHeading2(String text) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Heading2", text);
    }


    /**
     * Adds a new paragraph the Word document.
     *
     * @param text The paragraph text.
     *
     * @return the enclosing "paragraph" element.
     */
    public P addParagraph(String text) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Normal", text);
    }

    /**
     * Adds a new figure the Word document.
     *
     * @param drawing The figure's drawing.
     *
     * @return the enclosing "paragraph" element.
     */
    public P addFigure(Drawing drawing) throws Exception {
        final ObjectFactory factory = Context.getWmlObjectFactory();
        final P p = factory.createP();
        final R r = factory.createR();
        p.getContent().add(r);
        r.getContent().add(drawing);

        wordMLPackage.getMainDocumentPart().addObject(p);
        return p;
    }

    /**
     * Adds a new caption the Word document.
     *
     * @param label  The caption's label (e.g. "Figure").
     * @param number The caption's number (e.g. "1" or "1.1").
     * @param text   The caption's text.
     *
     * @return the enclosing "paragraph" element.
     */
    public P addCaption(String label, String number, String text) {
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
        final Text t = factory.createText();
        final JAXBElement<Text> textWrapped = factory.createRT(t);
        r.getContent().add(textWrapped);
        t.setValue(label + " ");
        t.setSpace("preserve");
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
        final Text t2 = factory.createText();
        final JAXBElement<Text> textWrapped2 = factory.createRT(t2);
        r2.getContent().add(textWrapped2);
        t2.setValue(number);
        // Create object for r
        final R r3 = factory.createR();
        p.getContent().add(r3);
        // Create object for t (wrapped in JAXBElement)
        final Text t3 = factory.createText();
        final JAXBElement<Text> textWrapped3 = factory.createRT(t3);
        r3.getContent().add(textWrapped3);
        t3.setValue(": " + text);
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

    /**
     * Add a "variable" to the Word document.
     *
     * @param variable The variable.
     *
     * @return the enclosing "paragraph" element.
     */
    public P addVariable(String variable) {
        final ObjectFactory factory = Context.getWmlObjectFactory();
        final P p = factory.createP();
        final R r = factory.createR();
        final Text text = factory.createText();
        text.setValue(variable);
        p.getContent().add(r);
        r.getContent().add(text);

        wordMLPackage.getMainDocumentPart().addObject(p);
        return p;
    }

    /**
     * Creates a new drawing from a resource.
     *
     * @param resource The resource.
     *
     * @return the drawing.
     *
     * @throws Exception if an error occurred.
     */
    public Drawing createDrawing(URL resource) throws Exception {
        final File imageFile = new File(resource.toURI());
        return createDrawing(imageFile);
    }

    /**
     * Creates a new drawing from an image file.
     *
     * @param imageFile The image file.
     *
     * @return the drawing.
     *
     * @throws Exception if an error occurred.
     */
    public Drawing createDrawing(File imageFile) throws Exception {
        final ObjectFactory factory = Context.getWmlObjectFactory();
        final Drawing drawing = factory.createDrawing();
        final BinaryPartAbstractImage imagePart = BinaryPartAbstractImage.createImagePart(wordMLPackage, imageFile);
        final Inline inline = imagePart.createImageInline(imageFile.getName(), imageFile.getName(), 0, 0, true);
        drawing.getAnchorOrInline().add(inline);

        return drawing;
    }

    /**
     * Traverses a Word document and looks for the first occurrence of a "template variable".
     *
     * @param variable The template variable.
     *
     * @return the enclosing "paragraph" element or {@code null}, if the requested template variable has not been found.
     */
    public P findVariable(String variable) {
        final MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
        final ClassFinder finder = new ClassFinder(P.class);
        new TraversalUtil(documentPart.getContent(), finder);

        for (final Object o : finder.results) {
            if (o instanceof P) {
                final P p = (P) o;
                final List<Object> c = p.getContent();
                if (c.size() == 1) {
                    final Object o1 = c.get(0);
                    if (o1 instanceof R) {
                        final R r = (R) o1;
                        final List<Object> c1 = r.getContent();
                        if (c1.size() == 1) {
                            final Object o2 = c1.get(0);
                            if (o2 instanceof Text) {
                                final Text t = (Text) o2;
                                if (variable.equalsIgnoreCase(t.getValue())) {
                                    return p;
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Traverses a Word document and removes the first occurrence of a given "template variable".
     *
     * @param variable The template variable.
     *
     * @return the enclosing "paragraph" removed or {@code null}, if the  template variable has not been found.
     */
    public P removeVariable(String variable) {
        final P p = findVariable(variable);
        if (p != null) {
            final boolean removed = wordMLPackage.getMainDocumentPart().getContent().remove(p);
            if (removed) {
                return p;
            }
        }
        return null;
    }

    /**
     * Traverses a Word document and replaces the first occurrence of a "template variable" with a drawing.
     *
     * @param variable The template variable.
     * @param drawing  The drawing.
     *
     * @return the replaced "text" or {@code null}, if the requested template variable has not been found.
     */
    public Text replaceVariable(String variable, Drawing drawing) {
        final P p = findVariable(variable);

        if (p != null) {
            final List<Object> c = p.getContent();
            final R r = (R) c.get(0);
            final List<Object> c1 = r.getContent();
            c1.add(drawing);
            return (Text) c1.remove(0);
        }

        return null;
    }
}
