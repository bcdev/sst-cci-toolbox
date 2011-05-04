package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Column;

import static org.junit.Assert.assertEquals;

public class MetopImageDimensionsTest extends AbstractRuleTest {

    @Override
    public void testNumericConversion() throws RuleException {
        // no numeric conversion is carried out for this rule
    }

    @Override
    protected void assertTargetColumn(Column targetColumn) {
        assertEquals("n metop.ni metop.nj", targetColumn.getDimensions());
    }

    @Override
    protected ColumnBuilder configureSourceColumnBuilder(ColumnBuilder columnBuilder) {
        return columnBuilder.setDimensions("n ny nx");
    }
}
