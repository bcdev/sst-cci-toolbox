package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import static org.junit.Assert.*;

public class ToReflectanceTest extends AbstractRuleTest {

    @Override
    protected ColumnBuilder configureSourceColumn(ColumnBuilder columnBuilder) {
        columnBuilder.type(DataType.SHORT);
        columnBuilder.addOffset(0.1);
        columnBuilder.scaleFactor(0.01);
        columnBuilder.validMin(0);
        columnBuilder.validMax(80);

        return columnBuilder;
    }

    @Override
    protected void assertTargetColumn(Item targetColumn) {
        assertTrue(DataType.SHORT.name().equals(targetColumn.getType()));
        assertEquals(0.0001, targetColumn.getScaleFactor().doubleValue(), 0.0);
        assertEquals(0.0, targetColumn.getAddOffset().doubleValue(), 0.0);
        assertEquals(Short.MIN_VALUE, targetColumn.getFillValue().shortValue());
        assertEquals(0, targetColumn.getValidMin().shortValue());
    }

    @Override
    public void testNumericConversion() throws RuleException {
        final Array sourceArray = Array.factory(DataType.SHORT, new int[]{2});
        sourceArray.setShort(0, (short) 10);
        sourceArray.setShort(1, (short) 70);

        final Array targetArray = getRule().apply(sourceArray, getSourceColumn());

        assertTrue(targetArray.getElementType() == short.class);
        assertEquals(2000, targetArray.getShort(0));
        assertEquals(8000, targetArray.getShort(1));
    }

}
