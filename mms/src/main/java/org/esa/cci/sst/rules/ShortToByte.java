package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;

/**
 * @author Ralf Quast
 */
final class ShortToByte extends AbstractReformat<Short, Byte> {

    protected ShortToByte() {
        super(Short.class, Byte.class);
    }

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) {
        targetColumnBuilder.type(DataType.BYTE);
    }

    @Override
    protected void apply(Array sourceArray, Array targetArray, Number scaleFactor, Number addOffset) {
        final IndexIterator sourceIterator = sourceArray.getIndexIterator();
        final IndexIterator targetIterator = targetArray.getIndexIterator();
        while (sourceIterator.hasNext() && targetIterator.hasNext()) {
            targetIterator.setByteNext(sourceIterator.getByteNext());
        }
    }
}
