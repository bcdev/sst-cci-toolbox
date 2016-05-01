#!/bin/bash

. ${mms_home}/bin/mms-env.sh

year=$1
month=$2
sensor=$3
archive_root=$4
target_root=$5

task="regrid"
jobname="$task-$year-$month-$sensor-$usecase"
command="$task-run.sh $year $month $sensor $archive_root $target_root"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '$jobname'"

read_task_jobs $jobname

if [ -z $jobs ]; then
    submit_job $jobname $command
fi

wait_for_task_jobs_completion $jobname
