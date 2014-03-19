#!/bin/bash

. ${MMS_HOME}/bin/mms-env.sh
cd ${MMS_INST}

year=$1
month=$2
sensor=$3
sensorprefix=$4
usecase=$5

task="coincidence"
jobname="${task}-${year}-${month}-${sensorprefix}_${sensor}"
command="${task}-run.sh ${year} ${month} ${sensor} ${sensorprefix} ${usecase}"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for usecase ${usecase}"

read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname}
