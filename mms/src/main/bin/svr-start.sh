#!/bin/bash

. ${mms.home}/bin/mms-env.sh

year=$1
month=$2
sensor=$3
usecase=$4
version=$5
archive_root=$6

task="svr"
jobname="${task}-${year}-${month}-${sensor}-${usecase}"
command="${mms.python.exec} ${mms.home}/python/svrrunner.py ${year} ${month} ${sensor} ${usecase} ${version} ${archive_root}"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}'"

read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname}
