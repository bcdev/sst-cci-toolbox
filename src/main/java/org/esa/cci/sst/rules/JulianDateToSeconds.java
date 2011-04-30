package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;
import ucar.ma2.DataType;

/**
 * Converts times (Julian Date) into seconds since 1978-01-01 00:00:00.
 */
final class JulianDateToSeconds implements Rule {

    @Override
    public final VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException {
        Assert.type(DataType.DOUBLE.name(), sourceDescriptor.getType());
        Assert.unit("Julian", sourceDescriptor.getUnit());
        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        targetDescriptor.setUnit("seconds since 1978-01-01 00:00:00");

        return targetDescriptor;
    }

    @Override
    public final Double apply(Number number, final VariableDescriptor sourceDescriptor) {
        return (number.doubleValue() - 2443509.5) * 86400.0;
    }
}
