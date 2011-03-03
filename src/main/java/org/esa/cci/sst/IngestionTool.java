package org.esa.cci.sst;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DataSchema;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.ObservationReader;
import org.esa.cci.sst.reader.ReaderFactory;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * Tool to ingest new input files containing records of observations into the MMS database.
 *
 * @author Martin Boettcher
 * @author Norman Fomferra
 */
public class IngestionTool extends MmsTool {

    public static void main(String[] args) {
        IngestionTool ingestionTool = new IngestionTool();
        try {
            if (!ingestionTool.setCommandLineArgs(args)) {
                return;
            }
            ingestionTool.initialize();
            ingestionTool.cleanup();
            ingestionTool.ingest();
        } catch (ToolException e) {
            System.err.println("Error: " + e.getMessage());
            if (ingestionTool.isDebug()) {
                e.printStackTrace(System.err);
            }
            System.exit(e.getExitCode());
        }
    }

    public IngestionTool() {
        super("mmsingest.sh", "0.1");
    }

    /**
     * Ingests all input files and creates observation entries in the database
     * for all records contained in input file.
     *
     * @throws ToolException if an error occurs.
     * @see #ingest(java.io.File, String, org.esa.cci.sst.reader.ObservationReader)
     */
    public void ingest() throws ToolException {
        final Properties configuration = getConfiguration();
        int directoryCount = 0;
        for (int i = 0; i < 100; i++) {
            final String schemaName = configuration.getProperty(
                    String.format("mms.test.inputSets.%d.schemaName", i));
            final String dirPath = configuration.getProperty(
                    String.format("mms.test.inputSets.%d.inputDirectory", i));
            if (schemaName == null || dirPath == null) {
                continue;
            }
            final String filenamePattern = configuration.getProperty(
                    String.format("mms.test.inputSets.%d.filenamePattern", i), ".*");
            final File dir = new File(dirPath);
            final FileFilter fileFilter = new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().matches(filenamePattern);
                }
            };

            final File[] inputFiles = dir.listFiles(fileFilter);
            if (inputFiles == null) {
                printInfo(MessageFormat.format("missing directory ''{0}''.", dirPath));
            } else {
                ObservationReader reader;
                try {
                    reader = ReaderFactory.createReader(schemaName);
                } catch (Exception e) {
                    throw new ToolException("No reader for schema '" + schemaName + "' found.", 1, e);
                }
                for (File inputFile : inputFiles) {
                    ingest(inputFile, schemaName, reader);
                }
            }
            directoryCount++;
        }
        if (directoryCount <= 0) {
            throw new ToolException("No input sets given.\n" +
                                    "Input sets are specified as configuration properties as follows:\n" +
                                    "\tmms.test.inputSets.<i>.schemaName = <schemaName>\n" +
                                    "\tmms.test.inputSets.<i>.inputDirectory = <inputDirectory>\n" +
                                    "\tmms.test.inputSets.<i>.filenamePattern = <filenamePattern> (opt)", 1);

        }
        printInfo(directoryCount + " input set(s) ingested.");
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
     *
     * @throws Exception if ingestion fails
     */
    private void ingest(File matchupFile, String schemaName, ObservationReader reader) throws ToolException {
        final PersistenceManager persistenceManager = getPersistenceManager();
        try {
            // open database
            persistenceManager.transaction();

            // lookup or create data schema and data file entry
            DataSchema dataSchema = (DataSchema) persistenceManager.pick("select s from DataSchema s where s.name = ?1",
                                                                         schemaName);
            final boolean isNewDataSchema = dataSchema == null;
            if (isNewDataSchema) {
                dataSchema = new DataSchema();
                dataSchema.setName(schemaName);
                //dataSchema.setSensorType(sensorType);
                persistenceManager.persist(dataSchema);
            }

            DataFile dataFile = new DataFile();
            dataFile.setPath(matchupFile.getPath());
            dataFile.setDataSchema(dataSchema);
            persistenceManager.persist(dataFile);

            reader.init(dataFile);
            //System.out.printf("numberOfRecords=%d\n", reader.getNumRecords());

            if (isNewDataSchema) {
                final Variable[] variables = reader.getVariables();
                System.out.printf("number of variables for schema '%s' = %d%n", schemaName, variables.length);
                for (Variable variable : variables) {
                    persistenceManager.persist(variable);
                }
            }

            // TODO remove restriction regarding time interval, used only during initial tests
            final long start = TimeUtil.parseCcsdsUtcFormat("2010-06-01T00:00:00Z");
            final long stop = TimeUtil.parseCcsdsUtcFormat("2010-07-01T00:00:00Z");
            int recordsInTimeInterval = 0;

            // loop over records
            for (int recordNo = 0; recordNo < reader.getNumRecords(); ++recordNo) {
                if (recordNo % 65536 == 0 && recordNo > 0) {
                    System.out.printf("reading record %s %d\n", schemaName, recordNo);
                }

                final long time = reader.getTime(recordNo);
                if (time >= start && time < stop) {
                    ++recordsInTimeInterval;
                    try {
                        final Observation observation = reader.readObservation(recordNo);
                        if (observation != null) {
                            persistenceManager.persist(observation);
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.printf("%s %d observation incomplete\n", schemaName, recordNo);
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
                persistenceManager.rollback();
            } catch (Exception e2) {
                // ignored, because surrounding exception is propagated
            }
            throw new ToolException("Failed to ingest file " + matchupFile, 7, e);

        } finally {
            try {
                // close match-up file
                reader.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public void cleanup() throws ToolException {
        final PersistenceManager persistenceManager = getPersistenceManager();
        persistenceManager.transaction();
        Query delete = persistenceManager.createQuery("delete from DataFile f");
        delete.executeUpdate();
        delete = persistenceManager.createQuery("delete from Observation o");
        delete.executeUpdate();
        delete = persistenceManager.createQuery("delete from Variable v");
        delete.executeUpdate();
        delete = persistenceManager.createQuery("delete from DataSchema s");
        delete.executeUpdate();
        delete = persistenceManager.createQuery("delete from Coincidence c");
        delete.executeUpdate();
        delete = persistenceManager.createQuery("delete from Matchup m");
        delete.executeUpdate();
        persistenceManager.commit();
    }

}
