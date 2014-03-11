#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks to de-stage high latitude $year/$month"

read_task_jobs destaginghl

if [ -z $jobs ]; then
    line=`qsub -l h_rt=24:00:00 -pe staging 1 -j y -cwd -o $MMS_LOG/destaginghl-$year-$month.out -N dh-$year$month $MMS_HOME/bin-hl/destaginghl-run.sh $year $month`
    echo $line
    jobs=`echo $line | awk '{ print $3 }'`
    echo "$MMS_LOG/destaginghl-$year-$month.out/$jobs" > $MMS_TASKS/destaginghl-$year-$month.tasks
fi

wait_for_task_jobs_completion destaginghl
