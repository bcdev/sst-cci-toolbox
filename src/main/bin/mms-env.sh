#!/bin/bash
# sets MMS_ variables for -start.sh, -run.sh, -tool.sh scripts

set -e # one fails, all fail
set -a
ulimit -s unlimited
umask 007

test $MMS_INST    || MMS_INST=`pwd`

if [ -z $MMS_HOME ]; then
    bindir=`dirname $0`
    basedir=`cd $bindir/..; pwd`
    export MMS_HOME="$basedir"
fi

if [ $MMS_CONFIG ]; then
    continue
elif [ -e $MMS_INST/mms-eddie.properties ]; then
    MMS_CONFIG=$MMS_INST/mms-eddie.properties
else
    MMS_CONFIG=$MMS_HOME/config/mms-eddie.properties
fi

test $MMS_ARCHIVE || MMS_ARCHIVE=/exports/work/geos_gc_sst_cci/stagingarea
test $MMS_TEMP    || MMS_TEMP=/exports/work/geos_gc_sst_cci/temparea

test $MMS_GBCS    || MMS_GBCS=/exports/work/geos_gc_sst_cci/avhrr/GBCS
test $MMS_CDO     || MMS_CDO=/exports/work/geos_gc_sst_cci/mms
test $MMS_ARC3    || MMS_ARC3=/exports/work/geos_gc_sst_cci/mms/arc3

if [ $MMS_DEBUG ]; then
    MMS_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
else
    MMS_OPTIONS=
fi

MMS_TASKS=$MMS_INST/tasks
MMS_LOG=$MMS_INST/log

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
