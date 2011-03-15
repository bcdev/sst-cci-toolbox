package org.esa.cci.sst.util;

import com.bc.ceres.core.Assert;
import org.esa.cci.sst.orm.PersistenceManager;

/**
 * Abstract implementation of {@link SubsceneGeneratorTool.SubsceneGenerator}. Provides access to persistance manager.
 *
 * @author Thomas Storm
 */
abstract class AbstractSubsceneGenerator implements SubsceneGeneratorTool.SubsceneGenerator {

    private PersistenceManager persistenceManager;

    AbstractSubsceneGenerator(PersistenceManager persistenceManager) {
        Assert.argument(persistenceManager != null, "persistance manager must not be null");
        this.persistenceManager = persistenceManager;
    }

    PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    abstract int getSensorDimensionSize();
}
