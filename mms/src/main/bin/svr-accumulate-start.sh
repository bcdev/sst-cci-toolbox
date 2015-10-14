#!/bin/bash

. ${mms_home}/bin/mms-env-svr.sh

sensor=$1
report_dirpath=$2
summary_report_pathname=$3

task="svr-accumulate"
jobname="$task-$sensor"
command="$task-run.sh $sensor $report_dirpath $summary_report_pathname"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '$jobname'"

read_task_jobs $jobname

if [ -z $jobs ]; then
    submit_job 8192 0:30 $jobname $command
fi

wait_for_task_jobs_completion $jobname
