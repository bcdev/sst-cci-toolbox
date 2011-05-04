package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Column;

/**
 * A rule is used for converting {@link Column} properties and for
 * carrying out a corresponding numerical conversion.
 *
 * @author Ralf Quast
 */
public interface Rule {

    /**
     * Applies the rule to the source column supplied as arguments.
     *
     * @param sourceColumn The source column.
     *
     * @return the target column resulting from applying this rule
     *         to the source column supplied as argument.
     *
     * @throws RuleException when the rule cannot be applied.
     */
    Column apply(Column sourceColumn) throws RuleException;

    /**
     * Applies the numerical conversion rule to the number supplied as argument.
     * <p/>
     * Note that the target column can be obtained by applying this rule to the
     * source column applied as argument.
     *
     * @param number       A number.
     * @param sourceColumn A column of the number supplied as argument.
     *
     * @return the converted number.
     *
     * @throws RuleException when the rule cannot be applied.
     */
    Number apply(Number number, Column sourceColumn) throws RuleException;
}

