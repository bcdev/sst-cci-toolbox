/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.rules;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.reader.Reader;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;

/**
 * @author Thomas Storm
 */
@SuppressWarnings({"UnusedDeclaration", "AbstractClassWithoutAbstractMethods"})
abstract class AtsrCalibrationFilenames extends Rule {

    private final String elementId;

    protected AtsrCalibrationFilenames(String elementId) {
        this.elementId = elementId;
    }

    @Override
    public Item apply(Item sourceColumn) throws RuleException {
        return new ColumnBuilder(sourceColumn).type(DataType.CHAR).build();
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array array = Array.factory(DataType.CHAR, new int[]{1, 80});
        final Reader observationReader = getContext().getObservationReader();
        if(observationReader == null) {
            return array;
        }
        final Product product = observationReader.getProduct();
        final MetadataElement dsdElement = product.getMetadataRoot().getElement("DSD");
        final MetadataElement element = dsdElement.getElement(elementId);
        final MetadataAttribute filenameAttribute = element.getAttribute("FILE_NAME");
        final String calibrationFilename = filenameAttribute.getData().getElemString();
        final Index index = array.getIndex();
        for(int i = 0; i < Math.min(calibrationFilename.length(), 80); i++) {
            index.set(0, i);
            array.setChar(index, calibrationFilename.charAt(i));
        }
        return array;
    }
}
