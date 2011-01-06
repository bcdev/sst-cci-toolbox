package org.esa.cci.sst;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DataSchema;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.ObservationReader;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.io.File;

/**
 * Tool to ingest new MD files into the MMS database.
 */
public class IngestionTool {

    static final String PERSISTENCE_UNIT_NAME = "matchupdb";

    PersistenceManager persistenceManager = new PersistenceManager(PERSISTENCE_UNIT_NAME);

    public void clearObservations() throws Exception {
        try {
            // open database
            persistenceManager.transaction();

            // clear observations as they are read from scratch
            Query delete = persistenceManager.createQuery("delete from Observation o");
            delete.executeUpdate();
            delete = persistenceManager.createQuery("delete from DataFile f");
            delete.executeUpdate();
            delete = persistenceManager.createQuery("delete from DataSchema s");
            delete.executeUpdate();

            persistenceManager.commit();

        } catch (Exception e) {

            // do not make any change in case of errors
            if (persistenceManager != null) {
                persistenceManager.rollback();
            }
            throw e;
        }
    }

    public void ingest(File matchupFile, String schemaName, ObservationReader reader) throws Exception {

        try {
            // open database
            persistenceManager.transaction();

            // lookup or create data schema and data file entry
            DataSchema dataSchema = (DataSchema) persistenceManager.pick("select s from DataSchema s where s.name = ?1", schemaName);
            if (dataSchema == null) {
                dataSchema = new DataSchema();
                dataSchema.setName(schemaName);
                //dataSchema.setSensorType(sensorType);
                persistenceManager.persist(dataSchema);
            }

            DataFile dataFile = new DataFile();
            dataFile.setPath(matchupFile.getPath());
            dataFile.setDataSchema(dataSchema);
            persistenceManager.persist(dataFile);

            reader.init(matchupFile, dataFile);
            System.out.printf("numberOfRecords=%d\n", reader.length());

            final long start = TimeUtil.parseCcsdsUtcFormat("2010-06-02T00:00:00Z");
            final long stop = TimeUtil.parseCcsdsUtcFormat("2010-06-03T00:00:00Z");
            // loop over records
            for (int recordNo = 0; recordNo < reader.length(); ++recordNo) {
                if (recordNo % 65536 == 0 && recordNo > 0) {
                    System.out.println(String.format("reading record %d", recordNo));
                }

                final long time = reader.getTime(recordNo);
                if (time >= start && time < stop) {
                    try {
                        final Observation refObs = reader.readRefObs(recordNo);
                        if (refObs != null) {
                            persistenceManager.persist(refObs);
                        }
                        final Observation observation = reader.readObservation(recordNo);
                        if (observation != null) {
                            persistenceManager.persist(observation);
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.printf("record %d contains fill value coordinate. skipped\n", recordNo);
                    }
                }
                if (recordNo % 65536 == 65535) {
                    persistenceManager.commit();
                    persistenceManager.transaction();
                }
            }

            // make changes in database
            persistenceManager.commit();

        } catch (Exception e) {

            // do not make any change in case of errors
            try {
                if (persistenceManager != null) {
                    persistenceManager.rollback();
                }
            } catch (Exception _) {}
            throw e;

        } finally {

            // close match-up file
            reader.close();
        }
    }
}
