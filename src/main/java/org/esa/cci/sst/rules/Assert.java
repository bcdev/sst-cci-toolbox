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

import org.esa.cci.sst.data.Item;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.text.MessageFormat;

/**
 * A utility class used for checking the applicability of rules.
 *
 * @author Ralf Quast
 */
class Assert {

    private Assert() {
    }

    static void notNull(String value, String name) throws RuleException {
        if (value == null) {
            throw new RuleException(
                    MessageFormat.format("Expected non-null value for property ''{0}''.", name));
        }
    }

    static void notEmpty(String value, String name) throws RuleException {
        if (value.isEmpty()) {
            throw new RuleException(
                    MessageFormat.format("Expected non-empty value for property ''{0}''.", name));
        }
    }

    static void condition(boolean condition, String expression) throws RuleException {
        if (!condition) {
            throw new RuleException(
                    MessageFormat.format("Expected condition ''{0}'' to be satisfied.", expression));
        }
    }

    static void type(DataType expectedType, Item column) throws RuleException {
        if (!expectedType.name().equals(column.getType())) {
            throw new RuleException(
                    MessageFormat.format("Expected data type ''{0}'', but actual type is ''{1}''.",
                                         expectedType.name(),
                                         column.getType()));
        }
    }

    static void type(DataType expectedType, Array array) throws RuleException {
        if (array.getElementType() != expectedType.getPrimitiveClassType()) {
            throw new RuleException(
                    MessageFormat.format("Expected data type ''{0}'', but actual type is ''{1}''.",
                                         expectedType,
                                         array.getElementType().getSimpleName()));
        }
    }

    static void unit(String expectedUnit, Item column) throws RuleException {
        if (!expectedUnit.equals(column.getUnit())) {
            throw new RuleException(
                    MessageFormat.format("Expected unit ''{0}'', but actual unit is ''{1}''.",
                                         expectedUnit,
                                         column.getUnit()));
        }
    }
}
