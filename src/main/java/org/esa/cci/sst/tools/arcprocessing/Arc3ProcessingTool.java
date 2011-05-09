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
    private PrintWriter reingestionCallWriter;
    private PrintWriter cleanupCallWriter;

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

    String createArc3Call() {
        final Properties configuration = getConfiguration();
        String sourceFilename = getSourceFilename();
        String executableName = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_EXECUTABLE, "MMD_SCREEN_Linux");
        String targetFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_TARGETFILE,
                                                          getDefaultTargetFileName(sourceFilename));
        String nwpFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_NWPFILE, "test_nwp.nc");

        final StringBuilder arc3Call = new StringBuilder();
        arc3Call.append(String.format("scp %s eddie.ecdf.ed.ac.uk:tmp/\n", sourceFilename));
        arc3Call.append(String.format("ssh eddie.ecdf.ed.ac.uk ./%s MDB.INP %s %s %s", executableName, sourceFilename,
                                      nwpFilename, targetFilename));
        return arc3Call.toString();
    }

    String createReingestionCall() {
        final String sourceFilename = getSourceFilename();
        final Properties configuration = getConfiguration();
        final String targetFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_TARGETFILE,
                                                          getDefaultTargetFileName(sourceFilename));
        final String pattern = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_PATTERN, "20000");

        final StringBuilder builder = new StringBuilder();
        builder.append("ssh eddie.ecdf.ed.ac.uk ");
        builder.append(String.format("bin/mmsreingestmmd.sh -Dmms.reingestion.filename=%s\n" +
                                      " -Dmms.reingestion.located=no \\\n" +
                                      " -Dmms.reingestion.sensor=ARC3 \\\n" +
                                      " -Dmms.reingestion.pattern=%s \\\n" +
                                      " -c config/mms-config-eddie1.properties", targetFilename, pattern));
        return builder.toString();
    }

    String createCleanupCall() {
        final String sourceFilename = getSourceFilename();
        return String.format("ssh eddie.ecdf.ed.ac.uk rm %s", sourceFilename);
    }

    String getDefaultTargetFileName(String sourceFilename) {
        final int extensionIndex = sourceFilename.lastIndexOf(".nc");
        return String.format("%s_ARC3.nc", sourceFilename.substring(0, extensionIndex));
    }

    private void writeCalls() {
        final String arc3Call = createArc3Call();
        final String reingestionCall = createReingestionCall();
        final String cleanupCall = createCleanupCall();
        arc3CallWriter.write(arc3Call);
        reingestionCallWriter.write(reingestionCall);
        cleanupCallWriter.write(cleanupCall);
    }

    private String getSourceFilename() {
        Properties configuration = getConfiguration();
        String sourceFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE);
        validateSourceFilename(sourceFilename);
        return sourceFilename;
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

        String arc3CallFilename = String.format("mms-arc3-%s-submit.sh", time);
        final File arc3CallFile = new File(tmpDir, arc3CallFilename);
        setFileExecutable(arc3CallFile);
        arc3CallWriter = new PrintWriter(arc3CallFile);
        arc3CallWriter.format("#!/bin/bash\n\n");

        String reingestionCallFilename = String.format("mms-arc3-%s-reingest.sh", time);
        final File reingestionFile = new File(tmpDir, reingestionCallFilename);
        setFileExecutable(reingestionFile);
        reingestionCallWriter = new PrintWriter(new BufferedWriter(new FileWriter(reingestionFile)));
        reingestionCallWriter.format("#!/bin/bash\n\n");

        String cleanupCallFilename = String.format("mms-arc3-%s-cleanup.sh", time);
        final File cleanupFile = new File(tmpDir, cleanupCallFilename);
        setFileExecutable(cleanupFile);
        cleanupCallWriter = new PrintWriter(new BufferedWriter(new FileWriter(cleanupFile)));
        cleanupCallWriter.format("#!/bin/bash\n\n");
    }

    private void setFileExecutable(File file) throws IOException {
        final boolean success = file.setExecutable(true);
        if(!success) {
            throw new IOException(
                    MessageFormat.format("Could not set file ''{0}'' executable.", file.getAbsolutePath()));
        }
    }

    private void createDirectory(File dir) throws IOException {
        if(dir.exists()) {
            return;
        }
        final boolean success = dir.mkdirs();
        if (!success) {
            throw new IOException(
                    MessageFormat.format("Could not create new directory ''{0}''.", dir.getAbsolutePath()));
        }
    }

    private void close() {
        if (arc3CallWriter != null) {
            arc3CallWriter.close();
        }
        if (reingestionCallWriter != null) {
            reingestionCallWriter.close();
        }
        if (cleanupCallWriter != null) {
            cleanupCallWriter.close();
        }
    }

    private void validateSourceFilename(String sourceFilename) {
        if (sourceFilename == null) {
            throw new IllegalStateException(
                    MessageFormat.format("Property ''{0}'' must be set.", Constants.PROPERTY_MMS_ARC3_SOURCEFILE));
        }
    }
}
