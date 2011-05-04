package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.ColumnBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic class for rule tests.
 *
 * @author Ralf Quast
 */
public abstract class AbstractRuleTest {

    private Rule rule;
    private Column sourceColumn;

    @Before
    public final void init() {
        rule = RuleFactory.getInstance().getRule(getClass().getSimpleName().replace("Test", ""));
        sourceColumn = configureSourceColumnBuilder(new ColumnBuilder()).build();
    }

    @Test
    public final void testColumnConversion() throws RuleException {
        final Rule rule = getRule();
        final Column sourceColumn = getSourceColumn();
        final Column targetColumn = rule.apply(sourceColumn);

        assertTargetColumn(targetColumn);
    }

    @Test
    public abstract void testNumericConversion() throws RuleException;

    protected final Rule getRule() {
        return rule;
    }

    protected abstract void assertTargetColumn(Column targetColumn);

    protected abstract ColumnBuilder configureSourceColumnBuilder(ColumnBuilder columnBuilder);

    protected final Column getSourceColumn() {
        return sourceColumn;
    }

}
