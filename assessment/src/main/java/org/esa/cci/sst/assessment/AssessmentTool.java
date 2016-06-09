package org.esa.cci.sst.assessment;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.WildcardMatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

class AssessmentTool {

    private static final String VERSION = "1.2";

    private final Options options;
    private final Logger logger;

    AssessmentTool() {
        options = createOptions();

        logger = Logger.getLogger("org.esa.cci.sst.assessment");
    }

    void run(String[] args) throws IOException, InvalidFormatException {
        final CmdLineParameter cmdLineParameter = parseCommandLine(options, args);
        if (cmdLineParameter == null) {
            return;
        }

        final PoiWordDocument wordDocument = loadWordTemplate(cmdLineParameter.getTemplateFile());
        final File propertiesFile = cmdLineParameter.getPropertiesFile();
        final Properties properties = loadProperties(propertiesFile);

        final TemplateVariables templateVariables = loadTemplateVariables(propertiesFile);

        replaceVariables(wordDocument, properties, templateVariables);

        saveDocument(wordDocument, cmdLineParameter.getOutputFile());
    }

    // @todo 3 tb/** make static, package local and test 2016-06-08
    private TemplateVariables loadTemplateVariables(File propertiesFile) throws IOException {
        final TemplateVariables templateVariables = new TemplateVariables();

        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(propertiesFile);
            templateVariables.load(inStream);
            logger.info("Loaded properties file '" + propertiesFile + "'");
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ignore) {
                }
            }
        }

        return templateVariables;
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

        if (outputFile.isFile() && options.hasOption("r")) {
            if (!outputFile.delete()) {
                final String message = "Unable to delete target file'" + outputFile.getAbsolutePath() + "'";
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

    private void replaceVariables(PoiWordDocument wordDocument, Properties properties, TemplateVariables templateVariables) throws IOException, InvalidFormatException {
        replaceWordVariables(wordDocument, templateVariables);
        replaceParagraphVariables(wordDocument, templateVariables);
        replaceFigureVariables(wordDocument, properties, templateVariables);
        replaceMultipleFigureVariables(wordDocument, properties, templateVariables);

        logger.info("Replaced variables in template");
    }

    private void replaceFigureVariables(PoiWordDocument wordDocument, Properties properties, TemplateVariables templateVariables) throws IOException, InvalidFormatException {
        final Set<String> propertyNames = properties.stringPropertyNames();
        final String figuresDirectory = templateVariables.getFiguresDirectory();
        if (StringUtils.isNullOrEmpty(figuresDirectory)) {
            logger.severe("Figures directory not configured.");
            return;
        }

        for (final String propertyName : propertyNames) {
            if (isFigureProperty(propertyName)) {
                final String figureName = properties.getProperty(propertyName);
                final File figure = new File(figuresDirectory, figureName);
                if (!figure.isFile()) {
                    logger.warning("Figure '" + figureName + "' at '" + figure.getAbsolutePath() + " does not exist.");
                    continue;
                }

                final String scaleProperty = properties.getProperty(createScaleName(propertyName));
                if (scaleProperty != null) {
                    final double scale = Double.parseDouble(scaleProperty);
                    wordDocument.replaceWithFigure(makeWordVariable(propertyName), figure, scale);
                } else {
                    wordDocument.replaceWithFigure(makeWordVariable(propertyName), figure);
                }
            }
        }
    }

    private void replaceMultipleFigureVariables(PoiWordDocument wordDocument, Properties properties, TemplateVariables templateVariables) throws IOException, InvalidFormatException {
        final Set<String> propertyNames = properties.stringPropertyNames();
        final String figuresDirectory = templateVariables.getFiguresDirectory();
        if (StringUtils.isNullOrEmpty(figuresDirectory)) {
            logger.severe("Figures directory not configured.");
            return;
        }

        for (final String propertyName : propertyNames) {
            if (isFiguresProperty(propertyName)) {
                final String figurePathesWildcards = properties.getProperty(propertyName);

                final ArrayList<File> fileList = applyWildCards(figuresDirectory, propertyName, figurePathesWildcards);

                final File[] files = fileList.toArray(new File[fileList.size()]);
                final String scaleProperty = properties.getProperty(createScaleName(propertyName));
                if (scaleProperty != null) {
                    final double scale = Double.parseDouble(scaleProperty);
                    wordDocument.replaceWithFigures(makeWordVariable(propertyName), files, scale);
                } else {
                    wordDocument.replaceWithFigures(makeWordVariable(propertyName), files);
                }
            }
        }
    }

    private ArrayList<File> applyWildCards(String figuresDirectory, String propertyName, String figurePathesWildcards) throws IOException {
        final ArrayList<File> fileList = new ArrayList<>();
        final String[] splittedWildcards = StringUtils.split(figurePathesWildcards, new char[]{';'}, true);
        for (final String wildcard : splittedWildcards) {
            final File[] files = WildcardMatcher.glob(new File(figuresDirectory, wildcard).getPath());
            for (final File file : files) {
                if (!file.isFile()) {
                    logger.warning("Figure '" + propertyName + "' at '" + file.getAbsolutePath() + " does not exist.");
                    continue;
                }
                fileList.add(file);
            }
        }
        return fileList;
    }

    private void replaceParagraphVariables(PoiWordDocument wordDocument, TemplateVariables templateVariables) {
        final Map<String, String> paragraphVariables = templateVariables.getParagraphVariables();
        final Set<Map.Entry<String, String>> entries = paragraphVariables.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            wordDocument.replaceParagraphText(makeWordVariable(entry.getKey()), entry.getValue());
        }
    }

    private void replaceWordVariables(PoiWordDocument wordDocument, TemplateVariables templateVariables) {
        final Map<String, String> wordVariables = templateVariables.getWordVariables();
        final Set<Map.Entry<String, String>> entries = wordVariables.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            wordDocument.replaceWithText(makeWordVariable(entry.getKey()), entry.getValue());
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

    static String makeWordVariable(String propertyName) {
        return "${" + propertyName + "}";
    }

    static String createScaleName(String figureName) {
        return figureName + ".scale";
    }

    static boolean isFigureProperty(String propertyName) {
        return propertyName.startsWith("figure.") && !propertyName.contains(".scale");
    }

    static boolean isFiguresProperty(String propertyName) {
        return propertyName.startsWith("figures.") && !propertyName.contains(".scale");
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
        helpFormatter.printHelp("assessment-tool " + VERSION + " [options]", "\nOptions:", options, "", false);
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

    static Options createOptions() {
        final Options options = new Options();
        final Option templateOption = new Option("t", "template", true, "The word template file-path to use");
        templateOption.setRequired(true);
        options.addOption(templateOption);

        final Option propertiesOption = new Option("p", "properties", true, "The properties file-path containing the variables");
        propertiesOption.setRequired(true);
        options.addOption(propertiesOption);

        final Option outputFileOption = new Option("o", "output", true, "The output file name/path");
        outputFileOption.setRequired(true);
        options.addOption(outputFileOption);

        final Option replaceFileOption = new Option("r", "replace", true, "Replace output file if existing");
        replaceFileOption.setRequired(false);
        replaceFileOption.setArgs(0);
        options.addOption(replaceFileOption);

        return options;
    }
}
