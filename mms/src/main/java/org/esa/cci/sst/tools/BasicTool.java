/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.tools;

import org.apache.commons.cli.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.cci.sst.log.SstLogging;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.util.TimeUtil;

import javax.media.jai.JAI;
import javax.persistence.Query;
import java.io.*;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * The base class for all MMS command line tools.
 *
 * @author Martin Boettcher
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public abstract class BasicTool {

    private Storage toolStorage;

    public static final String CONFIG_FILE_OPTION_NAME = "c";
    public static final String DEFAULT_CONFIGURATION_FILE_NAME = "mms-config.properties";

    private final String name;
    private final String version;
    private final Configuration config;
    private final Options options;

    protected final Logger logger;
    private ErrorHandler errorHandler;

    private boolean initialised;
    private PersistenceManager persistenceManager;

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.ENGLISH);

        System.setProperty("EPSG-HSQL.directory", System.getProperty("user.home", ".") + File.separator + "tmp");
        System.setProperty("beam.imageManager.enableSourceTileCaching", "true");

        JAI.enableDefaultTileCache();
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(1024 * 1024 * 1024);

        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

    protected BasicTool(String name, String version) {
        this.name = name;
        this.version = version;

        options = createCommandLineOptions();

        config = new Configuration();
        config.add(System.getProperties(), "mms.");

        logger = SstLogging.getLogger();
    }

    public final String getName() {
        return name;
    }

    public final ErrorHandler getErrorHandler() {
        if (errorHandler == null) {
            synchronized (this) {
                if (errorHandler == null) {
                    errorHandler = new ErrorHandler() {
                        @Override
                        public void terminate(ToolException e) {
                            final Logger localLogger = SstLogging.getLogger();
                            localLogger.log(Level.SEVERE, e.getMessage(), e);
                            if (e.getCause() != null) {
                                if (localLogger.isLoggable(Level.FINEST)) {
                                    for (final StackTraceElement element : e.getCause().getStackTrace()) {
                                        localLogger.log(Level.FINEST, element.toString());
                                    }
                                }
                                e.getCause().printStackTrace(System.err);
                            }
                            System.exit(e.getExitCode());
                        }

                        @Override
                        public void warn(Throwable t, String message) {
                            final Logger localLogger = SstLogging.getLogger();
                            localLogger.log(Level.WARNING, message, t);
                            if (localLogger.isLoggable(Level.FINEST)) {
                                for (final StackTraceElement element : t.getStackTrace()) {
                                    localLogger.log(Level.FINEST, element.toString());
                                }
                            }
                        }
                    };
                }
            }
        }
        return errorHandler;
    }

    public Configuration getConfig() {
        return config;
    }

    private void setDebug(boolean debug) {
        if (debug) {
            SstLogging.setLevelDebug();
        }
    }

    private void setSilent(boolean silent) {
        if (silent) {
            SstLogging.setLevelSilent();
        }
    }

    private Options getOptions() {
        return options;
    }

    public final PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    public final boolean setCommandLineArgs(String[] args) {
        final CommandLineParser parser = new PosixParser();
        try {
            final CommandLine commandLine = parser.parse(getOptions(), args);
            setSilent(commandLine.hasOption("silent"));
            setDebug(commandLine.hasOption("debug"));
            if (commandLine.hasOption("version")) {
                printVersion();
                return false;
            }
            if (commandLine.hasOption("help")) {
                printHelp();
                return false;
            }
            final String configurationFilePath = commandLine.getOptionValue(CONFIG_FILE_OPTION_NAME);
            if (configurationFilePath != null) {
                final File configurationFile = new File(configurationFilePath);
                addConfigurationProperties(configurationFile);
                config.put(Configuration.KEY_MMS_CONFIGURATION, configurationFile.getPath());
            }
            final Properties optionProperties = commandLine.getOptionProperties("D");
            config.add(optionProperties);

            final Properties privateProperties = new Properties();
            privateProperties.load(BasicTool.class.getResourceAsStream("PRIVATE.properties"));
            config.add(privateProperties);
        } catch (ParseException e) {
            throw new ToolException(e.getMessage(), e, ToolException.COMMAND_LINE_ARGUMENTS_PARSE_ERROR);
        } catch (IOException e) {
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }

        return true;
    }

    public Storage getStorage() {
        return toolStorage;
    }

    public void initialize() {
        if (initialised) {
            return;
        }
        SstLogging.getLogger().info("connecting to database " + config.getStringValue("openjpa.ConnectionURL"));
        try {
            persistenceManager = PersistenceManager.create(Constants.PERSISTENCE_UNIT_NAME,
                    Constants.PERSISTENCE_RETRY_COUNT,
                    config.getAsProperties());
        } catch (Exception e) {
            throw new ToolException("Unable to establish database connection.", e, ToolException.TOOL_DB_ERROR);
        }
        if (config.getBooleanValue("mms.db.useindex", false)) {
            try {
                persistenceManager.transaction();
                final Query setSeqScanOff = persistenceManager.createNativeQuery("set enable_seqscan to off");
                setSeqScanOff.executeUpdate();
                persistenceManager.commit();
            } catch (Exception e) {
                SstLogging.getLogger().warning("failed setting seqscan to off: " + e.getMessage());
            }
        }
        toolStorage = persistenceManager.getStorage();

        initialised = true;
    }

    protected String getCommandLineSyntax() {
        return getName();
    }

    protected void printHelp() {
        new HelpFormatter().printHelp(getCommandLineSyntax(), "Valid options are", getOptions(), "", true);
    }

    private void addConfigurationProperties(File configurationFile) {
        FileReader reader = null;
        try {
            reader = new FileReader(configurationFile);
            config.load(reader);
        } catch (FileNotFoundException e) {
            throw new ToolException(MessageFormat.format("File not found: {0}", configurationFile), e,
                    ToolException.CONFIGURATION_FILE_NOT_FOUND_ERROR);
        } catch (IOException e) {
            throw new ToolException(MessageFormat.format("Failed to read from {0}.", configurationFile), e,
                    ToolException.CONFIGURATION_FILE_IO_ERROR);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private void printVersion() {
        System.out.println(MessageFormat.format("Version {0}", version));
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
