#!/bin/bash

. ${mms.home}/bin/mms-env.sh

year=$1
month=$2
sensor=$3
usecase=$4
version=$5
archive_root=$6
report_root=$7

task="regrid"
jobname="$task-$year-$month-$day-$sensor-$usecase"
command="$task-run.sh $year $month $sensortype $sensor $usecase $version $archive_root $report_root"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '$jobname'"

read_task_jobs $jobname

if [ -z $jobs ]; then
    submit_job $jobname $command
fi

wait_for_task_jobs_completion $jobname
