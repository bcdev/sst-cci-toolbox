package org.esa.cci.sst;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.esa.cci.sst.orm.PersistenceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

/**
 * The base class for all MMS command line tools.
 *
 * @author Martin Boettcher
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public class MmsTool {

    static final File DEFAULT_CONFIGURATION_FILE = new File("mms-config.properties");
    /**
     * Name of persistence unit in META-INF/persistence.xml
     */
    public static final String CONFIG_FILE_OPTION_NAME = "c";

    private final String name;
    private final String version;
    private final Properties configuration;
    private final Options options;

    private boolean verbose;
    private boolean debug;
    private boolean initialised;
    private PersistenceManager persistenceManager;

    protected MmsTool(String name, String version) {
        this.name = name;
        this.version = version;
        configuration = new Properties();
        options = createCommandLineOptions();

        for (final Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            if (entry.getKey().toString().startsWith("mms.")) {
                getConfiguration().put(entry.getKey(), entry.getValue());
            }
        }
    }

    public final String getName() {
        return name;
    }

    public final String getVersion() {
        return version;
    }

    public final Properties getConfiguration() {
        return configuration;
    }

    public final boolean isDebug() {
        return debug;
    }

    public final void setDebug(boolean debug) {
        this.debug = debug;
    }

    public final boolean isVerbose() {
        return verbose;
    }

    public final void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public final Options getOptions() {
        return options;
    }

    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    public final boolean setCommandLineArgs(String[] args) throws ToolException {
        final CommandLineParser parser = new PosixParser();
        try {
            final CommandLine commandLine = parser.parse(getOptions(), args);
            setDebug(commandLine.hasOption("debug"));
            setVerbose(commandLine.hasOption("verbose"));
            if (commandLine.hasOption("version")) {
                printVersion();
                return false;
            }
            if (commandLine.hasOption("help")) {
                printHelp("");
                return false;
            }
            final File configurationFile = (File) commandLine.getParsedOptionValue(CONFIG_FILE_OPTION_NAME);
            if (configurationFile == null) {
                if (DEFAULT_CONFIGURATION_FILE.exists()) {
                    addConfigurationProperties(DEFAULT_CONFIGURATION_FILE);
                }
            } else {
                addConfigurationProperties(configurationFile);
            }
            final Properties optionProperties = commandLine.getOptionProperties("D");
            if (optionProperties.size() > 0) {
                addConfigurationProperties(optionProperties);
            }
            setAdditionalCommandLineArgs(commandLine);
        } catch (ParseException e) {
            throw new ToolException(e.getMessage(), 4, e);
        }

        return true;
    }

    public void initialize() throws ToolException {
        if (initialised) {
            return;
        }

        persistenceManager = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME, getConfiguration());
        initialised = true;
    }

    public String getCommandLineSyntax() {
        return getName();
    }

    public void printInfo(String msg) {
        if (isVerbose()) {
            System.out.println(MessageFormat.format("{0}: {1}", getName(), msg));
        }
    }

    protected void setAdditionalCommandLineArgs(CommandLine commandLine) {
    }

    protected void printHelp(String footer) {
        new HelpFormatter().printHelp(getCommandLineSyntax(), "Valid options are", getOptions(), footer, true);
    }

    private void addConfigurationProperties(File configurationFile) throws ToolException {
        FileReader reader = null;
        try {
            reader = new FileReader(configurationFile);
            Properties configuration = new Properties();
            configuration.load(reader);
            addConfigurationProperties(configuration);
        } catch (FileNotFoundException e) {
            throw new ToolException(MessageFormat.format("File not found {0}", configurationFile), 2, e);
        } catch (IOException e) {
            throw new ToolException(MessageFormat.format("Failed to read from {0}", configurationFile), 3, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        printInfo(MessageFormat.format("Using configuration read from {0}", configurationFile));
    }

    private void addConfigurationProperties(Properties properties) {
        for (final Map.Entry entry : properties.entrySet()) {
            configuration.put(entry.getKey(), entry.getValue());
        }
    }

    private void printVersion() {
        System.out.println(MessageFormat.format("Version {0}", getVersion()));
    }

    private static Options createCommandLineOptions() {
        final Option helpOpt = new Option("help", "print this message");
        final Option versionOpt = new Option("version", "print the version information and exit");
        final Option verboseOpt = new Option("verbose", "be extra verbose");
        final Option debugOpt = new Option("debug", "print debugging information");

        final Option confFileOpt = new Option(CONFIG_FILE_OPTION_NAME, "alternative configuration file");
        confFileOpt.setArgs(1);
        confFileOpt.setArgName("file");
        confFileOpt.setType(File.class);

        final Option propertyOpt = new Option("D", "use value for given property");
        propertyOpt.setValueSeparator('=');
        propertyOpt.setArgName("property=value");
        propertyOpt.setArgs(2);

        Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(versionOpt);
        options.addOption(verboseOpt);
        options.addOption(debugOpt);
        options.addOption(confFileOpt);
        options.addOption(propertyOpt);

        return options;
    }
}
