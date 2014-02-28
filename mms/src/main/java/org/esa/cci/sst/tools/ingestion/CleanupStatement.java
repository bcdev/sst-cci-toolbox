package org.esa.cci.sst.tools.ingestion;

import org.esa.cci.sst.orm.PersistenceManager;

import javax.persistence.Query;
import java.util.Date;

class CleanupStatement {

    private final PersistenceManager persistenceManager;

    CleanupStatement(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    public void execute() {
        persistenceManager.transaction();
        Query statement = persistenceManager.createQuery("delete from DataFile f");
        statement.executeUpdate();

        // @todo 3 tb/tb extract statement class and replace in other code segments
        statement = persistenceManager.createQuery("delete from Observation o");
        statement.executeUpdate();

        statement = persistenceManager.createQuery("delete from Column c");
        statement.executeUpdate();

        statement = persistenceManager.createQuery("delete from Sensor s");
        statement.executeUpdate();

        // @todo 3 tb/tb extract statement class and replace in other code segments
        statement = persistenceManager.createQuery("delete from Coincidence c");
        statement.executeUpdate();

        // @todo 3 tb/tb extract statement class and replace in other code segments
        statement = persistenceManager.createQuery("delete from Matchup m");
        statement.executeUpdate();

        persistenceManager.commit();
    }

    public void executeForInterval(Date startTime, Date stopTime) {
        // @todo 1 tb/rq - we need to open the files to retrieve the sensor name ... do we really want to do this?? tb 2014-02-26
    }
}
