#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2
sensor=$3
usecase=$4

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks for arc3 $year/$month/$sensor/$usecase"

read_task_jobs arc3

if [ -z $jobs ]; then
    if [ ! -e $MMS_ARCHIVE/$sensor/v1/$year/$month ]; then
        continue;
    fi

    echo "`date -u +%Y%m%d-%H%M%S` submitting job arc3 $year/$month sensor $sensor usecase $usecase"
    # TODO - where is the ARC script actually submitted to lotus?
    line=`bsub -q lotus -n 1 -P esacci_sst -o $MMS_LOG/arc3-$year-$month-$sensor.out -e $MMS_LOG/arc3-$year-$month-$sensor.err -J arc3-$year$month-$sensor $MMS_HOME/bin/arc3-run.sh $year $month $sensor`
    echo $line
    job=`echo $line | awk '{ print $sensor }'`
    if [ "$jobs" != "" ]
    then
        jobs="$jobs|$job"
    else
        jobs="$job"
    fi
    echo "$MMS_LOG/arc3-$year-$month-$sensor.out/$job" >> $MMS_TASKS/arc3-$year-$month.tasks
fi

wait_for_task_jobs_completion arc3
