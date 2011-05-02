package org.esa.cci.sst.tools.ingestion;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.data.Timed;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.IOHandler;
import org.esa.cci.sst.reader.IOHandlerFactory;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.MmsTool;
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

    void persistVariableDescriptors(final String sensorName, final IOHandler ioHandler) throws IOException {
        final Descriptor[] variableDescriptors = ioHandler.getVariableDescriptors();
        getLogger().info(MessageFormat.format("Number of variables for sensor ''{0}'' = {1}.",
                                              sensorName, variableDescriptors.length));
        for (final Descriptor variableDescriptor : variableDescriptors) {
            getPersistenceManager().persist(variableDescriptor);
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
     * @param readerSpec The specification string for the reader.
     * @param sensorName The sensor name.
     * @param pattern    The sensor pattern.
     *
     * @throws ToolException if ingestion fails
     */
    private void ingest(File file, String readerSpec, String sensorName, String observationType, long pattern) throws
                                                                                                               ToolException {
        getLogger().info("ingesting file " + file.getName());
        final PersistenceManager persistenceManager = getPersistenceManager();
        final IOHandler ioHandler = getIOHandler(readerSpec, sensorName);
        try {
            // open database
            persistenceManager.transaction();

            Sensor sensor = getSensor(sensorName);
            boolean addVariables = false;
            if (sensor == null) {
                addVariables = true;
                sensor = createSensor(sensorName, observationType, pattern);
            }
            final DataFile dataFile = DataUtil.createDataFile(file, sensor);
            ioHandler.init(dataFile);

            persistenceManager.persist(dataFile);
            if (addVariables) {
                persistVariableDescriptors(sensorName, ioHandler);
            }

            int recordsInTimeInterval = persistObservations(sensorName, ioHandler);
            // make changes in database
            persistenceManager.commit();
            getLogger().info(MessageFormat.format("{0} {1} records in time interval.", sensorName,
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

    Sensor getSensor(final String sensorName) {
        return (Sensor) getPersistenceManager().pick("select s from Sensor s where s.name = ?1", sensorName);
    }

    Sensor createSensor(String sensorName, String observationType, long pattern) {
        Sensor sensor = DataUtil.createSensor(sensorName, observationType, pattern);
        getPersistenceManager().persist(sensor);
        return sensor;
    }

    private IOHandler getIOHandler(final String readerSpec, final String sensor) throws ToolException {
        final IOHandler ioHandler;
        try {
            ioHandler = IOHandlerFactory.createHandler(readerSpec, sensor);
        } catch (Exception e) {
            throw new ToolException(MessageFormat.format(
                    "Cannot create IO handler for sensor ''{0}''.", sensor), e,
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
        return ioHandler;
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
            final String inputDirPath = configuration.getProperty(
                    String.format("mms.source.%d.inputDirectory", i));
            final String sensor = configuration.getProperty(
                    String.format("mms.source.%d.sensor", i));
            final String readerSpec = configuration.getProperty(
                    String.format("mms.source.%d.reader", i));
            final String patternString = configuration.getProperty(
                    String.format("mms.source.%d.pattern", i), "0");
            final String observationType = configuration.getProperty(
                    String.format("mms.source.%d.observationType", i), "RelatedObservation");
            final long pattern = Long.parseLong(patternString, 16);
            if (readerSpec == null || inputDirPath == null || sensor == null) {
                continue;
            }
            getLogger().info("looking for " + sensor + " files");
            final String filenamePattern = configuration.getProperty(
                    String.format("mms.source.%d.filenamePattern", i), ".*");
            final File inputDir = new File(inputDirPath);
            final List<File> inputFileList = getInputFiles(filenamePattern, inputDir);
            if (inputFileList.isEmpty()) {
                getLogger().warning(MessageFormat.format("Missing directory ''{0}''.", inputDirPath));
            }
            for (final File inputFile : inputFileList) {
                ingest(inputFile, readerSpec, sensor, observationType, pattern);
                directoryCount++;
            }
        }
        validateInputSet(directoryCount);
        getLogger().info(MessageFormat.format("{0} input set(s) ingested.", directoryCount));
    }

    private int persistObservations(final String sensorName, final IOHandler ioHandler) {
        final PersistenceManager persistenceManager = getPersistenceManager();
        int recordsInTimeInterval = 0;

        // loop over records
        for (int recordNo = 0; recordNo < ioHandler.getNumRecords(); ++recordNo) {
            if (recordNo % 65536 == 0 && recordNo > 0) {
                getLogger().info(MessageFormat.format("Reading record {0} {1}.", sensorName, recordNo));
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
        statement = persistenceManager.createQuery("delete from Sensor s");
        statement.executeUpdate();
        statement = persistenceManager.createQuery("delete from Coincidence c");
        statement.executeUpdate();
        statement = persistenceManager.createQuery("delete from Matchup m");
        statement.executeUpdate();
//        try {
//            statement = persistenceManager.createNativeQuery("drop index geo");
//            statement.executeUpdate();
//        } catch (Exception e) {
//            System.err.format("geo index dropping failed: %s\n%s\n", e.toString(), "drop index geo");
//        }
//        try {
//            statement = persistenceManager.createNativeQuery("create index geo on mm_observation using gist(location)");
//            statement.executeUpdate();
//        } catch (Exception e) {
//            System.err.format("geo index creation failed: %s\n%s\n", e.toString(),
//                              "create index geo on mm_observation using gist(location)");
//        }
        persistenceManager.commit();
    }

    private void validateInputSet(final int directoryCount) throws ToolException {
        if (directoryCount == 0) {
            throw new ToolException("No input sets given.\n" +
                                    "Input sets are specified as configuration properties as follows:\n" +
                                    "\tmms.source.<i>.inputDirectory = <inputDirectory>\n" +
                                    "\tmms.source.<i>.filenamePattern = <filenamePattern> (opt)" +
                                    "\tmms.source.<i>.sensor = <sensor>\n" +
                                    "\tmms.source.<i>.reader = <ReaderClass>",
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    private List<File> getInputFiles(final String filenamePattern, final File inputDir) {
        final List<File> inputFileList = new ArrayList<File>(0);
        collectInputFiles(inputDir, filenamePattern, inputFileList);
        return inputFileList;
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
        final String startTime = getConfiguration().getProperty(Constants.PROPERTY_SOURCE_START_TIME);
        final String endTime = getConfiguration().getProperty(Constants.PROPERTY_SOURCE_END_TIME);
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
