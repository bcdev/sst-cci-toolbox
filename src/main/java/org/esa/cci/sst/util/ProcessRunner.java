/*
 * Copyright (C) 2011 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.cci.sst.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
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

    public void execute(final String command) throws Exception {
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
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(MessageFormat.format("executing process <code>{0}</code>", command));
            }
            final Process process = Runtime.getRuntime().exec(command);

            if (logger.isLoggable(Level.INFO)) {
                final LoggingThread err = new LoggingThread(process.getErrorStream());
                final LoggingThread out = new LoggingThread(process.getInputStream());

                err.start();
                out.start();
            }
            if (process.waitFor() != 0) {
                throw new Exception(
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