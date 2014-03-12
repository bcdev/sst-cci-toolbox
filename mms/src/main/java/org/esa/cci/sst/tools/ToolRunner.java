/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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


import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// No unfinished job found

public class ToolRunner {

    public int start(String[] args) throws IOException, InterruptedException {
        final String taskName = args[0];
        final String execName = args[1];
        final String[] execArgs = Arrays.copyOfRange(args, 2, args.length);

        final CommandRunner commandRunner = new CommandRunner();
        final List<String> runningJobs = commandRunner.execute("inquire_job_status", "bjobs -w");

        final CommandBuilder commandBuilder = new CommandBuilder(execName);
        final String command = commandBuilder.build(execArgs);

        for (final String line : runningJobs) {
            if (line.contains(command)) {
                return 0;
            }
        }

        final String jobName = command.replaceFirst(execName, taskName).replaceAll(" ", "-");
        final CommandBuilder submitCommandBuilder = new CommandBuilder("bsub");
        submitCommandBuilder.option("-q", "lotus");
        submitCommandBuilder.option("-P", "esacci_sst");
        submitCommandBuilder.option("-oo", jobName + ".out");
        submitCommandBuilder.option("-eo", jobName + ".err");
        submitCommandBuilder.option("-n", "1");
        submitCommandBuilder.option("-cwd", ".");
        submitCommandBuilder.option("-W", "05:00");

        final List<String> response = commandRunner.execute(command, submitCommandBuilder.build(command));
        final int jobId = getJobIdFromSubmissionResponse(response.get(0));

        while (true) {
            final List<String> allJobs = commandRunner.execute("inquire_job_status", "bjobs -a -w");

            for (final String line : allJobs) {
                if (getJobIdFromStatusLine(line) == jobId) {
                    final Status status = getStatusFromStatusLine(line);
                    switch (status) {
                        case DONE:
                            // TODO - log successfully completed
                            return 0;
                        case PEND:
                            break;
                        case RUN:
                            break;
                        default:
                            // TODO - log exit status
                            return 1; // TODO - exit code
                    }
                    break;
                }
            }
            Thread.sleep(20000);
        }
    }

    private String getJobNameFromStatusLine(String line) {
        return null;
    }

    private Status getStatusFromStatusLine(String line) {
        return null;
    }

    private int getJobIdFromStatusLine(String line) {
        return 0;
    }

    private int getJobIdFromSubmissionResponse(String response) {
        return 0;
    }

    public void readTaskJobs(String[] args) {

    }

    public void waitForCompletion(String[] args) {

    }

    public enum Status {DONE, EXIT, PEND, RUN,}
}
