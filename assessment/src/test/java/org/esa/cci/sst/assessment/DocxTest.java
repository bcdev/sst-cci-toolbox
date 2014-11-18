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

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

/**
 * @author Ralf Quast
 */
public class DocxTest {

    @Ignore
    @Test
    public void testDocx() throws Exception {
        final File templateFile = new File("/Users/ralf/Desktop/Trends_and_Variability_Framework.docx");
        final WordprocessingMLPackage template = WordprocessingMLPackage.load(templateFile);

        template.getMainDocumentPart().addStyledParagraphOfText("Title", "Hello World!");
        template.getMainDocumentPart().addStyledParagraphOfText("Heading1", "Hello World!");
        template.getMainDocumentPart().addStyledParagraphOfText("Normal", "Hello World!");
        template.save(new File("/Users/ralf/Desktop/car.docx"));
    }
}
