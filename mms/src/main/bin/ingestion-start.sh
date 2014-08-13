#!/bin/bash

. ${mms.home}/bin/mms-env.sh
cd ${MMS_INST}

year=$1
month=$2
usecase=$3

task="ingestion"
jobname="${task}-${year}-${month}"
command="${task}-run.sh ${year} ${month} ${usecase}"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for usecase ${usecase}"

read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname}
