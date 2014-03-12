/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ralf Quast
 */
class CommandRunner {

    private final Logger logger;


    CommandRunner() {
        this(null);
    }

    CommandRunner(Logger logger) {
        this.logger = logger;
    }

    List<String> execute(String jobName, String command) throws IOException, InterruptedException {
        try {
            if (logger != null && logger.isLoggable(Level.FINER)) {
                logger.entering(CommandRunner.class.getName(), "execute");
            }
            if (command == null) {
                return Collections.emptyList();
            }
            if (command.isEmpty()) {
                return Collections.emptyList();
            }
            if (logger != null && logger.isLoggable(Level.INFO)) {
                logger.info(
                        MessageFormat.format("executing job ''{0}'' with command ''{1}''", jobName, command));
            }
            final Process process = Runtime.getRuntime().exec(command);
            if (process.waitFor() != 0) {
                throw new RuntimeException(
                        MessageFormat.format("Failed to execute job ''{0}''. Exit value is {1}.", jobName,
                                             process.exitValue()));
            }
            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            final List<String> response = new LinkedList<>();
            while (true) {
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }
                response.add(line);
            }
            if (logger != null && logger.isLoggable(Level.INFO)) {
                logger.info(MessageFormat.format("job ''{0}'' is executed successfully", jobName));
            }
            return response;
        } finally {
            if (logger != null && logger.isLoggable(Level.FINER)) {
                logger.exiting(CommandRunner.class.getName(), "execute");
            }
        }
    }

}