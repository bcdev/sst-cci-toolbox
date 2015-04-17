#!/bin/bash

. ${mms.home}/bin/mms-env.sh

usecase=$1
sensor=$2
summary_report_pathname=$3
figure_dirpath=$4

task="svr-plot"
jobname="$task-$usecase-$sensor"
command="$task-run.sh $usecase $sensor $summary_report_pathname $figure_dirpath"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '$jobname'"

read_task_jobs $jobname

if [ -z $jobs ]; then
    submit_job $jobname $command
fi

wait_for_task_jobs_completion $jobname
