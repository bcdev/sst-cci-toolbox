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
 * Tool to ingest new input files containing records of observations into the MMS database.
 */
public class IngestionTool {

    /**
     * Name of persistence unit in META-INF/persistence.xml
     */
    private static final String PERSISTENCE_UNIT_NAME = "matchupdb";

    /**
     * JPA persistence entity manager
     */
    private PersistenceManager persistenceManager = new PersistenceManager(PERSISTENCE_UNIT_NAME);

    /**
     * Deletes observations, data files and data schemata from database
     *
     * @throws Exception if deletion fails
     */
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

    /**
     * Ingests one input file and creates observation entries in the database
     * for all records contained in input file. Further creates the data file
     * entry, and the schema entry unless it exists, <p>
     * <p/>
     * For METOP MD files two observations are created, one reference observation
     * with a single pixel coordinate and one common observation with a sub-scene.
     * This is achieved by using both factory methods of the reader, readRefObs
     * and readObservation. For other readers only one of them returns an
     * observation. <p>
     * <p/>
     * In order to avoid large transactions a database checkpoint is inserted
     * every 65536 records. If ingestion fails rollback is only performed to
     * the respective checkpoint.
     *
     * @param matchupFile input file with records to be read and made persistent as observations
     * @param schemaName  name of the file type
     * @param reader      The reader to be used to read this file type
     * @throws Exception if ingestion fails
     */
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

            // TODO remove restriction regarding time interval, used only during initial tests
            final long start = TimeUtil.parseCcsdsUtcFormat("2010-06-02T00:00:00Z");
            final long stop = TimeUtil.parseCcsdsUtcFormat("2010-06-03T00:00:00Z");
            int recordsInTimeInterval = 0;

            // loop over records
            for (int recordNo = 0; recordNo < reader.length(); ++recordNo) {
                if (recordNo % 65536 == 0 && recordNo > 0) {
                    System.out.printf("reading record %s %d\n", schemaName, recordNo);
                }

                final long time = reader.getTime(recordNo);
                if (time >= start && time < stop) {
                    ++recordsInTimeInterval;
                    try {
                        final Observation refObs = reader.readRefObs(recordNo);
                        if (refObs != null) {
                            persistenceManager.persist(refObs);
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.printf("%s %d reference pixel coordinate missing\n", schemaName, recordNo);
                    }
                    try {
                        final Observation observation = reader.readObservation(recordNo);
                        if (observation != null) {
                            persistenceManager.persist(observation);
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.printf("%s %d polygon coordinates incomplete\n", schemaName, recordNo);
                    }
                }
                if (recordNo % 65536 == 65535) {
                    persistenceManager.commit();
                    persistenceManager.transaction();
                }
            }

            // make changes in database
            persistenceManager.commit();

            System.out.printf("%d %s records in time interval\n", recordsInTimeInterval, schemaName);

        } catch (Exception e) {

            // do not make any change in case of errors
            try {
                if (persistenceManager != null) {
                    persistenceManager.rollback();
                }
            } catch (Exception _) {
            }
            throw e;

        } finally {

            // close match-up file
            reader.close();
        }
    }
}
