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

import org.docx4j.wml.P;
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
public class WordDocumentTest {

    private static WordDocument wordDocument;

    @BeforeClass
    public static void setUp() throws Exception {
        wordDocument = new WordDocument();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        wordDocument.save(new File("test.docx"));
    }

    @Test
    public void testAddTitle() throws Exception {
        final P p = wordDocument.addTitle("This shall have Title style.");

        assertNotNull(p);
    }

    @Test
    public void testAddHeading1() throws Exception {
        final P p = wordDocument.addHeading1("This text shall have Heading 1 style.");

        assertNotNull(p);
    }

    @Test
    public void testAddHeading2() throws Exception {
        final P p = wordDocument.addHeading2("This text shall have Heading 2 style.");

        assertNotNull(p);
    }

    @Test
    public void testAddNormal() throws Exception {
        final P p = wordDocument.addParagraph("This text shall have Normal style.");

        assertNotNull(p);
    }

    @Test
    public void testAddFigure() throws Exception {
        final URL resource = getClass().getResource("newton-away-zoom.png");
        assertNotNull(resource);

        final P p = wordDocument.addFigure(resource);
        assertNotNull(p);
    }

    @Test
    public void testAddCaption() throws Exception {
        final P p = wordDocument.addCaption("Figure ", "1", "This shall be a caption");

        assertNotNull(p);
    }

    @Test
    public void testAnotherCaption() throws Exception {
        final P p = wordDocument.addCaption("Figure ", "2", "This shall be another caption");

        assertNotNull(p);
    }

}
