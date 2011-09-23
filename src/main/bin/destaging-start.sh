#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks to de-stage $year/$month"

read_task_jobs destaging

if [ -z $jobs ]; then
    line=`qsub -l h_rt=24:00:00 -pe staging 1 -j y -cwd -o $MMS_LOG/destaging-$year-$month.out -N de-$year$month $MMS_HOME/bin/destaging-run.sh $year $month`
    echo $line
    jobs=`echo $line | awk '{ print $3 }'`
    echo "$MMS_LOG/destaging-$year-$month.out/$jobs" > $MMS_TASKS/destaging-$year-$month.tasks
fi

wait_for_task_jobs_completion destaging
