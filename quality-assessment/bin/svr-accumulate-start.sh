#!/bin/bash

. /gws/nopw/j04/esacci_sst/mms/software/quality-assessment/bin/mms-env-svr.sh

sensor=$1
report_dirpath=$2
summary_report_pathname=$3

task="svr-accumulate"
jobname="$task-$sensor"
command="$task-run.sh $sensor $report_dirpath $summary_report_pathname"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '$jobname'"

if [ -z $jobs ]; then
    submit_job $jobname $command
fi
