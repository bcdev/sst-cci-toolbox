package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;
import ucar.ma2.DataType;

/**
 * Converts times (Julian Date) into seconds since 1978-01-01 00:00:00.
 */
class JulianDateToSeconds implements Rule {

    @Override
    public final VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException {
        RuleUtil.ensureType(DataType.DOUBLE.name(), sourceDescriptor.getType());
        RuleUtil.ensureUnit("Julian", sourceDescriptor.getUnit());
        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        targetDescriptor.setUnits("seconds since 1978-01-01 00:00:00");

        return targetDescriptor;
    }

    @Override
    public final Number apply(Number number) {
        return (number.doubleValue() - 2443509.5) * 86400.0;
    }
}
