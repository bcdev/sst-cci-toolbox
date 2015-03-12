#!/bin/bash

. ${mms.home}/bin/mms-env.sh

python_path=$1
input_dir=$2
output_dir=$2
sensor_name=$3

jobname="svr-${input_dir}-${sensor_name}"
command="${python_path} svr_workflow.py ${input_dir} ${output_dir} ${sensor_name}"

echo "`date -u +%Y%m%d-%H%M%S` submitting job 'svr' for ${input_dir} and ${sensor_name}"

read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname}