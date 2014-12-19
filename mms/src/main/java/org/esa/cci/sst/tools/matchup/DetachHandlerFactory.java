package org.esa.cci.sst.tools.matchup;


import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tool.Configuration;

class DetachHandlerFactory {

    static DetachHandler create(Configuration configuration, PersistenceManager persistenceManager) {
        final boolean detachMatchups = configuration.getBooleanValue(Configuration.KEY_MMS_MMD_DETACH_MATCHUPS, false);
        if (detachMatchups) {
            return new DbDetachHandler(persistenceManager);
        }
        return new NoOpDetachHandler();
    }
}
