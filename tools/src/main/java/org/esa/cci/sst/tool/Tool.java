package org.esa.cci.sst.tool;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * The SST_cci Regional-Average tool.
 *
 * @author Norman Fomferra
 */
public abstract class Tool {

    private CommandLine commandLine;
    private boolean dumpStackTrace;
    private Options options;

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
            throw new ToolException(e.getMessage() + " (use option --help for usage)", ExitCode.USAGE_ERROR);
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

        run(getConfiguration(), commandLine.getArgs());
    }

    protected abstract String getName();

    protected abstract String getVersion();

    protected abstract String getSyntax();

    protected abstract String getHeader();

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
            warn("Missing configuration '" + defaultConfigFile + "'.");
        }

        // 3. Overwrite from user config file
        String configPath = commandLine.getOptionValue("config", null);
        if (configPath != null) {
            loadConfig(configPath, properties);
        }

        // 4. Overwrite from command-line
        for (Parameter param : parameters) {
            if (commandLine.hasOption(param.getName())) {
                properties.setProperty(param.getName(), commandLine.getOptionValue(param.getName()));
            }
        }

        return new Configuration(properties);
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
            throw new ToolException("Failed to load configuration from '" + configPath + "'", ExitCode.IO_ERROR);
        }
    }

    protected void info(String message) {
        System.out.println(message);
    }

    protected void warn(String message) {
        System.out.println("Warning: " + message);
    }

    private void error(Throwable error, ExitCode exitCode) {
        if (ToolException.class.equals(error.getClass())) {
            System.err.println("Error: " + error.getMessage());
        } else {
            System.err.println("Internal error: " + error.getClass().getName() + ": " + error.getMessage()  + (dumpStackTrace? " (use option -e to dump a full stack trace)"  :""));
        }
        if (dumpStackTrace) {
            error.printStackTrace(System.err);
        }
        System.exit(exitCode.ordinal());
    }

    private void printHelp() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(getSyntax(),
                                getHeader(),
                                options,
                                "", false);
    }

    private void printVersion() {
        System.out.println(getVersion());
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
            options.addOption(createOption(null, param.getName(), param.getType(), description));
        }

        options.addOption(createOption("h", "help", null,
                                       "displays this help."));
        options.addOption(createOption("v", "version", null,
                                       "display the version of this program and exit."));
        options.addOption(createOption("c", "config", "FILE",
                                       "read configuration from given FILE."));
        options.addOption(createOption("e", "errors", null,
                                       "dumps full error stack trace."));
        return options;
    }


    private static Option createOption(String shortOpt, String longOpt, String argName, String description) {
        Option from = new Option(shortOpt, longOpt, argName != null, description);
        from.setRequired(false);
        from.setArgName(argName);
        return from;
    }

}
