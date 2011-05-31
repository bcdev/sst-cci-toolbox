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

import ucar.ma2.Array;

/**
 * Used for for carrying out numerical conversions.
 *
 * @author Ralf Quast
 */
public interface Converter {

    /**
     * Applies the numerical conversion rule to the numbers supplied as argument.
     *
     * @param numbers The numbers to be converted.
     *
     * @return the converted numbers.
     *
     * @throws RuleException when the conversion rule cannot be applied.
     */
    Array apply(Array numbers) throws RuleException;

    void setContext(Context context);
}
