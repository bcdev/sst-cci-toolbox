#!/bin/bash

# MMS function definitions
# useag${step}$MMS_HOME/bin/mms-env.sh  (in xxx-start.sh and xxx-run.sh)

set -e # one fa${step}all fail
#set -a # auto-export variables

MMS_OPTIONS=""
if [ ! -z ${MMS_DEBUG} ]; then
    MMS_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
fi

read_task_jobs() {
    jobname=$1
    jobs=
    if [ -e ${MMS_TASKS}/${jobname}.tasks ]
    then
        for logandid in `cat ${MMS_TASKS}/${jobname}.tasks`
        do
            job=`basename ${logandid}`
            log=`dirname ${logandid}`
            if grep -qF 'Successfully completed.' ${log}
            then
                if [ "${jobs}" != "" ]
                then
                    jobs="${jobs}|${job}"
                else
                    jobs="${job}"
                fi
            fi
        done
    fi
}

wait_for_task_jobs_completion() {
    jobname=$1
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

                if [ -s ${log} ]
                then
                    if ! grep -qF 'Successfully completed.' ${log}
                    then
                        echo "tail -n10 ${log}"
                        tail -n10 ${log}
                        echo "`date -u +%Y%m%d-%H%M%S` tasks for ${jobname} failed (reason: see ${log})"
                        exit 1
                    else
                        echo "`date -u +%Y%m%d-%H%M%S` tasks for ${jobname} done"
                        exit 0
                    fi
                else
                        echo "`date -u +%Y%m%d-%H%M%S` logfile ${log} for job ${job} not found"
                fi
            done
        fi
    done
}

submit_job() {
    jobname=$1
    command=$2
    bsubmit="bsub -R rusage[mem=8192] -q lotus -m host040 -n 1 -W 8:00 -P esacci_sst -cwd ${MMS_INST} -oo ${MMS_LOG}/${jobname}.out -eo ${MMS_LOG}/${jobname}.err -J ${jobname} ${MMS_HOME}/bin/${command} ${@:3}"

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
    else
        echo "`date -u +%Y%m%d-%H%M%S` tasks for ${jobname} failed (reason: was not submitted)"
        exit 1
    fi
}
