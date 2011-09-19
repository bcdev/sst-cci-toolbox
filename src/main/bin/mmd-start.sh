#!/bin/bash

. `dirname $0`/mms-env.sh

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks to generate mmd $year/$month"

read_task_jobs mmd

if [ -z $jobs ]; then
    line=`qsub -l h_rt=47:00:00,s_fsize=10G -pe memory-2G 2 -j y -cwd -o $MMS_LOG/mmd-$year-$month.out -N in-$year$month $MMS_HOME/bin/mmd-run.sh $year $month`
    echo $line
    jobs=`echo $line | awk '{ print $3 }'`
    echo "$MMS_LOG/mmd-$year-$month.out/$job" > $MMS_TASKS/mmd-$year-$month.tasks
fi

wait_for_task_jobs_completion mmd
