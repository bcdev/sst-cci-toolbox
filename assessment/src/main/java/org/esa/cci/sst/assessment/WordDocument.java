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

import org.docx4j.TextUtils;
import org.docx4j.TraversalUtil;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.finders.ClassFinder;
import org.docx4j.jaxb.Context;
import org.docx4j.model.datastorage.migration.VariablePrepare;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.*;
import org.esa.beam.util.io.WildcardMatcher;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * A facade representing a Microsoft Word document.
 *
 * @author Ralf Quast
 */
public class WordDocument {

    private final WordprocessingMLPackage wordMLPackage;

    /**
     * Creates a new empty instance of this class.
     *
     * @throws Exception if an error occurred.
     */
    public WordDocument() throws Exception {
        this.wordMLPackage = WordprocessingMLPackage.createPackage();
    }

    /**
     * Creates a new instance of this class from a URL pointing to a Word document file.
     *
     * @param url The URL.
     */
    public WordDocument(URL url) throws IOException, URISyntaxException {
        this(new File(url.toURI()));
    }

    /**
     * Creates a new instance of this class from a Word document file.
     *
     * @param wordFile The Word document file.
     */
    public WordDocument(File wordFile) throws IOException {
        try {
            this.wordMLPackage = WordprocessingMLPackage.load(wordFile);
            VariablePrepare.prepare(wordMLPackage);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Saves this Word document to a target file.
     *
     * @param targetFile The target file.
     * @throws Exception if on error has occurred.
     */
    public void save(File targetFile) throws Exception {
        wordMLPackage.save(targetFile);
    }

    /**
     * Adds a title to the Word document.
     *
     * @param text The title text.
     * @return the enclosing "paragraph" element.
     */
    public P addTitle(String text) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Title", text);
    }

    /**
     * Adds a first-level heading to the Word document.
     *
     * @param text The heading text.
     * @return the enclosing "paragraph" element.
     */
    public P addHeading1(String text) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Heading1", text);
    }

    /**
     * Adds a second-level heading to the Word document.
     *
     * @param text The heading text.
     * @return the enclosing "paragraph" element.
     */
    public P addHeading2(String text) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Heading2", text);
    }


    /**
     * Adds a new paragraph the Word document.
     *
     * @param text The paragraph text.
     * @return the enclosing "paragraph" element.
     */
    public P addParagraph(String text) {
        return wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("Normal", text);
    }

    /**
     * Adds a new figure the Word document.
     *
     * @param drawing The figure's drawing.
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
     * @return the enclosing "paragraph" element.
     */
    public P addCaption(String label, String number, String text) {
        /*
            Based on generated code, see http://webapp.docx4java.org/OnlineDemo/demo_landing.html
         */
        final ObjectFactory wmlObjectFactory = new ObjectFactory();
        final P p = wmlObjectFactory.createP();
        // Create object for pPr
        final PPr ppr = wmlObjectFactory.createPPr();
        p.setPPr(ppr);
        // Create object for pStyle
        final PPrBase.PStyle pPrBasePStyle = wmlObjectFactory.createPPrBasePStyle();
        ppr.setPStyle(pPrBasePStyle);
        pPrBasePStyle.setVal("Caption");
        // Create object for r
        final R r = wmlObjectFactory.createR();
        p.getContent().add(r);
        // Create object for t (wrapped in JAXBElement)
        final Text t = wmlObjectFactory.createText();
        final JAXBElement<Text> textWrapped = wmlObjectFactory.createRT(t);
        r.getContent().add(textWrapped);
        t.setValue(label + " ");
        t.setSpace("preserve");
        // Create object for fldSimple (wrapped in JAXBElement)
        final CTSimpleField simpleField = wmlObjectFactory.createCTSimpleField();
        final JAXBElement<CTSimpleField> simpleFieldWrapped = wmlObjectFactory.createPFldSimple(simpleField);
        p.getContent().add(simpleFieldWrapped);
        simpleField.setInstr(" SEQ Figure \\* ARABIC ");
        // Create object for r
        final R r2 = wmlObjectFactory.createR();
        simpleField.getContent().add(r2);
        // Create object for rPr
        final RPr rpr = wmlObjectFactory.createRPr();
        r2.setRPr(rpr);
        // Create object for noProof
        final BooleanDefaultTrue booleanDefaultTrue = wmlObjectFactory.createBooleanDefaultTrue();
        rpr.setNoProof(booleanDefaultTrue);
        // Create object for t (wrapped in JAXBElement)
        final Text t2 = wmlObjectFactory.createText();
        final JAXBElement<Text> textWrapped2 = wmlObjectFactory.createRT(t2);
        r2.getContent().add(textWrapped2);
        t2.setValue(number);
        // Create object for r
        final R r3 = wmlObjectFactory.createR();
        p.getContent().add(r3);
        // Create object for t (wrapped in JAXBElement)
        final Text t3 = wmlObjectFactory.createText();
        final JAXBElement<Text> textWrapped3 = wmlObjectFactory.createRT(t3);
        r3.getContent().add(textWrapped3);
        t3.setValue(": " + text);
        // Create object for bookmarkStart (wrapped in JAXBElement)
        final CTBookmark bookmark = wmlObjectFactory.createCTBookmark();
        final JAXBElement<CTBookmark> bookmarkWrapped = wmlObjectFactory.createPBookmarkStart(bookmark);
        p.getContent().add(bookmarkWrapped);
        bookmark.setName("_GoBack");
        bookmark.setId(BigInteger.valueOf(0));
        // Create object for bookmarkEnd (wrapped in JAXBElement)
        final CTMarkupRange markupRange = wmlObjectFactory.createCTMarkupRange();
        final JAXBElement<CTMarkupRange> markupRangeWrapped = wmlObjectFactory.createPBookmarkEnd(markupRange);
        p.getContent().add(markupRangeWrapped);
        markupRange.setId(BigInteger.valueOf(0));

        wordMLPackage.getMainDocumentPart().addObject(p);
        return p;
    }

    /**
     * Add a "variable" to the Word document.
     *
     * @param variable The variable.
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
     * @return the drawing.
     * @throws Exception if an error has occurred.
     */
    Drawing createDrawing(URL resource) throws Exception {
        final File imageFile = new File(resource.toURI());
        return createDrawing(imageFile);
    }

    /**
     * Creates a new drawing from an image file.
     *
     * @param imageFile The image file.
     * @return the drawing.
     * @throws Exception if an error has occurred.
     */
    Drawing createDrawing(File imageFile) throws Exception {
        final ObjectFactory factory = Context.getWmlObjectFactory();
        final Drawing drawing = factory.createDrawing();
        final BinaryPartAbstractImage imagePart = BinaryPartAbstractImage.createImagePart(wordMLPackage, imageFile);
        final Inline inline = imagePart.createImageInline(imageFile.getName(), imageFile.getName(), 0, 0, true);
        drawing.getAnchorOrInline().add(inline);

        return drawing;
    }

    /**
     * Traverses a Word document and looks for the first occurrence of a "template variable" as a complete paragraph.
     *
     * @param variable The template variable.
     * @return the enclosing "paragraph" element or {@code null}..
     */
    public P findVariableParagraph(String variable) {
        final MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
        final ClassFinder finder = new ClassFinder(P.class);
        new TraversalUtil(documentPart.getContent(), finder);

        for (final Object o : finder.results) {
            if (o instanceof P) {
                final P p = (P) o;
                final StringWriter sw = new StringWriter();
                try {
                    TextUtils.extractText(p, sw);
                    final String text = sw.toString();
                    if (variable.equalsIgnoreCase(text)) {
                        return p;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    /**
     * Traverses a Word document and looks for the first occurrence of a "template variable".
     *
     * @param variable The template variable.
     * @return the enclosing "paragraph" element or {@code null}.
     */
    public P findVariable(String variable) {
        final MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
        final ClassFinder finder = new ClassFinder(P.class);
        new TraversalUtil(documentPart.getContent(), finder);

        for (final Object o : finder.results) {
            if (o instanceof P) {
                final P p = (P) o;
                final StringWriter sw = new StringWriter();
                try {
                    TextUtils.extractText(p, sw);
                    final String text = sw.toString();
                    if (text.contains(variable)) {
                        return p;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    /**
     * Traverses a Word document and removes the first occurrence of a given "template variable".
     *
     * @param variable The template variable.
     * @return the enclosing "paragraph" removed or {@code null}, if the  template variable has not been removed.
     */
    public P removeVariable(String variable) {
        final P p = findVariableParagraph(variable);
        if (p != null) {
            final List<Object> parent = getParentContainer(p);
            final boolean removed = parent.remove(p);
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
     * @return the "paragraph" where the replacing occurred or {@code null}, if the requested template variable has not been found.
     */
    public P replaceWithDrawing(String variable, Drawing drawing) {
        final P p = findVariableParagraph(variable);

        if (p != null) {
            return replaceContentWithDrawing(p, drawing);
        }

        return null;
    }

    public void replaceWithText(String variable, String text) throws IOException {

        final HashMap<String, String> mappings = new HashMap<>();
        mappings.put(variable, text);

        try {
            wordMLPackage.getMainDocumentPart().variableReplace(mappings);
        } catch (JAXBException | Docx4JException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Traverses a Word document and replaces the first occurrence of a "template variable" with an image.
     *
     * @param variable  The template variable.
     * @param imageFile The image file.
     * @return the "paragraph" where the replacing occurred or {@code null}, if the requested template variable has not been found.
     */
    public P replaceWithImage(String variable, File imageFile) throws Exception {
        return replaceWithDrawing(variable, createDrawing(imageFile));
    }

    /**
     * Traverses a Word document and replaces the first occurrence of a "template variable" with a series of images.
     *
     * @param variable   The template variable.
     * @param imageFiles The image files.
     * @return the "paragraph" where the replacing occurred or {@code null}, if the requested template variable has not been found.
     */
    public P replaceWithImages(String variable, File[] imageFiles) throws Exception {
        final P p = findVariableParagraph(variable);

        if (p != null) {
            final List<Object> c = getParentContainer(p);
            final int index = c.indexOf(p);
            final ObjectFactory wmlObjectFactory = new ObjectFactory();

            replaceContentWithDrawing(p, createDrawing(imageFiles[0]));

            for (int i = 1; i < imageFiles.length; i++) {
                final P q = wmlObjectFactory.createP();
                final R r = wmlObjectFactory.createR();
                final Drawing drawing = createDrawing(imageFiles[i]);
                q.getContent().add(r);
                r.getContent().add(drawing);

                c.add(index + i, q);
            }

            return p;
        }

        return null;
    }

    /**
     * Traverses a Word document and replaces the first occurrence of a "template variable" with a paragraph of text.
     *
     * @param variable The template variable.
     * @param text     The text.
     * @return the "paragraph" where the replacing occurred or {@code null}, if the requested template variable has not been found.
     */
    public P replaceWithParagraph(String variable, String text) {
        final P p = findVariableParagraph(variable);

        if (p != null) {
            final ObjectFactory wmlObjectFactory = new ObjectFactory();
            final R r = replaceContentWithR(p, wmlObjectFactory);

            final Text t = wmlObjectFactory.createText();
            t.setValue(text);

            final JAXBElement<Text> textWrapped = wmlObjectFactory.createRT(t);
            r.getContent().add(textWrapped);

            return p;
        }

        return null;
    }

    private static List<Object> getParentContainer(P p) {
        final Object parent = p.getParent();
        if (parent instanceof List<?>) {
            //noinspection unchecked
            return (List<Object>) parent;
        }
        if (parent instanceof ContentAccessor) {
            return ((ContentAccessor) parent).getContent();
        }
        throw new RuntimeException("Unexpected parent type for P.");
    }

    private static P replaceContentWithDrawing(P p, Drawing drawing) {
        final R r = replaceContentWithR(p, new ObjectFactory());

        r.getContent().add(drawing);

        return p;
    }

    private static R replaceContentWithR(P p, ObjectFactory wmlObjectFactory) {
        final List<Object> c = p.getContent();
        c.clear();
        final R r = wmlObjectFactory.createR();
        c.add(r);
        return r;
    }

    /**
     * Creates a new Word document file from a template file.
     *
     * @param templateFile   The template file.
     * @param propertiesFile The properties files with template variables to be resolved.
     * @param figureRootDir  The root directory containing figure replacements.
     * @param wordFile       The target word document file.
     * @throws Exception If an error has occurred.
     */
    public static void createWordDocumentFromTemplate(File templateFile, File propertiesFile, File figureRootDir, File wordFile) throws Exception {
        final WordDocument wordDocument = new WordDocument(templateFile);
        final Properties properties = new Properties();
        try (final Reader reader = new FileReader(propertiesFile)) {
            properties.load(reader);
        }

        for (final String name : properties.stringPropertyNames()) {
            final String value = properties.getProperty(name);
            if (name.startsWith("comment.")) {
                wordDocument.replaceWithParagraph(name, value);
            } else if (name.startsWith("figure.")) {
                wordDocument.replaceWithImage(name, new File(figureRootDir, value));
            } else if (name.startsWith("figures.")) {
                wordDocument.replaceWithImages(name, WildcardMatcher.glob(new File(figureRootDir, value).getPath()));
            } else if (name.startsWith("paragraph.")) {
                wordDocument.replaceWithParagraph(name, value);
            }
        }

        wordDocument.save(wordFile);
    }
}
