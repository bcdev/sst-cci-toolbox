package org.esa.cci.sst.rules;

import ucar.ma2.Array;

/**
 * Rule for converting number types to 'FLOAT'.
 *
 * @author Ralf Quast
 */
class FloatFormat<S extends Number> extends AbstractFormat<S, Float> {

    protected FloatFormat(Class<S> sourceType) {
        super(sourceType, Float.class);
    }

    @Override
    protected final Number getTargetFillValue(Number sourceFillValue, Number addOffset, Number scaleFactor) {
        final float a = getFloat(scaleFactor, 1.0f);
        final float b = getFloat(addOffset, 0.0f);
        return a * sourceFillValue.floatValue() + b;
    }

    @Override
    protected final void apply(Array sourceArray, Array targetArray, Number addOffset, Number scaleFactor) {
        final float a = getFloat(scaleFactor, 0.0f);
        final float b = getFloat(addOffset, 0.0f);
        for (int i = 0; i < sourceArray.getSize(); i++) {
            targetArray.setFloat(i, a * sourceArray.getFloat(i) + b);
        }
    }

    private float getFloat(Number number, float defaultValue) {
        if (number == null) {
            return defaultValue;
        }
        return number.floatValue();
    }
}
