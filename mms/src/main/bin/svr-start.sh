#!/bin/bash

. ${mms.home}/bin/mms-env-svr.sh

year=$1
month=$2
day=$3
sensor=$4
usecase=$5
version=$6
archive_root=$7
report_root=$8

task="svr"
jobname="$task-$year-$month-$day-$sensor-$usecase"
command="$task-run.sh $year $month $day $sensor $usecase $version $archive_root $report_root"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '$jobname'"

read_task_jobs $jobname

if [ -z $jobs ]; then
    submit_job 8192 01:30 $jobname $command
fi

wait_for_task_jobs_completion $jobname
