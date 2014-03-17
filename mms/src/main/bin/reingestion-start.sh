#!/bin/bash

. ${MMS_HOME}/bin/mms-env.sh
cd ${MMS_INST}

year=$1
month=$2
sensor=$3
mmdtype=$4
usecase=$5

task="reingestion"
jobname="${task}-${year}-${month}-${sensor}-${mmdtype}"
command="${task}-run.sh ${year} ${month} ${sensor} ${mmdtype} ${usecase}"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}' for usecase ${usecase}"

read_task_jobs ${jobname}

echo jobs: ${jobs}

if [ -z ${jobs} ]; then
    echo "submit_job ${jobname} ${command}"
fi

#wait_for_task_jobs_completion ${jobname}
