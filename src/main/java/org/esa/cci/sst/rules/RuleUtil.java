package org.esa.cci.sst.rules;

import java.text.MessageFormat;

/**
 * A utility class.
 *
 * @author Ralf Quast
 */
class RuleUtil {

    private RuleUtil() {
    }

    static String replaceDimension(String dimensionsString, String replacementDimension, int replacementIndex) throws
                                                                                                               RuleException {
        final String[] dimensions = dimensionsString.split(" ");
        ensureDimensionCount(replacementIndex, dimensions);
        final StringBuilder dimensionsStringBuilder = new StringBuilder();
        for (int i = 0; i < replacementIndex; i++) {
            dimensionsStringBuilder.append(dimensions[i]);
            dimensionsStringBuilder.append(" ");
        }
        dimensionsStringBuilder.append(replacementDimension);
        for (int i = replacementIndex + 1; i < dimensions.length; i++) {
            dimensionsStringBuilder.append(" ");
            dimensionsStringBuilder.append(dimensions[i]);
        }
        return dimensionsStringBuilder.toString();
    }

    private static void ensureDimensionCount(int expectedCount, String[] dimensions) throws RuleException {
        if (dimensions.length < expectedCount) {
            throw new RuleException(
                    MessageFormat.format("Expected {0} dimensions, but actual number of dimensions is {1}.",
                                         expectedCount,
                                         dimensions.length));
        }
    }

    static void ensureType(String expectedType, String actualType) throws RuleException {
        if (!expectedType.equals(actualType)) {
            throw new RuleException(
                    MessageFormat.format("Expected variable type ''{0}'', but actual type is ''{1}''.", expectedType,
                                         actualType));
        }
    }

    static void ensureUnit(String expectedUnit, String actualUnit) throws RuleException {
        if (!actualUnit.contains(expectedUnit)) {
            throw new RuleException(
                    MessageFormat.format("Expected unit ''{0}'', but actual unit is ''{1}''.", expectedUnit,
                                         actualUnit));
        }
    }
}
