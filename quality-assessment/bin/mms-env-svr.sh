#!/bin/bash

# project name (to identify the job groups on the cluster)
export PROJECT=esacci_sst_svr

export PM_EXE_DIR='/gws/nopw/j04/esacci_sst/mms/software/quality-assessment'
export PM_PYTHON_EXEC='/gws/nopw/j04/esacci_sst/software/miniconda3/envs/sst_cci_qa/bin/python'

export PATH=${PM_EXE_DIR}/bin:$PATH

# ensure that processes exit
set -e

if [ -z "${WORKING_DIR}" ]; then
    WORKING_DIR=`pwd -P`
fi

export PM_LOG_DIR=${WORKING_DIR}/log

submit_job() {
    jobname=$1
    command=$2

     bsubmit="bsub -R rusage[mem=2048] -M 2048 -q short-serial -n 1 -W 1:00 -P ${PROJECT} -cwd ${WORKING_DIR} -oo ${PM_LOG_DIR}/${jobname}.out -eo ${PM_LOG_DIR}/${jobname}.err -J ${jobname} ${PM_EXE_DIR}/bin/${command} ${@}"

    rm -f ${PM_LOG_DIR}/${jobname}.out
    rm -f ${PM_LOG_DIR}/${jobname}.err

    # line contains the console output of the bsub command
    line=`${bsubmit}`

    if echo ${line} | grep -qF 'is submitted'
    then
        # extract the job_id from the bsub message, concatenate '_' and jobname to form an identifier
        # and dump to std_out to be fetched by pmonitor
        job_id=`echo ${line} | awk '{ print substr($2,2,length($2)-2) }'`
        echo "${job_id}_${jobname}"
    else
        echo "`date -u +%Y%m%d-%H%M%S` - submit of ${jobname} failed: ${line}"
        exit 1
    fi
}