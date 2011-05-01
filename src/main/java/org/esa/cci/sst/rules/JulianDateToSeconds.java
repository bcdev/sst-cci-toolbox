package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.VariableDescriptor;
import ucar.ma2.DataType;

/**
 * Converts times (Julian Date) into seconds since 1978-01-01 00:00:00.
 */
final class JulianDateToSeconds implements Rule {

    @Override
    public Descriptor apply(Descriptor sourceDescriptor) throws RuleException {
        Assert.type(DataType.DOUBLE, sourceDescriptor);
        Assert.unit("Julian Date", sourceDescriptor);

        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        targetDescriptor.setUnit("seconds since 1978-01-01 00:00:00");
        final Number sourceFillValue = sourceDescriptor.getFillValue();
        if (sourceFillValue != null) {
            targetDescriptor.setFillValue(apply(sourceFillValue.doubleValue(), sourceDescriptor));
        }

        return targetDescriptor;
    }

    @Override
    public Double apply(Number number, Descriptor sourceDescriptor) throws RuleException {
        Assert.condition(number instanceof Double, "number instanceof Double");

        return (number.doubleValue() - 2443509.5) * 86400.0;
    }
}
