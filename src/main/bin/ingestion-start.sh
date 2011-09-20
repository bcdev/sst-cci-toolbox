#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks to ingest $year/$month"

read_task_jobs ingestion

if [ -z $jobs ]; then
    line=`qsub -l h_rt=24:00:00,sages_1ppn=1 -j y -cwd -o $MMS_LOG/ingestion-$year-$month.out -N in-$year$month $MMS_HOME/bin/ingestion-run.sh $year $month`
    echo $line
    jobs=`echo $line | awk '{ print $3 }'`
    echo "$MMS_LOG/ingestion-$year-$month.out/$job" > $MMS_TASKS/ingestion-$year-$month.tasks
fi

wait_for_task_jobs_completion ingestion
