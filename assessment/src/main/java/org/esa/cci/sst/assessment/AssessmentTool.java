package org.esa.cci.sst.assessment;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

class AssessmentTool {
    private final Options options;
    private final Logger logger;

    AssessmentTool() {
        options = new Options();
        final Option templateOption = new Option("t", "template", true, "The word template file-path to use");
        templateOption.setRequired(true);
        options.addOption(templateOption);

        final Option propertiesOption = new Option("p", "properties", true, "The properties file-path containing the variables");
        propertiesOption.setRequired(true);
        options.addOption(propertiesOption);

        final Option outputFileOption = new Option("o", "output", true, "The output file name/path");
        outputFileOption.setRequired(true);
        options.addOption(outputFileOption);

        logger = Logger.getLogger("org.esa.cci.sst.assessment");
    }

    void run(String[] args) throws IOException, InvalidFormatException {
        final CmdLineParameter cmdLineParameter = parseCommandLine(options, args);
        if (cmdLineParameter == null) {
            return;
        }

        final PoiWordDocument wordDocument = loadWordTemplate(cmdLineParameter.getTemplateFile());
        final Properties properties = loadProperties(cmdLineParameter.getPropertiesFile());

        replaceVariables(wordDocument, properties);

        saveDocument(wordDocument, cmdLineParameter.getOutputFile());
    }

    private void saveDocument(PoiWordDocument wordDocument, File outputFile) throws IOException {
        final File absoluteFile = outputFile.getAbsoluteFile();
        final File outputDir = absoluteFile.getParentFile();
        if (!outputDir.isDirectory()) {
            if (!outputDir.mkdirs()) {
                final String message = "Unable to create target directory'" + outputDir.getAbsolutePath() + "'";
                logger.severe(message);
                throw new IOException(message);
            }
        }

        if (!outputFile.createNewFile()) {
            final String message = "Unable to create target file'" + outputFile.getAbsolutePath() + "'";
            logger.severe(message);
            throw new IOException(message);
        }

        wordDocument.save(outputFile);
        logger.info("Saved precessed Word document to '" + absoluteFile.getAbsolutePath() + "'");
    }

    private void replaceVariables(PoiWordDocument wordDocument, Properties properties) throws IOException, InvalidFormatException {
        replaceWordVariables(wordDocument, properties);
        replaceFigureVariables(wordDocument, properties);

        logger.info("Replaced variables in template");
    }

    private void replaceFigureVariables(PoiWordDocument wordDocument, Properties properties) throws IOException, InvalidFormatException {
        final Set<String> propertyNames = properties.stringPropertyNames();
        final String figuresDirectory = properties.getProperty("figures.directory");

        for (final String propertyName : propertyNames) {
            if (isFigureProperty(propertyName)) {
                final String figureName = properties.getProperty(propertyName);
                final File figure = new File(figuresDirectory, figureName);
                if (!figure.isFile()) {
                    logger.warning("Figure '" + figureName + "' at '" + figure.getAbsolutePath() + " does not exist.");
                    continue;
                }

                final String scaleProperty = properties.getProperty(createScaleName(figureName));
                if (scaleProperty != null) {
                    final double scale = Double.parseDouble(scaleProperty);
                    wordDocument.replaceWithFigure(makeWordVariable(propertyName), figure, scale);
                } else {
                    wordDocument.replaceWithFigure(makeWordVariable(propertyName), figure);
                }
            }
        }
    }

    static String makeWordVariable(String propertyName) {
        return "${" + propertyName + "}";
    }

    static String createScaleName(String figureName) {
        return figureName + ".scale";
    }

    static boolean isFigureProperty(String propertyName) {
        return propertyName.startsWith("figure.") && !propertyName.contains(".scale");
    }

    private void replaceWordVariables(PoiWordDocument wordDocument, Properties properties) {
        final Set<String> propertyNames = properties.stringPropertyNames();

        for (final String propertyName : propertyNames) {
            if (propertyName.startsWith("word.")) {
                final String variableValue = properties.getProperty(propertyName);
                wordDocument.replaceWithText(makeWordVariable(propertyName), variableValue);
            }
        }
    }

    private Properties loadProperties(File propertiesFile) throws IOException {
        final Properties properties = new Properties();

        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(propertiesFile);
            properties.load(inStream);
            logger.info("Loaded properties file '" + propertiesFile + "'");
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ignore) {
                }
            }
        }

        return properties;
    }

    private PoiWordDocument loadWordTemplate(File templateFile) throws IOException {
        final PoiWordDocument poiWordDocument = new PoiWordDocument(templateFile);
        logger.info("Loaded Word template file '" + templateFile + "'");
        return poiWordDocument;
    }

    private static void printHelp(Options options) {
        final HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setNewLine("\n");
        helpFormatter.setWidth(80);
        helpFormatter.printHelp("assessment-tool [options]", "\nOptions:", options, "", false);
    }

    static CmdLineParameter parseCommandLine(Options options, String[] args) {
        final CmdLineParameter cmdLineParameter = new CmdLineParameter();
        try {
            final CommandLine commandLine = new PosixParser().parse(options, args);

            final String templateFilePath = commandLine.getOptionValue("t");
            final File templateFile = createFileVerified(templateFilePath, "Template");
            cmdLineParameter.setTemplateFile(templateFile);

            final String propertiesFilePath = commandLine.getOptionValue("p");
            final File propertiesFile = createFileVerified(propertiesFilePath, "Properties");
            cmdLineParameter.setPropertiesFile(propertiesFile);

            final String outputFilePath = commandLine.getOptionValue("o");
            final File outputFile = new File(outputFilePath);
            cmdLineParameter.setOutputFile(outputFile);

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            printHelp(options);
            return null;
        }

        return cmdLineParameter;
    }

    private static File createFileVerified(String filePath, String fileDescription) throws ParseException {
        final File file = new File(filePath);
        if (!file.isFile()) {
            throw new ParseException(fileDescription + " file '" + filePath + "` does not exist");
        }
        return file;
    }
}
