#!/bin/bash

. ${mms_home}/bin/mms-env.sh

year=$1
month=$2
sensors=$3
mmdtype=$4
seltype=$5
usecase=$6

task="selection"
jobname="${task}-${year}-${month}-${sensors}-${mmdtype}"
command="${task}-run.sh ${year} ${month} ${sensors} ${mmdtype} ${seltype} ${usecase}"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for usecase ${usecase}"

read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname}
