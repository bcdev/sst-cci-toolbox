package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.tools.Constants;

/**
 * @author Ralf Quast
 */
public class ToKelvin extends AbstractAttributeModification {

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        final Number addOffset = sourceColumn.getAddOffset();
        if (addOffset == null) {
            targetColumnBuilder.
                    unit(Constants.UNIT_SEA_SURFACE_TEMPERATURE).
                    addOffset(-273.15);
        } else {
            targetColumnBuilder.
                    unit(Constants.UNIT_SEA_SURFACE_TEMPERATURE).
                    addOffset(sourceColumn.getAddOffset().doubleValue() - 273.15);
        }
    }
}
