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
package org.esa.cci.sst.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for running a system process, which redirects the output of the standard
 * streams into a logger.
 *
 * @author Ralf Quast
 */
public class ProcessRunner {

    private final Logger logger;

    public ProcessRunner(String loggerName) {
        logger = Logger.getLogger(loggerName);
    }

    public static File writeExecutableScript(String template, Properties properties) throws IOException {
        final File script = File.createTempFile("cdo", ".sh");
        final boolean executable = script.setExecutable(true);
        if (!executable) {
            throw new IOException("Cannot create executable script.");
        }
        final Writer writer = new FileWriter(script);
        try {
            final TemplateResolver templateResolver = new TemplateResolver(properties);
            writer.write(templateResolver.resolve(template));
        } finally {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
        return script;
    }

    public void execute(final String command) throws IOException, InterruptedException {
        try {
            if (logger.isLoggable(Level.FINER)) {
                logger.entering(ProcessRunner.class.getName(), "execute");
            }
            if (command == null) {
                return;
            }
            if (command.isEmpty()) {
                return;
            }
            if (logger.isLoggable(Level.INFO)) {
                logger.info(MessageFormat.format("executing process <code>{0}</code>", command));
            }
            final Process process = Runtime.getRuntime().exec(command);

            final LoggingThread err = new LoggingThread(process.getErrorStream());
            final LoggingThread out = new LoggingThread(process.getInputStream());
            err.start();
            out.start();

            if (process.waitFor() != 0) {
                throw new RuntimeException(
                        MessageFormat.format("Command <code>{0}</code> terminated with exit value {1}",
                                             command, process.exitValue()));
            }
        } finally {
            if (logger.isLoggable(Level.FINER)) {
                logger.exiting(ProcessRunner.class.getName(), "execute");
            }
        }
    }

    private class LoggingThread extends Thread {

        private final InputStream inputStream;

        private LoggingThread(final InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                while (true) {
                    final String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    logger.info(line);
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

}