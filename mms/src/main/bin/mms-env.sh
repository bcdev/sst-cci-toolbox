#!/bin/bash

# MMS function definitions
# useage: . $MMS_HOME/bin/mms-env.sh  (in xxx-start.sh and xxx-run.sh)

set -e # one fails, all fail
#set -a # auto-export variables

MMS_OPTIONS=""
if [ ! -z $MMS_DEBUG ]; then
    MMS_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
fi

read_task_jobs() {
    step=$1
    jobs=
    if [ -e $MMS_TASKS/$step-$year-$month.tasks ]
    then
        for logandid in `cat $MMS_TASKS/$step-$year-$month.tasks`
        do
            job=`basename $logandid`
            log=`dirname $logandid`
            if [ "$jobs" != "" ]
            then
                jobs="$jobs|$job"
            else
                jobs="$job"
            fi
        done
        test $jobs || jobs=none
    fi
}

wait_for_task_jobs_completion() {
    step=$1
    while true
    do
        sleep 120
        
        echo "`date -u +%Y%m%d-%H%M%S` inquiring jobs $jobs for $step $year/$month"
        # TODO - replace 'qstat' with 'bjobs'?
        if qstat | egrep -q "$jobs"
        then
            continue
        fi
        
        for logandid in `cat $MMS_TASKS/$step-$year-$month.tasks`
        do
            job=`basename $logandid`
            log=`dirname $logandid`
            if ! tail -n1 $log | grep -q done
            then
                echo "tail -n10 $log"
                tail -n10 $log
                echo "`date -u +%Y%m%d-%H%M%S` tasks for $step $year/$month failed"
                exit 1
            fi
        done
        echo "`date -u +%Y%m%d-%H%M%S` tasks for $step $year/$month done"
        exit 0
    done
}
