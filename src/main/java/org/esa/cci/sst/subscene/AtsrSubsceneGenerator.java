package org.esa.cci.sst.subscene;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.SensorName;
import org.esa.cci.sst.orm.PersistenceManager;

/**
 * Implementation of {@link ProductSubsceneGenerator} for ATSR sensor.
 *
 * @author Thomas Storm
 */
class AtsrSubsceneGenerator extends ProductSubsceneGenerator {

    AtsrSubsceneGenerator(PersistenceManager persistenceManager) {
        super(persistenceManager, SensorName.SENSOR_NAME_AATSR.getSensor());
    }

    @Override
    int getSensorDimensionSize() {
        return Constants.AATSR_LENGTH;
    }
}
