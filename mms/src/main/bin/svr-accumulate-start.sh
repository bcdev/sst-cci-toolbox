#!/bin/bash

. ${mms.home}/bin/mms-env.sh

report_dirpath=$1
summary_report_pathname=$2

task="svr-accumulate"
jobname="$task-$report_dirpath-$summary_report_pathname"
command="$task-run.sh $report_dirpath $summary_report_pathname"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '$jobname'"

read_task_jobs $jobname

if [ -z $jobs ]; then
    submit_job $jobname $command
fi

wait_for_task_jobs_completion $jobname
