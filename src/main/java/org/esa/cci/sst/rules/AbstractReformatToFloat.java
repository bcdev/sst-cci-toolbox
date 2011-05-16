package org.esa.cci.sst.rules;

import ucar.ma2.Array;

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
    protected final Float getTargetFillValue(Number sourceFillValue, Number scaleFactor, Number addOffset) {
        final double a = getDouble(scaleFactor, 1.0);
        final double b = getDouble(addOffset, 0.0);
        return apply(sourceFillValue.doubleValue(), a, b);
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

    private double getDouble(Number number, double defaultValue) {
        if (number == null) {
            return defaultValue;
        }
        return number.doubleValue();
    }
}
