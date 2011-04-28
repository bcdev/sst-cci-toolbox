package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

/**
 * A rule is used for converting {@link VariableDescriptor} properties and for carrying out
 * the corresponding numerical conversion.
 *
 * @author Ralf Quast
 */
public interface Rule {

    /**
     * Applies the rule to the source descriptor supplied as arguments.
     *
     * @param sourceDescriptor The source descriptor.
     *
     * @return the target descriptor resulting from applying this rule to the
     *         source descriptor supplied as argument.
     *
     * @throws RuleException when the rule cannot be applied.
     */
    VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException;

    /**
     * Applies the numerical conversion rule to the number supplied as argument.
     *
     *
     *
     * @param number A number.
     *
     * @param targetDescriptor
     *@param sourceDescriptor @return the converted number. The number returned complies with the properties
     *         of the target descriptor returned by {@link #apply(VariableDescriptor)}.
     *
     * @throws RuleException when the rule cannot be applied.
     */
    Number apply(Number number, VariableDescriptor targetDescriptor, VariableDescriptor sourceDescriptor) throws
                                                                                                          RuleException;
}
