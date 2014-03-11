#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2
parts='a b c d e f'

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks for reingest3hl $year/$month"

read_task_jobs reingest3hl

if [ -z $jobs ]; then
    for part in $parts
    do

    echo "`date -u +%Y%m%d-%H%M%S` submitting job reingest3hl $year/$month $part"
    line=`qsub -l h_rt=24:00:00,sages_1ppn=1 -j y -cwd -o $MMS_LOG/reingest3hl-$year-$month-$part.out -N i3-$year$month$part $MMS_HOME/bin-hl/reingest3hl-run.sh $year $month $part`
    echo $line
    job=`echo $line | awk '{ print $3 }'`
    if [ "$jobs" != "" ]
    then
        jobs="$jobs|$job"
    else
        jobs="$job"
    fi
    echo "$MMS_LOG/reingest3hl-$year-$month-$part.out/$job" >> $MMS_TASKS/reingest3hl-$year-$month.tasks

    done
fi

wait_for_task_jobs_completion reingest3hl
