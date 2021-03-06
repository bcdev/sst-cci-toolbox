#!/bin/bash

. ${mms.home}/bin/mms-env.sh

year=$1
month=$2
sensor=$3
strategy=$4
usecase=$5

task="mapplot"
jobname="${task}-${year}-${month}-${sensor}-${strategy}"
command="${task}-run.sh ${year} ${month} ${sensor} ${strategy} ${usecase}"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for usecase ${usecase}"

read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname}

