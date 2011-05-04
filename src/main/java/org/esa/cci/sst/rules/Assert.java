package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Column;
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

    static void type(DataType expectedType, Column column) throws RuleException {
        if (!expectedType.name().equals(column.getType())) {
            throw new RuleException(
                    MessageFormat.format("Expected variable type ''{0}'', but actual type is ''{1}''.",
                                         expectedType,
                                         column.getType()));
        }
    }

    static void unit(String expectedUnit, Column column) throws RuleException {
        if (!expectedUnit.equals(column.getUnit())) {
            throw new RuleException(
                    MessageFormat.format("Expected unit ''{0}'', but actual unit is ''{1}''.",
                                         expectedUnit,
                                         column.getUnit()));
        }
    }
}
