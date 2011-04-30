package org.esa.cci.sst.rules;

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

    static void type(String expectedType, String actualType) throws RuleException {
        if (!expectedType.equals(actualType)) {
            throw new RuleException(
                    MessageFormat.format("Expected variable type ''{0}'', but actual type is ''{1}''.",
                                         expectedType,
                                         actualType));
        }
    }

    static void unit(String expectedUnit, String actualUnit) throws RuleException {
        if (!actualUnit.contains(expectedUnit)) {
            throw new RuleException(
                    MessageFormat.format("Expected unit ''{0}'', but actual unit is ''{1}''.",
                                         expectedUnit,
                                         actualUnit));
        }
    }
}
