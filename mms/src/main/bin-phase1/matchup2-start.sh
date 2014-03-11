#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks to matchup2 $year/$month"

read_task_jobs matchup2

if [ -z $jobs ]; then
    line=`qsub -l h_rt=24:00:00 -pe memory-2G 3 -j y -cwd -o $MMS_LOG/matchup2-$year-$month.out -N m2-$year$month $MMS_HOME/bin/matchup2-run.sh $year $month`
    echo $line
    jobs=`echo $line | awk '{ print $3 }'`
    echo "$MMS_LOG/matchup2-$year-$month.out/$jobs" > $MMS_TASKS/matchup2-$year-$month.tasks
fi

wait_for_task_jobs_completion matchup2
