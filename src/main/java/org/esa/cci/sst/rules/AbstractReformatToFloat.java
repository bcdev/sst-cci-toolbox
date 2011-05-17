package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import ucar.ma2.Array;
import ucar.ma2.DataType;

/**
 * Abstract base class for numeric conversions into 'FLOAT'.
 *
 * @author Ralf Quast
 */
abstract class AbstractReformatToFloat<S extends Number> extends AbstractReformat<S, Float> {

    protected AbstractReformatToFloat(Class<S> sourceType) {
        super(sourceType, Float.class);
    }

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) {
        targetColumnBuilder.fillValue(getScaledNumber(sourceColumn, sourceColumn.getFillValue()));
        targetColumnBuilder.validMin(getScaledNumber(sourceColumn, sourceColumn.getValidMin()));
        targetColumnBuilder.validMax(getScaledNumber(sourceColumn, sourceColumn.getValidMax()));
    }

    @Override
    protected final void apply(Array sourceArray, Array targetArray, Number scaleFactor, Number addOffset) {
        final double a = getDouble(scaleFactor, 0.0);
        final double b = getDouble(addOffset, 0.0);
        for (int i = 0; i < sourceArray.getSize(); i++) {
            targetArray.setFloat(i, apply(sourceArray.getDouble(i), a, b));
        }
    }

    protected abstract float apply(double number, double scaleFactor, double addOffset);

    private Number getScaledNumber(Item sourceColumn, Number sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        if (sourceColumn.isUnsigned()) {
            switch (DataType.valueOf(sourceColumn.getType())) {
                case BYTE:
                    sourceValue = DataType.unsignedByteToShort(sourceValue.byteValue());
                    break;
                case SHORT:
                    sourceValue = DataType.unsignedShortToInt(sourceValue.shortValue());
                    break;
                case INT:
                    sourceValue = DataType.unsignedIntToLong(sourceValue.intValue());
            }
        }
        return getScaledNumber(sourceValue, sourceColumn.getScaleFactor(), sourceColumn.getAddOffset());
    }

    private Number getScaledNumber(Number sourceFillValue, Number scaleFactor, Number addOffset) {
        final double a = getDouble(scaleFactor, 1.0);
        final double b = getDouble(addOffset, 0.0);
        return apply(sourceFillValue.doubleValue(), a, b);
    }

    private double getDouble(Number number, double defaultValue) {
        if (number == null) {
            return defaultValue;
        }
        return number.doubleValue();
    }
}
