package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Column;
import ucar.ma2.DataType;

/**
 * Converts times (Julian Date) into seconds since 1978-01-01 00:00:00.
 */
final class JulianDateToSeconds implements Rule {

    @Override
    public Column apply(Column sourceColumn) throws RuleException {
        Assert.type(DataType.DOUBLE, sourceColumn);
        Assert.unit("Julian Date", sourceColumn);

        final ColumnBuilder builder = new ColumnBuilder(sourceColumn);
        builder.setUnit("seconds since 1978-01-01 00:00:00");
        final Number sourceFillValue = sourceColumn.getFillValue();
        if (sourceFillValue != null) {
            builder.setFillValue(apply(sourceFillValue.doubleValue(), sourceColumn));
        }

        return builder.build();
    }

    @Override
    public Double apply(Number number, Column sourceColumn) throws RuleException {
        Assert.condition(number instanceof Double, "number instanceof Double");

        return (number.doubleValue() - 2443509.5) * 86400.0;
    }
}
