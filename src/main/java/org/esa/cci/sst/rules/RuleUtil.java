package org.esa.cci.sst.rules;

import com.bc.ceres.core.Assert;

import java.text.MessageFormat;

/**
 * A utility class.
 *
 * @author Ralf Quast
 */
class RuleUtil {

    private RuleUtil() {
    }

    static String replaceDimension(String dimensionsString, String replacementDimension, int replacementIndex) {
        final String[] dimensions = dimensionsString.split(" ");
        Assert.state(dimensions.length >= replacementIndex);

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
