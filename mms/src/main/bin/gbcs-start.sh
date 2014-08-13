#!/bin/bash

. ${mms.home}/bin/mms-env.sh
cd ${MMS_INST}

year=$1
month=$2
sensor=$3
usecase=$4

task="gbcs"
jobname="${task}-${year}-${month}-${sensor}"
command="${task}-run.sh ${year} ${month} ${sensor} ${usecase}"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for usecase ${usecase}"

read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname}
