/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.tool;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Abstract base class for all SST-CCI tools.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public abstract class Tool {

    private static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.INFO;
    private CommandLine commandLine;
    private boolean dumpStackTrace;
    private Options options;

    public static final Logger LOGGER = Logger.getLogger("org.esa.cci.sst");

    static {
        BeamLogManager.removeRootLoggerHandlers();
    }

    protected Tool() {
    }

    public final void run(String[] arguments) {
        options = createOptions();
        try {
            run0(arguments);
        } catch (ToolException e) {
            error(e, e.getExitCode());
        } catch (Throwable e) {
            error(e, ExitCode.INTERNAL_ERROR);
        }
    }

    private void run0(String[] arguments) throws ToolException {
        try {
            commandLine = parseCommandLine(arguments);
        } catch (ParseException e) {
            throw new ToolException(e.getMessage(), ExitCode.USAGE_ERROR);
        }

        if (commandLine.getArgs().length == 0 && commandLine.getOptions().length == 0) {
            throw new ToolException("", ExitCode.USAGE_ERROR);
        }

        dumpStackTrace = commandLine.hasOption("errors");

        if (commandLine.hasOption("help")) {
            printHelp();
            return;
        }

        if (commandLine.hasOption("version")) {
            printVersion();
            return;
        }

        LogLevel logLevel = DEFAULT_LOG_LEVEL;
        if (commandLine.hasOption("logLevel")) {
            logLevel = LogLevel.valueOf(commandLine.getOptionValue("logLevel"));
        }
        initLogger(logLevel);

        run(getConfiguration(), commandLine.getArgs());
    }

    protected abstract String getName();

    protected abstract String getVersion();

    protected abstract String getSyntax();

    protected abstract String getHeader();

    protected String getFooter() {
        return "\n" +
               "All parameter options may also be read from a key-value-pair file. The tool will always try " +
               "to read settings in the default configuration file './" + getDefaultConfigFile() + "'. Optionally, a " +
               "configuration file may be provided using the -c <FILE> option (see above)." +
               "Command-line options overwrite the settings given by -c, which again overwrite settings in " +
               "default configuration file.\n";
    }

    protected abstract String getToolHome();

    protected abstract Parameter[] getParameters();

    protected abstract void run(Configuration configuration, String[] arguments) throws ToolException;

    private Configuration getConfiguration() throws ToolException {
        Properties properties = new Properties();

        Parameter[] parameters = getParameters();

        // 1. Set default values
        for (Parameter param : parameters) {
            if (param.getDefaultValue() != null) {
                properties.setProperty(param.getName(), param.getDefaultValue());
            }
        }

        // 2. Overwrite from default config file
        File defaultConfigFile = getDefaultConfigFile();
        if (defaultConfigFile.exists()) {
            loadConfig(defaultConfigFile.getPath(), properties);
        } else {
            info("Default configuration file '" + defaultConfigFile + "' does ot exist.");
        }

        // 3. Overwrite from user config file
        String configPath = commandLine.getOptionValue("config", null);
        if (configPath != null) {
            loadConfig(configPath, properties);
        }

        // 4. Overwrite from command-line
        for (Parameter param : parameters) {
            if (commandLine.hasOption(param.getName())) {
                String optionValue = commandLine.getOptionValue(param.getName());
                if (optionValue == null) {
                    // option without arg means, an option has been set (to "true")
                    optionValue = "true";
                }
                properties.setProperty(param.getName(), optionValue);
            }
        }

        return new Configuration(getToolHome(), properties);
    }

    protected File getDefaultConfigFile() {
        return new File(getName() + ".properties");
    }

    private void loadConfig(String configPath, Properties properties) throws ToolException {
        try {
            FileReader reader = new FileReader(configPath);
            try {
                properties.load(reader);
                info("Configuration '" + configPath + "' loaded");
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new ToolException(
                    MessageFormat.format("Failed to load configuration from ''{0}'': {1}", configPath, e.getMessage()),
                    ExitCode.IO_ERROR);
        }
    }

    protected void info(String message) {
        LOGGER.info(message);
    }

    protected void warn(String message) {
        LOGGER.warning(message);
    }

    private void error(Throwable error, ExitCode exitCode) {
        if (ToolException.class.equals(error.getClass())) {
            LOGGER.severe("Error: " + error.getMessage());
            if (exitCode == ExitCode.USAGE_ERROR) {
                LOGGER.severe("Consider using option -h to display the usage help");
            }
        } else {
            LOGGER.severe("Internal error: " + error.getClass().getName() + ": " + error.getMessage());
            if (!dumpStackTrace) {
                LOGGER.severe("Consider using option -e to display the error's full stack trace");
            }
        }
        if (dumpStackTrace) {
            error.printStackTrace(System.err);
        }
        System.exit(exitCode.ordinal());
    }

    private void printHelp() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setWidth(120);
        helpFormatter.printHelp(getSyntax(),
                                getHeader(),
                                options,
                                getFooter(), false);
    }

    private void printVersion() {
        System.out.printf("%s version %s%n", getName(), getVersion());
    }

    private CommandLine parseCommandLine(String[] arguments) throws ParseException {
        CommandLineParser parser = new GnuParser();
        return parser.parse(options, arguments);
    }

    private Options createOptions() {
        Options options = new Options();
        for (Parameter param : getParameters()) {
            String description = param.getDescription();
            if (param.getDefaultValue() != null) {
                if (!description.endsWith(".")) {
                    description += ".";
                }
                description += " The default value is '" + param.getDefaultValue() + "'.";
            }
            options.addOption(createOption(null, param.getName(), param.getArgName(), description));
        }

        options.addOption(createOption("h", "help", null,
                                       "Displays this help."));
        options.addOption(createOption("v", "version", null,
                                       "Displays the version of this program and exits."));
        options.addOption(createOption("c", "config", "FILE",
                                       "Reads a configuration (key-value pairs) from given FILE."));
        options.addOption(createOption("e", "errors", null,
                                       "Dumps a full error stack trace."));
        options.addOption(createOption("l", "logLevel", "LEVEL",
                                       String.format(
                                               "sets the logging level. Must be one of %s. Use level '%s' to also output diagnostics. The default value is '%s'.",
                                               Arrays.toString(LogLevel.values()), LogLevel.ALL, DEFAULT_LOG_LEVEL)));
        return options;
    }


    private static Option createOption(String shortOpt, String longOpt, String argName, String description) {
        Option from = new Option(shortOpt, longOpt, argName != null, description);
        from.setRequired(false);
        from.setArgName(argName);
        return from;
    }

    protected static void initLogger(LogLevel logLevel) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final Formatter formatter = new Formatter() {
            @Override
            public String format(LogRecord record) {
                final StringBuilder sb = new StringBuilder();
                sb.append(dateFormat.format(new Date(record.getMillis())));
                sb.append(" - ");
                sb.append(record.getLevel().getName());
                sb.append(": ");
                sb.append(record.getMessage());
                sb.append("\n");
                @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                final Throwable thrown = record.getThrown();
                if (thrown != null) {
                    sb.append(thrown.toString());
                    sb.append("\n");
                }
                return sb.toString();
            }
        };
        final ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        handler.setLevel(Level.ALL);
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(handler);
        LOGGER.setLevel(logLevel.getValue());
    }

}
