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

/**
 * A rule is used for converting {@link Item} properties and for
 * carrying out a corresponding numerical conversion.
 *
 * @author Ralf Quast
 */
public abstract class Rule {

    private Context context;

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
    public abstract Item apply(Item sourceColumn) throws RuleException;

    /**
     * Applies the numerical conversion rule to the numbers supplied as argument.
     *
     * @param sourceArray  The numbers to be converted.
     * @param sourceColumn The column associated with the numbers supplied as
     *                     argument.
     *
     * @return the converted numbers.
     *
     * @throws RuleException when the rule cannot be applied.
     */
    public abstract Array apply(Array sourceArray, Item sourceColumn) throws RuleException;

    /**
     * Sets the rule's context.
     */
    public void setContext(Context context) {
        this.context = context;
    }

    protected Context getContext() {
        return context;
    }
}

