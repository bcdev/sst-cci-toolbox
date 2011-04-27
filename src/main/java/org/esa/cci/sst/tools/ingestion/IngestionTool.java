package org.esa.cci.sst.tools.ingestion;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DataSchema;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Timed;
import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.IOHandler;
import org.esa.cci.sst.reader.IOHandlerFactory;
import org.esa.cci.sst.tools.MmsTool;
import org.esa.cci.sst.tools.SensorType;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.DataUtil;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
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

    public static final String ALL_SENSORS_QUERY = "select sensor from mm_observation group by sensor";

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
            final boolean doCleanup = Boolean.parseBoolean(tool.getConfiguration().getProperty("mms.initialcleanup"));
            if (doCleanup) {
                tool.cleanup();
            }
            tool.ingest();
        } catch (ToolException e) {
            tool.getErrorHandler().handleError(e, e.getMessage(), e.getExitCode());
        } catch (Throwable t) {
            tool.getErrorHandler().handleError(t, t.getMessage(), 1);
        }
    }

    IngestionTool() {
        super("mmsingest.sh", "0.1");
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
     * @param sensorName The sensor name.
     *
     * @throws ToolException if ingestion fails
     */
    void ingest(File file, String schemaName, String sensorType, String sensorName) throws ToolException {
        getLogger().info("ingesting file " + file.getName());
        final PersistenceManager persistenceManager = getPersistenceManager();
        final IOHandler ioHandler = getIOHandler(schemaName, sensorName);
        try {
            // open database
            persistenceManager.transaction();
            DataSchema dataSchema = getDataSchema(schemaName, sensorType);

            final DataFile dataFile = DataUtil.createDataFile(file, dataSchema);
            ioHandler.init(dataFile);

            persistenceManager.persist(dataFile);
            persistVariableDescriptors(schemaName, sensorName, ioHandler);

            int recordsInTimeInterval = persistObservations(schemaName, ioHandler);
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

    boolean persistObservation(final Observation observation, final int recordNo) throws IOException,
                                                                                     ParseException {
        boolean hasPersisted = false;
        final PersistenceManager persistenceManager = getPersistenceManager();
        if (checkTime(observation)) {
            try {
                persistenceManager.persist(observation);
                hasPersisted = true;
            } catch (IllegalArgumentException e) {
                final String message = MessageFormat.format("Observation {0} {1} is incomplete: {2}",
                                                            observation.getName(),
                                                            recordNo,
                                                            e.getMessage());
                getErrorHandler().handleWarning(e, message);
            }
        }
        return hasPersisted;
    }

    IOHandler getIOHandler(final String schemaName, final String sensor) throws ToolException {
        final IOHandler ioHandler;
        try {
            ioHandler = IOHandlerFactory.createHandler(this, schemaName, sensor);
        } catch (IllegalArgumentException e) {
            throw new ToolException(MessageFormat.format(
                    "Cannot create IO handler for schema ''{0}''.", schemaName), e,
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
        return ioHandler;
    }

    @SuppressWarnings({"unchecked"})
    void persistVariableDescriptors(final String schemaName, final String sensorName,
                                    final IOHandler ioHandler) throws IOException {
        final Query query = getPersistenceManager().createNativeQuery(ALL_SENSORS_QUERY, String.class);
        final List<String> sensorList = query.getResultList();

        if (!sensorList.contains(sensorName)) {
            final VariableDescriptor[] variableDescriptors = ioHandler.getVariableDescriptors();
            getLogger().info(MessageFormat.format("Number of variables for schema ''{0}'' = {1}.",
                                                  schemaName, variableDescriptors.length));
            for (final VariableDescriptor variableDescriptor : variableDescriptors) {
                getPersistenceManager().persist(variableDescriptor);
            }
        }
    }

    /**
     * Ingests all input files and creates observation entries in the database
     * for all records contained in input file.
     *
     * @throws ToolException if an error occurs.
     */
    private void ingest() throws ToolException {
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
            validateSensorType(sensorType);
            getLogger().info("looking for " + sensor + " files");
            final String filenamePattern = configuration.getProperty(
                    String.format("mms.test.inputSets.%d.filenamePattern", i), ".*");
            final File inputDir = new File(inputDirPath);
            final List<File> inputFileList = getInputFiles(filenamePattern, inputDir);
            if (inputFileList.isEmpty()) {
                getLogger().warning(MessageFormat.format("Missing directory ''{0}''.", inputDirPath));
            }
            for (final File inputFile : inputFileList) {
                ingest(inputFile, schemaName, sensorType, sensor);
                directoryCount++;
            }
        }
        validateInputSet(directoryCount);
        getLogger().info(MessageFormat.format("{0} input set(s) ingested.", directoryCount));
    }

    private int persistObservations(final String schemaName, final IOHandler ioHandler) {
        final PersistenceManager persistenceManager = getPersistenceManager();
        int recordsInTimeInterval = 0;

        // loop over records
        for (int recordNo = 0; recordNo < ioHandler.getNumRecords(); ++recordNo) {
            if (recordNo % 65536 == 0 && recordNo > 0) {
                getLogger().info(MessageFormat.format("Reading record {0} {1}.", schemaName, recordNo));
            }
            try {
                final Observation observation = ioHandler.readObservation(recordNo);
                if (persistObservation(observation, recordNo)) {
                    recordsInTimeInterval++;
                }
            } catch (Exception e) {
                getLogger().warning(MessageFormat.format("Ignoring observation for record number {0}: {1}",
                                                         recordNo, e.getMessage()));
            }
            if (recordNo % 65536 == 65535) {
                persistenceManager.commit();
                persistenceManager.transaction();
            }
        }
        return recordsInTimeInterval;
    }

    private void cleanup() throws ToolException {
        getLogger().info("cleaning up database");
        final PersistenceManager persistenceManager = getPersistenceManager();
        persistenceManager.transaction();
        Query statement = persistenceManager.createQuery("delete from DataFile f");
        statement.executeUpdate();
        statement = persistenceManager.createQuery("delete from Observation o");
        statement.executeUpdate();
        statement = persistenceManager.createQuery("delete from VariableDescriptor v");
        statement.executeUpdate();
        statement = persistenceManager.createQuery("delete from DataSchema s");
        statement.executeUpdate();
        statement = persistenceManager.createQuery("delete from Coincidence c");
        statement.executeUpdate();
        statement = persistenceManager.createQuery("delete from Matchup m");
        statement.executeUpdate();
        try {
            statement = persistenceManager.createNativeQuery("drop index geo");
            statement.executeUpdate();
        } catch (Exception _) {}
        statement = persistenceManager.createNativeQuery("create index geo on mm_observation using gist(location)");
        statement.executeUpdate();
        persistenceManager.commit();
    }

    private DataSchema getDataSchema(final String schemaName, final String sensorType) {
        // lookup or create data schema and data file entry
        DataSchema dataSchema = (DataSchema) getPersistenceManager().pick(
                "select s from DataSchema s where s.name = ?1",
                schemaName);
        if (dataSchema == null) {
            dataSchema = DataUtil.createDataSchema(schemaName, sensorType);
            getPersistenceManager().persist(dataSchema);
        }
        return dataSchema;
    }

    private void validateInputSet(final int directoryCount) throws ToolException {
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
    }

    private List<File> getInputFiles(final String filenamePattern, final File inputDir) {
        final List<File> inputFileList = new ArrayList<File>(0);
        collectInputFiles(inputDir, filenamePattern, inputFileList);
        return inputFileList;
    }

    private void validateSensorType(final String sensorType) throws ToolException {
        if (!SensorType.isSensorType(sensorType)) {
            throw new ToolException(MessageFormat.format("Unknown sensor type ''{0}''.", sensorType),
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
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

    private boolean checkTime(Observation observation) throws ParseException {
        final String startTime = getConfiguration().getProperty("mms.test.startTime");
        final String endTime = getConfiguration().getProperty("mms.test.endTime");
        final long start = TimeUtil.parseCcsdsUtcFormat(startTime);
        final long stop = TimeUtil.parseCcsdsUtcFormat(endTime);
        if (!(observation instanceof Timed)) {
            return true;
        }
        final long time = ((Timed) observation).getTime().getTime();
        final long timeRadius;
        if (observation instanceof InsituObservation) {
            timeRadius = ((InsituObservation) observation).getTimeRadius();
        } else {
            timeRadius = 0;
        }
        return time + timeRadius * 1000 >= start && time - timeRadius * 1000 < stop;
    }
}
