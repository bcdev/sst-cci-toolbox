/*
 * $Id$
 *
 * Copyright (C) 2010 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.cci.sst.assessment;

import static org.esa.beam.util.Debug.trace;
import static org.hsqldb.HsqlDateTime.e;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.esa.beam.util.Debug;

import java.io.File;
import java.io.IOException;

public class CarToolFasade {

    private final AssessmentTool assessmentTool;
    private Options options;

    public CarToolFasade() {
        assessmentTool = new AssessmentTool();
    }

    public String renderDocument(String[] args) {
        try {
            parseCommandLine(getOptions(), args);
            assessmentTool.run(args);
            return "success";
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return e.getMessage();
        } catch (InvalidFormatException e) {
            e.printStackTrace(System.err);
            return e.getMessage();
        } catch (ParseException e) {
            e.printStackTrace(System.err);
            return e.getMessage();
        }
    }

    private Options getOptions() {
        if (options == null) {
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

            final Option replaceFileOption = new Option("r", "replace", true, "Replace output file if existing");
            replaceFileOption.setRequired(false);
            replaceFileOption.setArgs(0);

            options.addOption(replaceFileOption);
        }
        return options;
    }

    private CmdLineParameter parseCommandLine(Options options, String[] args) throws ParseException {
        final CmdLineParameter cmdLineParameter = new CmdLineParameter();
        final CommandLine commandLine = new PosixParser().parse(options, args);

        final String templateFilePath = commandLine.getOptionValue("t");
        final File templateFile = createFileVerified(templateFilePath, "Template");
        cmdLineParameter.setTemplateFile(templateFile);

        final String propertiesFilePath = commandLine.getOptionValue("p");
        final File propertiesFile = new File(propertiesFilePath);
        final File propertiesDir = propertiesFile.getParentFile();
        if (!propertiesDir.isDirectory()) {
            throw new ParseException("Unable to write properties file. Invalid properties target dir.");
        }
        cmdLineParameter.setPropertiesFile(propertiesFile);

        final String outputFilePath = commandLine.getOptionValue("o");
        final File outputFile = new File(outputFilePath);
        final File outputDir = outputFile.getParentFile();
        if (!outputDir.isDirectory()) {
            throw new ParseException("Unable to render document. Invalid document target dir.");
        }
        cmdLineParameter.setOutputFile(outputFile);

        return cmdLineParameter;
    }

    private File createFileVerified(String filePath, String fileDescription) throws ParseException {
        final File file = new File(filePath);
        if (!file.isFile()) {
            throw new ParseException(fileDescription + " file does not exist!");
        }
        return file;
    }
}
