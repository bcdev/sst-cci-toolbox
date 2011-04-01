package org.esa.cci.sst;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DataSchema;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.IOHandler;
import org.esa.cci.sst.reader.IOHandlerFactory;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.io.File;
import java.io.FileFilter;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Tool to ingest new input files containing records of observations into the MMS database.
 *
 * @author Martin Boettcher
 * @author Norman Fomferra
 */
public class IngestionTool extends MmsTool {

    public static void main(String[] args) {
        // comment out the following two lines in order to activate the tool
        //System.out.println("The ingestion tool is deactivated in order to preserve the state of the database.");
        //System.exit(0);
        final IngestionTool tool = new IngestionTool();
        try {
            final boolean performWork = tool.setCommandLineArgs(args);
            if (!performWork) {
                return;
            }
            tool.initialize();
            tool.cleanup();
            tool.ingest();
        } catch (ToolException e) {
            tool.getErrorHandler().handleError(e, e.getMessage(), e.getExitCode());
        } catch (Throwable t) {
            tool.getErrorHandler().handleError(t, t.getMessage(), 1);
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
     * @see #ingest(java.io.File, String, String, org.esa.cci.sst.reader.IOHandler)
     */
    public void ingest() throws ToolException {
        final Properties configuration = getConfiguration();
        int directoryCount = 0;
        for (int i = 0; i < 100; i++) {
            final String schemaName = configuration.getProperty(
                    String.format("mms.test.inputSets.%d.schemaName", i));
            final String inputDirPath = configuration.getProperty(
                    String.format("mms.test.inputSets.%d.inputDirectory", i));
            final String sensorType = configuration.getProperty(
                    String.format("mms.test.inputSets.%d.sensorType", i));
            final String sensor = configuration.getProperty(
                    String.format("mms.test.inputSets.%d.sensor", i));
            if (schemaName == null || inputDirPath == null || sensorType == null || sensor == null) {
                continue;
            }
            if (!SensorType.isSensorType(sensorType)) {
                throw new ToolException(MessageFormat.format("Unknown sensor type ''{0}''.", sensorType),
                                        ToolException.TOOL_CONFIGURATION_ERROR);
            }
            final String filenamePattern = configuration.getProperty(
                    String.format("mms.test.inputSets.%d.filenamePattern", i), ".*");
            final File inputDir = new File(inputDirPath);
            final ArrayList<File> inputFileList = new ArrayList<File>(0);
            collectInputFiles(inputDir, filenamePattern, inputFileList);
            if (!inputFileList.isEmpty()) {
                try {
                    final IOHandler ioHandler = IOHandlerFactory.createHandler(schemaName, sensor);
                    for (final File inputFile : inputFileList) {
                        ingest(inputFile, schemaName, sensorType, ioHandler);
                    }
                } catch (IllegalArgumentException e) {
                    throw new ToolException(MessageFormat.format(
                            "Cannot create IO handler for schema ''{0}''.", schemaName), e,
                                            ToolException.TOOL_CONFIGURATION_ERROR);
                }
                directoryCount++;
            } else {
                getLogger().fine(MessageFormat.format("Missing directory ''{0}''.", inputDirPath));
            }
        }
        if (directoryCount == 0) {
            throw new ToolException("No input sets given.\n" +
                                    "Input sets are specified as configuration properties as follows:\n" +
                                    "\tmms.test.inputSets.<i>.schemaName = <schemaName>\n" +
                                    "\tmms.test.inputSets.<i>.inputDirectory = <inputDirectory>\n" +
                                    "\tmms.test.inputSets.<i>.filenamePattern = <filenamePattern> (opt)" +
                                    "\tmms.test.inputSets.<i>.sensor = <sensor>\n" +
                                    "\tmms.test.inputSets.<i>.sensorType = <sensorType>",
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
        getLogger().fine(MessageFormat.format("{0} input set(s) ingested.", directoryCount));
    }

    private void collectInputFiles(File inputDir, final String filenamePattern, List<File> inputFileList) {
        if (inputDir.isDirectory()) {
            final File[] inputFiles = inputDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isFile() && file.getName().matches(filenamePattern);
                }
            });
            Collections.addAll(inputFileList, inputFiles);
            final File[] subDirs = inputDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
            for (final File subDir : subDirs) {
                collectInputFiles(subDir, filenamePattern, inputFileList);
            }
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
     * @param file       The input file with records to be read and made persistent as observations.
     * @param schemaName The name of the input file type.
     * @param sensorType The type of sensor being the source of the the input data.
     * @param ioHandler  The handler to be used to read this file type
     *
     * @throws ToolException if ingestion fails
     */
    private void ingest(File file, String schemaName, String sensorType, IOHandler ioHandler) throws ToolException {
        final PersistenceManager persistenceManager = getPersistenceManager();
        try {
            // open database
            persistenceManager.transaction();

            // lookup or create data schema and data file entry
            DataSchema dataSchema = (DataSchema) persistenceManager.pick("select s from DataSchema s where s.name = ?1",
                                                                         schemaName);
            final boolean newDataSchema = (dataSchema == null);
            if (newDataSchema) {
                dataSchema = new DataSchema();
                dataSchema.setName(schemaName);
                dataSchema.setSensorType(sensorType);
                persistenceManager.persist(dataSchema);
            }

            final DataFile dataFile = new DataFile();
            dataFile.setPath(file.getPath());
            dataFile.setDataSchema(dataSchema);
            persistenceManager.persist(dataFile);

            ioHandler.init(dataFile);

            final VariableDescriptor[] variableDescriptors = ioHandler.getVariableDescriptors();
            getLogger().info(MessageFormat.format("Number of variables for schema ''{0}'' = {1}.",
                                                  schemaName, variableDescriptors.length));
            for (VariableDescriptor variableDescriptor : variableDescriptors) {
                // todo - check if descriptors have already been persisted (rq-20100401)
                persistenceManager.persist(variableDescriptor);
            }

            int recordsInTimeInterval = 0;

            // loop over records
            for (int recordNo = 0; recordNo < ioHandler.getNumRecords(); ++recordNo) {
                if (recordNo % 65536 == 0 && recordNo > 0) {
                    getLogger().info(MessageFormat.format("Reading record {0} {1}.", schemaName, recordNo));
                }
                try {
                    final Observation observation = ioHandler.readObservation(recordNo);
                    if (checkTime(observation)) {
                        ++recordsInTimeInterval;
                        try {
                            persistenceManager.persist(observation);
                        } catch (IllegalArgumentException e) {
                            final String message = MessageFormat.format("Observation {0} {1} is incomplete: {2}",
                                                                        schemaName,
                                                                        recordNo,
                                                                        e.getMessage());
                            getErrorHandler().handleWarning(e, message);
                        }
                    }
                } catch (Exception e) {
                    getLogger().fine(MessageFormat.format("Ignoring observation for record number {0}: {1}",
                                                          recordNo, e.getMessage()));
                }
                if (recordNo % 65536 == 65535) {
                    persistenceManager.commit();
                    persistenceManager.transaction();
                }
            }
            // make changes in database
            persistenceManager.commit();
            getLogger().info(MessageFormat.format("{0} {1} records in time interval.", schemaName,
                                                  recordsInTimeInterval));
        } catch (Exception e) {
            // do not make any change in case of errors
            try {
                persistenceManager.rollback();
            } catch (Exception ignored) {
                // ignored, because surrounding exception is propagated
            }
            getErrorHandler().handleWarning(e, MessageFormat.format("Failed to ingest file ''{0}''.", file));
        } finally {
            ioHandler.close();
        }
    }

    public void cleanup() throws ToolException {
        final PersistenceManager persistenceManager = getPersistenceManager();
        persistenceManager.transaction();
        Query delete = persistenceManager.createQuery("delete from DataFile f");
        delete.executeUpdate();
        delete = persistenceManager.createQuery("delete from Observation o");
        delete.executeUpdate();
        delete = persistenceManager.createQuery("delete from VariableDescriptor v");
        delete.executeUpdate();
        delete = persistenceManager.createQuery("delete from DataSchema s");
        delete.executeUpdate();
        delete = persistenceManager.createQuery("delete from Coincidence c");
        delete.executeUpdate();
        delete = persistenceManager.createQuery("delete from Matchup m");
        delete.executeUpdate();
        persistenceManager.commit();
    }

    private boolean checkTime(Observation observation) throws ParseException {
        final String startTime = getConfiguration().getProperty("mms.test.startTime");
        final String endTime = getConfiguration().getProperty("mms.test.endTime");
        final long start = TimeUtil.parseCcsdsUtcFormat(startTime);
        final long stop = TimeUtil.parseCcsdsUtcFormat(endTime);
        final long time = observation.getTime().getTime();
        final long timeRadius;
        if (observation instanceof InsituObservation) {
            timeRadius = ((InsituObservation) observation).getTimeRadius();
        } else {
            timeRadius = 0;
        }
        return time + timeRadius * 1000 >= start && time - timeRadius * 1000 < stop;
    }
}
