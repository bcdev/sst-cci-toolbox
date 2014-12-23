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
package org.eumetsat.beam.dataio.metop;

import org.esa.beam.framework.datamodel.MetadataElement;

/**
 * Reads a Second Product Header Record (SPHR)and make the contained
 * metadata available as MetadataElements
 *
 * @author marcoz
 * @version $Revision: 1.1.1.1 $ $Date: 2007/03/22 11:12:51 $
 */
class SecondaryProductHeaderRecord extends AsciiRecord {
    private static final int NUM_FIELDS = 3;

    public SecondaryProductHeaderRecord() {
        super(NUM_FIELDS);
    }

    @Override
    public MetadataElement getMetaData() {
        final MetadataElement element = new MetadataElement("SPH");
        element.addAttribute(createIntAttribute("SRC_DATA_QUAL", null));
        element.addAttribute(createIntAttribute("EARTH_VIEWS_PER_SCANLINE", null));
        element.addAttribute(createIntAttribute("NAV_SAMPLE_RATE", null));
        return element;
    }
}
