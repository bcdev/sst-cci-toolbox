/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools.arcprocessing;

import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TimeUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Tool responsible for generating calls of the ARC3 processor.
 *
 * @author Thomas Storm
 */
public class Arc3ProcessingTool extends BasicTool {

    private PrintWriter arc3CallWriter;
    private PrintWriter subsceneWriter;
    private PrintWriter reingestionCallWriter;
    private PrintWriter cleanupCallWriter;
    private String arc3CallScript;
    private String reingestionCallScript;
    private String cleanupScript;
    private String subsceneScript;

    public static void main(String[] args) {
        final Arc3ProcessingTool tool = new Arc3ProcessingTool();
        tool.setCommandLineArgs(args);
        tool.initialize();
        try {
            tool.setupWriters();
            tool.writeCalls();
        } catch (IOException e) {
            tool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.TOOL_IO_ERROR));
        } finally {
            tool.close();
        }
    }

    Arc3ProcessingTool() {
        super("arc3processing.sh", "0.1");
    }

    private void writeCalls() throws IOException {
        final Arc3CallBuilder arc3Caller = new Arc3CallBuilderFactory().createArc3CallBuilder();
        final String subsceneCall = arc3Caller.createSubsceneCall();
        final String arc3Call = arc3Caller.createArc3Call();
        final String reingestionCall = arc3Caller.createReingestionCall();
        final String cleanupCall = arc3Caller.createCleanupCall(subsceneScript, arc3CallScript, reingestionCallScript, cleanupScript);

        subsceneWriter.write(subsceneCall);
        arc3CallWriter.write(arc3Call);
        reingestionCallWriter.write(reingestionCall);
        cleanupCallWriter.write(cleanupCall);
    }

    private void setupWriters() throws IOException {
        final String destPath = getConfiguration().getProperty(Constants.PROPERTY_OUTPUT_DESTDIR, ".");
        final File destDir = new File(destPath);
        final String tmpPath = getConfiguration().getProperty(Constants.PROPERTY_OUTPUT_TMPDIR, ".");
        final File tmpDir = new File(tmpPath);
        createDirectory(destDir);
        createDirectory(tmpDir);
        final String timeProperty = getConfiguration().getProperty(Constants.PROPERTY_OUTPUT_START_TIME);
        final Date timeAsDate = TimeUtil.getConfiguredTimeOf(timeProperty);
        final String time = TimeUtil.formatCompactUtcFormat(timeAsDate);

        String subsceneFilename = String.format("mms-subscene-%s-submit.sh", time);
        final File subsceneFile = new File(tmpDir, subsceneFilename);
        setFileExecutable(subsceneFile);
        subsceneScript = subsceneFile.getAbsolutePath();
        subsceneWriter = new PrintWriter(subsceneFile);
        subsceneWriter.format("#!/bin/bash\n\n");

        String arc3CallFilename = String.format("mms-arc3-%s-submit.sh", time);
        final File arc3CallFile = new File(tmpDir, arc3CallFilename);
        setFileExecutable(arc3CallFile);
        arc3CallScript = arc3CallFile.getAbsolutePath();
        arc3CallWriter = new PrintWriter(arc3CallFile);
        arc3CallWriter.format("#!/bin/bash\n\n");

        String reingestionCallFilename = String.format("mms-arc3-%s-reingest.sh", time);
        final File reingestionFile = new File(tmpDir, reingestionCallFilename);
        setFileExecutable(reingestionFile);
        reingestionCallScript = reingestionFile.getAbsolutePath();
        reingestionCallWriter = new PrintWriter(new BufferedWriter(new FileWriter(reingestionFile)));
        reingestionCallWriter.format("#!/bin/bash\n\n");

        String cleanupCallFilename = String.format("mms-arc3-%s-cleanup.sh", time);
        final File cleanupFile = new File(tmpDir, cleanupCallFilename);
        setFileExecutable(cleanupFile);
        cleanupScript = cleanupFile.getAbsolutePath();
        cleanupCallWriter = new PrintWriter(new BufferedWriter(new FileWriter(cleanupFile)));
        cleanupCallWriter.format("#!/bin/bash\n\n");
    }

    private void setFileExecutable(File file) throws IOException {
        final boolean success = file.setExecutable(true);
        if (!success) {
            throw new IOException(
                    MessageFormat.format("Could not set file ''{0}'' executable.", file.getAbsolutePath()));
        }
    }

    private void createDirectory(File dir) throws IOException {
        if (dir.exists()) {
            return;
        }
        final boolean success = dir.mkdirs();
        if (!success) {
            throw new IOException(
                    MessageFormat.format("Could not create new directory ''{0}''.", dir.getAbsolutePath()));
        }
    }

    private void close() {
        closeWriter(subsceneWriter);
        closeWriter(arc3CallWriter);
        closeWriter(reingestionCallWriter);
        closeWriter(cleanupCallWriter);
    }

    private void closeWriter(PrintWriter writer) {
        if(writer != null) {
            writer.close();
        }
    }

    private class Arc3CallBuilderFactory {

        private Arc3CallBuilder createArc3CallBuilder() {
            final Properties configuration = getConfiguration();
            final String cutSubscenes = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_CUT_SUBSCENES, "false");
            if (Boolean.parseBoolean(cutSubscenes)) {
                return new SubsceneArc3CallBuilder(configuration);
            } else {
                return new SimpleArc3CallBuilder(configuration);
            }
        }
    }

}
