package org.esa.cci.sst.util;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.SensorName;
import org.esa.cci.sst.orm.PersistenceManager;

/**
 * Implementation of {@link ProductSubsceneGenerator} for AMSRE data.
 *
 * @author Thomas Storm
 */
class AmsreSubsceneGenerator extends ProductSubsceneGenerator {

    AmsreSubsceneGenerator(PersistenceManager persistenceManager) {
        super(persistenceManager, SensorName.SENSOR_NAME_AMSRE.getSensor());
    }

    @Override
    int getSensorDimensionSize() {
        return Constants.AMSRE_LENGTH;
    }

}
