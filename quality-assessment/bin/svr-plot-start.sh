#!/bin/bash

. /group_workspaces/cems2/esacci_sst/mms/software/quality-assessment/bin/mms-env-svr.sh

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
    submit_job 0:30 $jobname $command
fi
