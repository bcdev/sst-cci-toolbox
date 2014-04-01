package org.esa.cci.sst.rules;

import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.common.ExtractDefinitionBuilder;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.Reader;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;

class InsituMohcId extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.INT;
    // package access for testing only tb 2014-03-13
    int[] historyShape;

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        targetColumnBuilder.type(DATA_TYPE);
        targetColumnBuilder.fillValue(Integer.MIN_VALUE);
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Context context = getContext();
        final ReferenceObservation referenceObservation = context.getMatchup().getRefObs();
        final Reader observationReader = context.getObservationReader();    // @todo 2 tb/** use referenceObservation reader tb 2014-03-12
        try {
            if (observationReader != null) {
                final ExtractDefinition extractDefinition = new ExtractDefinitionBuilder()
                        .shape(historyShape)
                        .referenceObservation(referenceObservation)
                        .build();
                return observationReader.read("insitu.mohc_id", extractDefinition);
            } else {
                throw new RuleException("Unable to read in-situ mohc_id");
            }
        } catch (IOException e) {
            throw new RuleException("Unable to read in-situ mohc_id", e);
        }
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);

        historyShape = InsituHelper.getShape(context);
    }
}
