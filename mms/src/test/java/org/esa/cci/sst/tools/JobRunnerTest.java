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

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @author Ralf Quast
 */
public class JobRunnerTest {

    /*
 submit_job() {
    jobname=$1
    command=$2
    bsubmit="bsub -q lotus -n 1 -W 04:00 -P esacci_sst -cwd ${MMS_INST} -oo ${MMS_LOG}/${jobname}.out -eo ${MMS_LOG}/${jobname}.err -J ${jobname} ${MMS_HOME}/bin/${command} ${@:3}"

    rm -f ${MMS_LOG}/${jobname}.out
    rm -f ${MMS_LOG}/${jobname}.err

    if hostname | grep -qF 'lotus.jc.rl.ac.uk'
    then
        echo "${bsubmit}"
        line=`${bsubmit}`
    else
        echo "ssh -A lotus.jc.rl.ac.uk ${bsubmit}"
        line=`ssh -A lotus.jc.rl.ac.uk ${bsubmit}`
    fi

    echo ${line}
    if echo ${line} | grep -qF 'is submitted'
    then
        jobs=`echo ${line} | awk '{ print substr($2,2,length($2)-2) }'`
        echo "${MMS_LOG}/${jobname}.out/${jobs}" > ${MMS_TASKS}/${jobname}.tasks
    fi
}
     */

    @Test
    public void testBuildCommand_ForLotus() throws Exception {
        final JobRunner runner = new JobRunner("bsub");

        final String jobname = "testjob";
        runner.option("-q", "lotus");
        runner.option("-P", "esacci_sst");
        runner.option("-oo", jobname + ".out");
        runner.option("-eo", jobname + ".err");
        runner.option("-J", jobname);
        runner.option("-n", "1");
        runner.option("-cwd", ".");
        runner.option("-W", "04:00");

        final String command = runner.buildCommand("sleep 100");
        assertEquals(
                "bsub -J testjob -P esacci_sst -W 04:00 -cwd . -eo testjob.err -n 1 -oo testjob.out -q lotus sleep 100",
                command);
    }

    @Test
    public void testResponseLine() throws Exception {
        final File executable = new File("/bin/echo");
        if (executable.canExecute()) {
            final JobRunner runner = new JobRunner("echo");
            final String jobname = "testjob";
            final String command = "Job <1711> is submitted successfully";

            final String message = runner.submit(jobname, command);
            assertEquals("Job <1711> is submitted successfully", message);
        }
    }


    private static class JobRunner {

        private final String exec;
        private final SortedMap<String, String> options;
        private final Logger logger;


        private JobRunner(String exec) {
            this(exec, null);
        }

        private JobRunner(String exec, Logger logger) {
            this.exec = exec;
            this.options = new TreeMap<>();
            this.logger = logger;
        }

        public JobRunner option(String option, String value) {
            this.options.put(option, value);
            return this;
        }

        public String submit(String jobname, String job) throws IOException, InterruptedException {
            try {
                if (logger != null && logger.isLoggable(Level.FINER)) {
                    logger.entering(JobRunner.class.getName(), "submit");
                }
                if (job == null) {
                    return "";
                }
                if (job.isEmpty()) {
                    return "";
                }
                final String command = buildCommand(job);
                if (logger != null && logger.isLoggable(Level.INFO)) {
                    logger.info(
                            MessageFormat.format("Submitting job {0} with command <code>{1}</code>", jobname, command));
                }
                final Process process = Runtime.getRuntime().exec(command);
                if (process.waitFor() != 0) {
                    throw new RuntimeException(
                            MessageFormat.format("Failed to submit job {0}. Exit value is {1}.", jobname,
                                                 process.exitValue()));
                }
                final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                final String line = reader.readLine();
                if (logger != null && logger.isLoggable(Level.INFO)) {
                    logger.info(MessageFormat.format("Job {0} is submitted successfully: {1}", jobname, line));
                }
                return line;
            } finally {
                if (logger != null && logger.isLoggable(Level.FINER)) {
                    logger.exiting(JobRunner.class.getName(), "submit");
                }
            }
        }

        // package public for testing
        String buildCommand(String job) {
            final StringBuilder commandBuilder = new StringBuilder(exec);
            for (Map.Entry<String, String> option : options.entrySet()) {
                commandBuilder.append(' ');
                commandBuilder.append(option.getKey());
                commandBuilder.append(' ');
                commandBuilder.append(option.getValue());
            }
            commandBuilder.append(' ');
            commandBuilder.append(job);

            return commandBuilder.toString();
        }

    }
}
