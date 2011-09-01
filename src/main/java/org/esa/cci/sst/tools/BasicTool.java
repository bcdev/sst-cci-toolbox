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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.esa.beam.framework.gpf.GPF;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.util.TimeUtil;

import javax.media.jai.JAI;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Formatter;
import java.util.logging.Handler;
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

    static class SstLogFormatter extends Formatter {

        private static final String LINE_SEPARATOR = System.getProperty("line.separator");

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();

            sb.append(TimeUtil.formatCcsdsUtcMillisFormat(new Date(record.getMillis())))
                    .append(" ")
                    .append(record.getLevel().getLocalizedName())
                    .append(": ")
                    .append(formatMessage(record))
                    .append(LINE_SEPARATOR);

            if (record.getThrown() != null) {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    sb.append(sw.toString());
                } catch (Exception ex) {
                    // ignore
                }
            }

            return sb.toString();
        }
    }


    public static final String CONFIG_FILE_OPTION_NAME = "c";
    public static final String DEFAULT_CONFIGURATION_FILE_NAME = "mms-config.properties";

    private final String name;
    private final String version;
    private final Properties configuration;
    private final Options options;

    private Logger logger;
    private ErrorHandler errorHandler;

    private boolean initialised;
    private PersistenceManager persistenceManager;

    private Date sourceStartTime;
    private Date sourceStopTime;

    static {
        System.setProperty("EPSG-HSQL.directory", System.getProperty("user.home", ".") + File.separator + "tmp");
        JAI.enableDefaultTileCache();
//        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(1024 * 1024 * 1024);
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(1024 * 1024 * 128);
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

    protected BasicTool(String name, String version) {
        this.name = name;
        this.version = version;

        configuration = new Properties();
        options = createCommandLineOptions();

        for (final Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            if (entry.getKey().toString().startsWith("mms.")) {
                configuration.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public final String getName() {
        return name;
    }

    public Logger getLogger() {
        if (logger == null) {
            synchronized (this) {
                if (logger == null) {
                    logger = Logger.getLogger("org.esa.cci.sst");
                    logger.setLevel(Level.INFO);
                    final SstLogFormatter formatter = new SstLogFormatter();
                    for (Handler handler : logger.getHandlers()) {
                        handler.setFormatter(formatter);
                    }
                }
            }
        }
        return logger;
    }

    public final ErrorHandler getErrorHandler() {
        if (errorHandler == null) {
            synchronized (this) {
                if (errorHandler == null) {
                    errorHandler = new ErrorHandler() {
                        @Override
                        public void terminate(ToolException e) {
                            getLogger().log(Level.SEVERE, e.getMessage(), e);
                            if (getLogger().isLoggable(Level.FINEST)) {
                                for (final StackTraceElement element : e.getCause().getStackTrace()) {
                                    getLogger().log(Level.FINEST, element.toString());
                                }
                            }
                            e.getCause().printStackTrace(System.err);
                            System.exit(e.getExitCode());
                        }

                        @Override
                        public void warn(Throwable t, String message) {
                            Logger.getLogger("org.esa.cci.sst").log(Level.WARNING, message, t);
                            if (getLogger().isLoggable(Level.FINEST)) {
                                for (final StackTraceElement element : t.getStackTrace()) {
                                    getLogger().log(Level.FINEST, element.toString());
                                }
                            }
                        }
                    };
                }
            }
        }
        return errorHandler;
    }

    public Properties getConfiguration() {
        return configuration;
    }

    private void setDebug(boolean debug) {
        if (debug) {
            getLogger().setLevel(Level.ALL);
        }
    }

    private void setSilent(boolean silent) {
        if (silent) {
            getLogger().setLevel(Level.WARNING);
        }
    }

    private Options getOptions() {
        return options;
    }

    public final PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    public final Date getSourceStartTime() {
        return sourceStartTime;
    }

    public final Date getSourceStopTime() {
        return sourceStopTime;
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
                printHelp("");
                return false;
            }
            File configurationFile = (File) commandLine.getParsedOptionValue(CONFIG_FILE_OPTION_NAME);
            if (configurationFile == null) {
                configurationFile = new File(DEFAULT_CONFIGURATION_FILE_NAME);
                if (configurationFile.exists()) {
                    addConfigurationProperties(configurationFile);
                    configuration.put(Constants.PROPERTY_CONFIGURATION, configurationFile.getPath());
                }
            } else {
                addConfigurationProperties(configurationFile);
                configuration.put(Constants.PROPERTY_CONFIGURATION, configurationFile.getPath());
            }

            final Properties optionProperties = commandLine.getOptionProperties("D");
            if (optionProperties.size() > 0) {
                addConfigurationProperties(optionProperties);
            }
            // set java.io.tmpdir system property for file io of compressed files
            final String systemTmpDir = System.getProperty("java.io.tmpdir", "/tmp");
            final String tmpDir = commandLine.getOptionValue("tmp", systemTmpDir);
            System.getProperties().put("java.io.tmpdir", tmpDir);

            setAdditionalCommandLineArgs(commandLine);
        } catch (ParseException e) {
            throw new ToolException(e.getMessage(), e, ToolException.COMMAND_LINE_ARGUMENTS_PARSE_ERROR);
        }

        return true;
    }

    public final Sensor getSensor(final String sensorName) {
        return (Sensor) getPersistenceManager().pick("select s from Sensor s where s.name = ?1", sensorName);
    }

    public void initialize() {
        if (initialised) {
            return;
        }
        getLogger().info("connecting to database " + getConfiguration().get("openjpa.ConnectionURL"));
        persistenceManager = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME, getConfiguration());

        final String startTime = configuration.getProperty(Constants.PROPERTY_SOURCE_START_TIME,
                                                           "1978-01-01T00:00:00Z");
        final String endTime = configuration.getProperty(Constants.PROPERTY_SOURCE_END_TIME,
                                                         "2100-01-01T00:00:00Z");
        try {
            sourceStartTime = TimeUtil.parseCcsdsUtcFormat(startTime);
            sourceStopTime = TimeUtil.parseCcsdsUtcFormat(endTime);
        } catch (java.text.ParseException e) {
            throw new ToolException("Cannot parse start or stop date.", e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
        initialised = true;
    }

    protected String getCommandLineSyntax() {
        return getName();
    }

    protected void setAdditionalCommandLineArgs(CommandLine commandLine) {
    }

    protected void printHelp(String footer) {
        new HelpFormatter().printHelp(getCommandLineSyntax(), "Valid options are", getOptions(), footer, true);
    }

    private void addConfigurationProperties(File configurationFile) {
        FileReader reader = null;
        try {
            reader = new FileReader(configurationFile);
            Properties configuration = new Properties();
            configuration.load(reader);
            addConfigurationProperties(configuration);
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

    private void addConfigurationProperties(Properties properties) {
        for (final Map.Entry entry : properties.entrySet()) {
            configuration.put(entry.getKey(), entry.getValue());
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

        final Option tmpDirOpt = new Option("tmp", true, "temp dir for file IO");

        Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(versionOpt);
        options.addOption(verboseOpt);
        options.addOption(debugOpt);
        options.addOption(confFileOpt);
        options.addOption(propertyOpt);
        options.addOption(tmpDirOpt);

        return options;
    }
}
