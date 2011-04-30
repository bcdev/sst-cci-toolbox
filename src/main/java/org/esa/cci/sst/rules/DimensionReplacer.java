package org.esa.cci.sst.rules;

import java.text.MessageFormat;

/**
 * Used for dimension replacing rules.
 *
 * @author Ralf Quast
 */
final class DimensionReplacer {

    private final String[] dimensions;

    DimensionReplacer(String dimensionsString) {
        dimensions = dimensionsString.split("\\s+");
    }

    DimensionReplacer replace(int i, String dimension) throws RuleException {
        if (dimensions.length < i + 1) {
            throw new RuleException(
                    MessageFormat.format("Expected {0} or more dimensions, but actual number of dimensions is {1}.",
                                         i + 1,
                                         dimensions.length));
        }
        dimensions[i] = dimension;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final String dimension : dimensions) {
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(dimension);
        }
        return sb.toString();
    }
}
