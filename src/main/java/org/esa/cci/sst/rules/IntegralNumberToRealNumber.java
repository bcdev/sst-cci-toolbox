package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.DescriptorBuilder;
import ucar.ma2.DataType;

import java.text.MessageFormat;

/**
 * Base class some for numeric conversions.
 *
 * @author Ralf Quast
 */
public abstract class IntegralNumberToRealNumber implements Rule {

    @Override
    public Descriptor apply(Descriptor sourceDescriptor) throws RuleException {
        final DataType sourceDataType = getSourceDataType();
        Assert.condition(sourceDataType.isIntegral(),
                         MessageFormat.format("Expected integral numeric type, actual type is ''{0}''.",
                                              sourceDataType.name()));
        Assert.type(sourceDataType, sourceDescriptor);

        final DescriptorBuilder builder = new DescriptorBuilder(sourceDescriptor);
        builder.setAddOffset(null);
        builder.setScaleFactor(null);
        builder.setType(getTargetDataType());
        builder.setFillValue(null);

        final Number sourceFillValue = sourceDescriptor.getFillValue();
        if (sourceFillValue != null) {
            switch (sourceDataType) {
            case BYTE:
                builder.setFillValue(apply(sourceFillValue.byteValue(), sourceDescriptor));
                break;
            case SHORT:
                builder.setFillValue(apply(sourceFillValue.shortValue(), sourceDescriptor));
                break;
            case INT:
                builder.setFillValue(apply(sourceFillValue.intValue(), sourceDescriptor));
                break;
            case LONG:
                builder.setFillValue(apply(sourceFillValue.longValue(), sourceDescriptor));
                break;
            }
        }

        return builder.build();
    }

    @Override
    public Number apply(Number number, Descriptor sourceDescriptor) throws RuleException {
        Number sourceAddOffset = sourceDescriptor.getAddOffset();
        Number sourceScaleFactor = sourceDescriptor.getScaleFactor();
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
