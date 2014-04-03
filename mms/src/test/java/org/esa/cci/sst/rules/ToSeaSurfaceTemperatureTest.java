package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.tools.Constants;
import ucar.ma2.Array;

import static org.junit.Assert.assertEquals;

/**
 * @author Ralf Quast
 */
public class ToSeaSurfaceTemperatureTest extends AbstractRuleTest {

    @Override
    protected ColumnBuilder configureSourceColumn(ColumnBuilder columnBuilder) {
        return columnBuilder.fillValue(Short.MIN_VALUE);
    }

    @Override
    protected void assertTargetColumn(Item targetColumn) {
        assertEquals(Constants.UNIT_SEA_SURFACE_TEMPERATURE, targetColumn.getUnit());
        assertEquals(-22000, targetColumn.getValidMin().shortValue());
        assertEquals(31850, targetColumn.getValidMax().shortValue());
    }

    @Override
    public void testNumericConversion() throws RuleException {
        fromKelvinToKelvin();
        fromCelsiusToKelvin();
    }

    private void fromKelvinToKelvin() throws RuleException {
        final Array sourceArray;
        final Array targetArray;

        sourceArray = Array.factory(new float[]{Short.MIN_VALUE, 0.f, 293.15f, 303.15f});
        targetArray = getRule().apply(sourceArray, getSourceColumn());

        assertEquals(-32768, targetArray.getShort(0));
        assertEquals(-32768, targetArray.getShort(1));
        assertEquals(0, targetArray.getShort(2));
        assertEquals(10000, targetArray.getShort(3));
    }

    private void fromCelsiusToKelvin() throws RuleException {
        final Rule celsiusToKelvin = new CelsiusToKelvin();
        final Array sourceArray;
        final Array targetArray;
        sourceArray = Array.factory(new float[]{Short.MIN_VALUE, -273.15f, 20.0f, 30.0f});
        targetArray = getRule().apply(sourceArray, celsiusToKelvin.apply(getSourceColumn()));

        assertEquals(-32768, targetArray.getShort(0));
        assertEquals(-32768, targetArray.getShort(1));
        assertEquals(0, targetArray.getShort(2));
        assertEquals(10000, targetArray.getShort(3));
    }
}
