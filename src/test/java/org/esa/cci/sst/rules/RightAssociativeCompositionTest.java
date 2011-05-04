package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Column;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RightAssociativeCompositionTest {

    @Test
    public void testAssociativity() throws RuleException {
        final Rule a = new ColumnModification() {

            @Override
            public Column apply(Column sourceColumn) {
                return new ColumnBuilder(sourceColumn).setName("a").build();
            }
        };
        final Rule b = new ColumnModification() {

            @Override
            public Column apply(Column sourceColumn) {
                return new ColumnBuilder(sourceColumn).setName("b").setRole("b").build();
            }
        };
        final Rule ab = new RightAssociativeComposition(a, b);
        final ColumnBuilder columnBuilder = new ColumnBuilder();
        final Column sourceColumn = columnBuilder.build();
        final Column targetColumn = ab.apply(sourceColumn);

        assertEquals("a", targetColumn.getName());
        assertEquals("b", targetColumn.getRole());
    }
}
