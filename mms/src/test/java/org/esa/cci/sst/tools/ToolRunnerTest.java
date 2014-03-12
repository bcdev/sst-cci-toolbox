package org.esa.cci.sst.tools;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class ToolRunnerTest {

    /*
read_task_jobs() {
    jobname=$1
    jobs=
    if [ -e ${MMS_TASKS}/${jobname}.tasks ]
    then
        for logandid in `cat ${MMS_TASKS}/${jobname}.tasks`
        do
            job=`basename ${logandid}`
            log=`dirname ${logandid}`
            if [ "${jobs}" != "" ]
            then
                jobs="${jobs}|${job}"
            else
                jobs="${job}"
            fi
        done
        test ${jobs} || jobs=none
    fi
}

wait_for_task_jobs_completion() {
    step=$1
    while true
    do
        sleep 60

        echo "`date -u +%Y%m%d-%H%M%S` inquiring jobs ${jobs} for ${jobname}"
        # output of bjobs command
        # jobs=7948
        # JOBID   USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME
        # 7948    mboettc RUN   lotus      lotus.jc.rl host045.jc. *g-2003-01 Feb 13 13:13
        #if ssh -A lotus.jc.rl.ac.uk bjobs -P esacci_sst | egrep -q "^$jobs\\>"
        if bjobs -P esacci_sst | egrep -q "^$jobs\\>"
        then
            continue
        fi

        sleep 60

        if [ -s ${MMS_TASKS}/${jobname}.tasks ]
        then
            for logandid in `cat ${MMS_TASKS}/${jobname}.tasks`
            do
                job=`basename ${logandid}`
                log=`dirname ${logandid}`

                if ! grep -qF 'Successfully completed.' ${log}
                then
                    if [ -s ${log} ]
                    then
                        echo "tail -n10 ${log}"
                        tail -n10 ${log}
                    else
                        echo "`date -u +%Y%m%d-%H%M%S` logfile ${log} for job ${job} not found"
                    fi
                    echo "`date -u +%Y%m%d-%H%M%S` tasks for ${jobname} failed"
                    exit 1
                fi
            done
            echo "`date -u +%Y%m%d-%H%M%S` tasks for ${jobname} done"
            exit 0
        fi
    done
}

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
    public void testReadTaskJobs() throws Exception {
        final ToolRunner toolRunner = new ToolRunner();

        final String[] args = new String[]{};

        toolRunner.readTaskJobs(args);

    }

    @Test
    public void testWaitForCompletion() throws Exception {
        final ToolRunner toolRunner = new ToolRunner();

        final String[] args = new String[]{};

        toolRunner.waitForCompletion(null);

    }


}
