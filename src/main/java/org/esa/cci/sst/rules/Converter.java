package org.esa.cci.sst.rules;

/**
 * Used for for carrying out numerical conversions.
 *
 * @author Ralf Quast
 */
public interface Converter {

    /**
     * Applies the numerical conversion rule to the number supplied as argument.
     *
     * @param number A number.
     *
     * @return the converted number.
     *
     * @throws RuleException when the conversion rule cannot be applied.
     */
    Number apply(Number number) throws RuleException;
}
