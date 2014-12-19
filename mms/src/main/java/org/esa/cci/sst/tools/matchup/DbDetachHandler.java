package org.esa.cci.sst.tools.matchup;

import org.esa.cci.sst.orm.PersistenceManager;

class DbDetachHandler implements DetachHandler {

    private final PersistenceManager persistenceManager;

    DbDetachHandler(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    public void detach(Object toDetach) {
        persistenceManager.detach(toDetach);
    }
}
