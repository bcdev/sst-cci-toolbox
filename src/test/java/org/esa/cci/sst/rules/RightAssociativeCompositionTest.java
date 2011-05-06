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

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.ColumnI;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RightAssociativeCompositionTest {

    @Test
    public void testAssociativity() throws RuleException {
        final Rule a = new ColumnModification() {

            @Override
            public ColumnI apply(ColumnI sourceColumn) {
                return new ColumnBuilder(sourceColumn).setName("a").build();
            }
        };
        final Rule b = new ColumnModification() {

            @Override
            public ColumnI apply(ColumnI sourceColumn) {
                return new ColumnBuilder(sourceColumn).setName("b").setRole("b").build();
            }
        };
        final Rule ab = new RightAssociativeComposition(a, b);
        final ColumnBuilder columnBuilder = new ColumnBuilder();
        final ColumnI sourceColumn = columnBuilder.build();
        final ColumnI targetColumn = ab.apply(sourceColumn);

        assertEquals("a", targetColumn.getName());
        assertEquals("b", targetColumn.getRole());
    }
}
