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

import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.HeaderUtil;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

/**
 * The abstract base class for all header containing infomation in ASCII format
 *
 * @author Marco Zühlke
 */
abstract class AsciiRecord {

    private Map<String, String> map;
    private int fieldCount;

    AsciiRecord(int fieldCount) {
        this.fieldCount = fieldCount;
        this.map = new HashMap<>();
    }

    void readRecord(ImageInputStream imageInputStream) throws IOException {
        for (int i = 0; i < fieldCount; i++) {
            final String fieldString = imageInputStream.readLine();
            final KeyValuePair field = new KeyValuePair(fieldString);

            map.put(field.key, field.value);
        }
    }

    String getValue(String key) {
        return map.get(key);
    }

    int getIntValue(String key) {
        return Integer.parseInt(getValue(key));
    }

    abstract MetadataElement getMetaData();

    private class KeyValuePair {
        final String key;
        final String value;

        public KeyValuePair(String field) {
            key = field.substring(0, 30).trim();
            value = field.substring(32).trim();
        }
    }

    MetadataAttribute createStringAttribute(String key, String unit) {
        String stringValue = getValue(key);
        if (stringValue != null) {
            return HeaderUtil.createAttribute(key, stringValue, unit);
        } else {
            return null;
        }
    }

    MetadataAttribute createFloatAttribute(String key, float scalingFactor, String unit) {
        String stringValue = getValue(key);
        if (stringValue != null) {
            try {
                final long longValue = Long.parseLong(stringValue);
                return HeaderUtil.createAttribute(key, longValue * scalingFactor, unit);
            } catch (NumberFormatException e) {
                return HeaderUtil.createAttribute(key, stringValue, unit);
            }
        } else {
            return null;
        }
    }

    MetadataAttribute createIntAttribute(String key, String unit) {
        String stringValue = getValue(key);
        if (stringValue != null) {
            try {
                final int intValue = Integer.parseInt(stringValue);
                return HeaderUtil.createAttribute(key, intValue, unit);
            } catch (NumberFormatException e) {
                return HeaderUtil.createAttribute(key, stringValue, unit);
            }
        } else {
            return null;
        }
    }

    MetadataAttribute createDateAttribute(String key, DateFormat dateFormat) {
        final String dateString = getValue(key);
        MetadataAttribute attribute;
        try {
            final Date date = dateFormat.parse(dateString);
            ProductData.UTC utc = ProductData.UTC.create(date, 0);
            attribute = new MetadataAttribute(key, utc, true);
        } catch (ParseException e) {
            ProductData data = ProductData.createInstance(dateString);
            attribute = new MetadataAttribute(key, data, true);
        }
        attribute.setUnit(AvhrrConstants.UNIT_DATE);
        return attribute;
    }
}