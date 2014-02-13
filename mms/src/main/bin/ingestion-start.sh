#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2
usecase=$3

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks to ingest $year/$month"

read_task_jobs ingestion

if [ -z $jobs ]; then
    line=`ssh lotus.jc.rl.ac.uk bsub -q lotus -n 1 -W 02:00 -P esacci_sst -cwd $MMS_INST -oo $MMS_LOG/ingestion-$year-$month.out -eo $MMS_LOG/ingestion-$year-$month.err -J ing-$year$month $MMS_HOME/bin/ingestion-run2.sh $year $month $usecase`
    echo $line
    jobs=`echo $line | awk '{ print substr($2,2,length($2)-2) }'`
    echo "$MMS_LOG/ingestion-$year-$month.out/$jobs" > $MMS_TASKS/ingestion-$year-$month.tasks
fi

wait_for_task_jobs_completion ingestion
