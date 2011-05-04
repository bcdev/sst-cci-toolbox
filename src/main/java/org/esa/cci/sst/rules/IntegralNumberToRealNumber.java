package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.ColumnBuilder;
import ucar.ma2.DataType;

import java.text.MessageFormat;

/**
 * Base class some for numeric conversions.
 *
 * @author Ralf Quast
 */
public abstract class IntegralNumberToRealNumber implements Rule {

    @Override
    public Column apply(Column sourceColumn) throws RuleException {
        final DataType sourceDataType = getSourceDataType();
        Assert.condition(sourceDataType.isIntegral(),
                         MessageFormat.format("Expected integral numeric type, actual type is ''{0}''.",
                                              sourceDataType.name()));
        Assert.type(sourceDataType, sourceColumn);

        final ColumnBuilder builder = new ColumnBuilder(sourceColumn);
        builder.setAddOffset(null);
        builder.setScaleFactor(null);
        builder.setType(getTargetDataType());
        builder.setFillValue(null);

        final Number sourceFillValue = sourceColumn.getFillValue();
        if (sourceFillValue != null) {
            switch (sourceDataType) {
            case BYTE:
                builder.setFillValue(apply(sourceFillValue.byteValue(), sourceColumn));
                break;
            case SHORT:
                builder.setFillValue(apply(sourceFillValue.shortValue(), sourceColumn));
                break;
            case INT:
                builder.setFillValue(apply(sourceFillValue.intValue(), sourceColumn));
                break;
            case LONG:
                builder.setFillValue(apply(sourceFillValue.longValue(), sourceColumn));
                break;
            }
        }

        return builder.build();
    }

    @Override
    public Number apply(Number number, Column sourceColumn) throws RuleException {
        Number sourceAddOffset = sourceColumn.getAddOffset();
        Number sourceScaleFactor = sourceColumn.getScaleFactor();
        if (sourceScaleFactor == null) {
            sourceScaleFactor = 1.0;
        }
        if (sourceAddOffset == null) {
            sourceAddOffset = 0.0;
        }

        return computeTargetNumber(number, sourceAddOffset, sourceScaleFactor);
    }

    protected abstract DataType getTargetDataType();

    protected abstract DataType getSourceDataType();

    protected abstract Number computeTargetNumber(Number number,
                                                  Number sourceAddOffset,
                                                  Number sourceScaleFactor) throws RuleException;
}
