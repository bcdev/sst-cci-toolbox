package org.esa.cci.sst;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DataSchema;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.AatsrMatchupReader;
import org.esa.cci.sst.reader.MetopMatchupReader;
import org.esa.cci.sst.reader.ObservationReader;
import org.esa.cci.sst.reader.SeviriMatchupReader;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Tool to ingest new input files containing records of observations into the MMS database.
 *
 * @author Martin Boettcher
 * @author Norman Fomferra
 */
public class IngestionTool {

    /**
     * Name of persistence unit in META-INF/persistence.xml
     */
    private static final String PERSISTENCE_UNIT_NAME = "matchupdb";

    /**
     * JPA persistence entity manager
     */
    private PersistenceManager persistenceManager;

    /**
     * Debug mode?
     */
    private boolean debug;

    /**
     * Verbose mode?
     */
    private boolean verbose;

    /**
     * The list of input files to be ingested.
     */
    private File[] inputFiles;

    /**
     * Data schema name.
     */
    private String schemaName;

    private Properties configuration;
    private Options options;
    private boolean initialised;

    public static void main(String[] args) {
        IngestionTool ingestionTool = new IngestionTool();
        try {
            if (ingestionTool.setCommandLineArgs(args)) {
                ingestionTool.ingest();
            }
        } catch (ToolException e) {
            System.err.println("Error: " + e.getMessage());
            if (ingestionTool.isDebug()) {
                e.printStackTrace(System.err);
            }
            System.exit(e.getExitCode());
        }
    }

    public IngestionTool() {
        options = createCommandLineOptions();

        configuration = new Properties();
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            if (entry.getKey().toString().startsWith("mms.")) {
                configuration.put(entry.getKey(), entry.getValue());
            }
        }

        inputFiles = new File[0];
    }

    public File[] getInputFiles() {
        return inputFiles;
    }

    public void setInputFiles(File[] inputFiles) {
        this.inputFiles = inputFiles.clone();
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }


    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public Properties getConfiguration() {
        return new Properties(configuration);
    }

    /**
     * Deletes observations, data files and data schemata from database
     *
     * @throws ToolException if deletion fails
     */
    public void clearObservations() throws ToolException {
        initialize();
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
            persistenceManager.rollback();
            throw new ToolException("Failed to clear observations: " + e.getMessage(), 6, e);
        }
    }

    /**
     * Ingests all input files and creates observation entries in the database
     * for all records contained in input file.
     *
     * @throws ToolException if an error occurs.
     * @see #ingest(java.io.File, String, org.esa.cci.sst.reader.ObservationReader)
     */
    public void ingest() throws ToolException {
        initialize();
        checkInputFilesAreAvailable();
        ObservationReader reader = createReader(schemaName);
        for (File inputFile : inputFiles) {
            ingest(inputFile, schemaName, reader);
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
    public void ingest(File matchupFile, String schemaName, ObservationReader reader) throws ToolException {
        initialize();
        checkInputFilesAreAvailable();
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
            System.out.printf("numberOfRecords=%d\n", reader.getNumRecords());

            // TODO remove restriction regarding time interval, used only during initial tests
            final long start = TimeUtil.parseCcsdsUtcFormat("2010-06-02T00:00:00Z");
            final long stop = TimeUtil.parseCcsdsUtcFormat("2010-06-03T00:00:00Z");
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

    private void initialize() throws ToolException {
        if (initialised) {
            return;
        }

        initialised = true;

        persistenceManager = new PersistenceManager(PERSISTENCE_UNIT_NAME, configuration);
    }

    private void checkInputFilesAreAvailable() throws ToolException {
        if (inputFiles.length == 0) {
            throw new ToolException("No input file(s) specified. Use option -help to print usage.", 1);
        }
        for (File inputFile1 : inputFiles) {
            printInfo(MessageFormat.format("Checking input file {0}", inputFile1));
            if (!inputFile1.exists()) {
                throw new ToolException(MessageFormat.format("File not found {0}", inputFile1), 2);
            }
        }
    }

    static ObservationReader createReader(String schemaName) throws ToolException {
        // todo - get reader plugin from from registration
        ObservationReader reader;
        if (schemaName.equalsIgnoreCase("aatsr")) {
            reader = new AatsrMatchupReader();
        } else if (schemaName.equalsIgnoreCase("metop")) {
            reader = new MetopMatchupReader();
        } else if (schemaName.equalsIgnoreCase("seviri")) {
            reader = new SeviriMatchupReader();
        } else {
            throw new ToolException(MessageFormat.format("No appropriate reader for schema {0} found", schemaName), 8);
        }
        return reader;
    }

    boolean setCommandLineArgs(String[] args) throws ToolException {

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            setDebug(cmd.hasOption("debug"));
            setVerbose(cmd.hasOption("verbose"));

            if (cmd.hasOption("version")) {
                printVersion();
                return false;
            }

            if (cmd.hasOption("help")) {
                printHelp("");
                return false;
            }

            File configurationFile = (File) cmd.getParsedOptionValue("conf");
            if (configurationFile != null) {
                addConfigurationProperties(configurationFile);
            }
            Properties optionProperties = cmd.getOptionProperties("D");
            if (optionProperties.size() > 0) {
                addConfigurationProperties(optionProperties);
            }

            schemaName = cmd.getOptionValue("schema", "[auto-detect]");

            List inputFileList = cmd.getArgList();
            File[] inputFiles = new File[inputFileList.size()];
            for (int i = 0; i < inputFileList.size(); i++) {
                inputFiles[i] = new File(inputFileList.get(i).toString());
            }
            setInputFiles(inputFiles);

        } catch (ParseException e) {
            throw new ToolException(e.getMessage(), 4, e);
        }

        return true;
    }

    private static Options createCommandLineOptions() {
        final Option helpOpt = new Option("help", "print this message");
        final Option versionOpt = new Option("version", "print the version information and exit");
        final Option verboseOpt = new Option("verbose", "be extra verbose");
        final Option debugOpt = new Option("debug", "print debugging information");

        final Option confFileOpt = new Option("conf", "alternate configuration file");
        confFileOpt.setArgs(1);
        confFileOpt.setArgName("file");
        confFileOpt.setType(File.class);

        // todo - append list of possible schema names to description text
        final Option schemaOpt = new Option("schema", "the data schema name of the input files");
        schemaOpt.setArgs(1);
        schemaOpt.setArgName("name");
        schemaOpt.setType(String.class);

        final Option propertyOpt = new Option("D", "use value for given property");
        propertyOpt.setValueSeparator('=');
        propertyOpt.setArgName("property=value");
        propertyOpt.setArgs(2);

        Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(versionOpt);
        options.addOption(verboseOpt);
        options.addOption(debugOpt);
        options.addOption(schemaOpt);
        options.addOption(confFileOpt);
        options.addOption(propertyOpt);

        return options;
    }

    public void addConfigurationProperties(Properties properties) {
        for (Map.Entry entry : properties.entrySet()) {
            configuration.put(entry.getKey(), entry.getValue());
        }
    }

    public void addConfigurationProperties(File configurationFile) throws ToolException {
        try {
            FileReader reader = new FileReader(configurationFile);
            try {
                Properties configuration = new Properties();
                configuration.load(reader);
                addConfigurationProperties(configuration);
            } finally {
                reader.close();
            }
        } catch (FileNotFoundException e) {
            throw new ToolException(MessageFormat.format("File not found {0}", configurationFile), 2, e);
        } catch (IOException e) {
            throw new ToolException(MessageFormat.format("Failed to read from {0}", configurationFile), 3, e);
        }
        printInfo(MessageFormat.format("Using configuration read from {0}", configurationFile));
    }

    private void printVersion() {
        System.out.println("Version 1.0");
    }

    void printHelp(String footer) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("mms-ingest <input-files>",
                            "Valid options are",
                            options,
                            footer,
                            true);
    }

    private void printInfo(String msg) {
        if (isVerbose()) {
            System.out.println(msg);
        }
    }


    public static class ToolException extends Exception {
        int exitCode;

        private ToolException(String message, int exitCode) {
            super(message);
            this.exitCode = exitCode;
        }

        private ToolException(String message, int exitCode, Throwable cause) {
            super(message, cause);
            this.exitCode = exitCode;
        }

        public int getExitCode() {
            return exitCode;
        }
    }

}
