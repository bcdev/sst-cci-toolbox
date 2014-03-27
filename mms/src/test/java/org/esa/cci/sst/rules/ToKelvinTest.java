package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.tools.Constants;

import static org.junit.Assert.assertEquals;

/**
 * @author Ralf Quast
 */
public class ToKelvinTest extends AbstractRuleTest {

    @Override
    protected ColumnBuilder configureSourceColumn(ColumnBuilder columnBuilder) {
        return columnBuilder.unit("Celsius").addOffset(20.0);
    }

    @Override
    protected void assertTargetColumn(Item targetColumn) {
        assertEquals(Constants.UNIT_SEA_SURFACE_TEMPERATURE, targetColumn.getUnit());
        assertEquals(-253.15, targetColumn.getAddOffset().doubleValue(), 1.0e-10);
    }

    @Override
    public void testNumericConversion() throws RuleException {
    }
}
