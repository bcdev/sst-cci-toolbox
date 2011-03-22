package org.esa.cci.sst.subscene;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.SensorType;
import org.esa.cci.sst.orm.PersistenceManager;

/**
 * Implementation of {@link ProductSubsceneGenerator} for ATSR sensor.
 *
 * @author Thomas Storm
 */
class AtsrSubsceneGenerator extends ProductSubsceneGenerator {

    AtsrSubsceneGenerator(PersistenceManager persistenceManager) {
        super(persistenceManager, SensorType.ATSR.nameLowerCase());
    }

    @Override
    int getSensorDimensionSize() {
        return Constants.AATSR_LENGTH;
    }
}
