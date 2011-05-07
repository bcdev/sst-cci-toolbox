package org.esa.cci.sst.rules;

import ucar.ma2.Array;
import ucar.ma2.DataType;

/**
 * abstract rule for converting number types to 'FLOAT'.
 *
 * @author Ralf Quast
 */
abstract class AbstractFloatFormat<S extends Number> extends AbstractFormat<S, Float> {

    protected AbstractFloatFormat(Class<S> sourceType) {
        super(sourceType, Float.class);
    }

    @Override
    protected final Float getTargetFillValue(Number sourceFillValue, Number scaleFactor, Number addOffset) {
        final float a = getFloat(scaleFactor, 1.0f);
        final float b = getFloat(addOffset, 0.0f);
        return apply(sourceFillValue.floatValue(), a, b);
    }

    @Override
    protected final void apply(Array sourceArray, Array targetArray, Number scaleFactor, Number addOffset) {
        final float a = getFloat(scaleFactor, 0.0f);
        final float b = getFloat(addOffset, 0.0f);
        for (int i = 0; i < sourceArray.getSize(); i++) {
            targetArray.setFloat(i, apply(sourceArray.getFloat(i), a, b));
        }
    }

    protected abstract float apply(float number, float scaleFactor, float addOffset);

    private float getFloat(Number number, float defaultValue) {
        if (number == null) {
            return defaultValue;
        }
        return number.floatValue();
    }
}
