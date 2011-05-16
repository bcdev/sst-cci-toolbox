package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import ucar.ma2.Array;
import ucar.ma2.DataType;

/**
 * Abstract rescaling rule.
 *
 * @author Ralf Quast
 */
abstract class AbstractRescaling implements Rule {

    private final double targetAddOffset;
    private final double targetScaleFactor;

    protected AbstractRescaling(double targetScaleFactor, double targetAddOffset) {
        this.targetScaleFactor = targetScaleFactor;
        this.targetAddOffset = targetAddOffset;
    }

    @Override
    public final Item apply(Item sourceColumn) throws RuleException {
        final ColumnBuilder builder = new ColumnBuilder(sourceColumn);
        builder.addOffset(targetAddOffset);
        builder.scaleFactor(targetScaleFactor);
        builder.type(DataType.SHORT);
        builder.fillValue(Short.MIN_VALUE);
        configureTargetColumn(builder);

        return builder.build();
    }

    @Override
    public final Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array targetArray = Array.factory(DataType.SHORT, sourceArray.getShape());
        apply(sourceArray, targetArray,
              sourceColumn.getScaleFactor(),
              sourceColumn.getAddOffset(),
              sourceColumn.getFillValue());

        return targetArray;
    }

    protected abstract void configureTargetColumn(ColumnBuilder builder);

    private void apply(Array sourceArray, Array targetArray,
                       Number sourceScaleFactor,
                       Number sourceAddOffset,
                       Number sourceFillValue) {
        final double a = getDouble(sourceScaleFactor, 0.0);
        final double b = getDouble(sourceAddOffset, 0.0);
        for (int i = 0; i < sourceArray.getSize(); i++) {
            final double number = sourceArray.getDouble(i);
            if (isInvalid(number, sourceFillValue)) {
                targetArray.setShort(i, Short.MIN_VALUE);
            }
            targetArray.setShort(i, rescale(number, a, b));
        }
    }

    private boolean isInvalid(double d, Number fillValue) {
        return fillValue != null && d == fillValue.doubleValue() || Double.isNaN(d) || Double.isInfinite(d);
    }

    private short rescale(double d, double sourceScaleFactor, double sourceAddOffset) {
        return (short) (((sourceScaleFactor * d + sourceAddOffset) - targetAddOffset) / targetScaleFactor);
    }

    private double getDouble(Number number, double defaultValue) {
        if (number == null) {
            return defaultValue;
        }
        return number.floatValue();
    }
}
