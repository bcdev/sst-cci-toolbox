#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks for reingest12 $year/$month"

read_task_jobs reingest12

if [ -z $jobs ]; then

    echo "`date -u +%Y%m%d-%H%M%S` submitting job reingest12hl $year/$month"
    line=`qsub -l h_rt=24:00:00,sages_1ppn=1 -j y -cwd -o $MMS_LOG/reingest12hl-$year-$month.out -N i1-$year$month $MMS_HOME/bin-hl/reingest12hl-run.sh $year $month`
    echo $line
    job=`echo $line | awk '{ print $3 }'`
    if [ "$jobs" != "" ]
    then
        jobs="$jobs|$job"
    else
        jobs="$job"
    fi
    echo "$MMS_LOG/reingest12hl-$year-$month.out/$job" >> $MMS_TASKS/reingest12hl-$year-$month.tasks

fi

wait_for_task_jobs_completion reingest12
