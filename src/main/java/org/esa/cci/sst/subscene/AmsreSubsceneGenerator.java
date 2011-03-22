package org.esa.cci.sst.subscene;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.SensorType;
import org.esa.cci.sst.orm.PersistenceManager;

/**
 * Implementation of {@link ProductSubsceneGenerator} for AMSRE data.
 *
 * @author Thomas Storm
 */
class AmsreSubsceneGenerator extends ProductSubsceneGenerator {

    AmsreSubsceneGenerator(PersistenceManager persistenceManager) {
        super(persistenceManager, SensorType.AMSRE.getSensor());
    }

    @Override
    int getSensorDimensionSize() {
        return Constants.AMSRE_LENGTH;
    }

}
