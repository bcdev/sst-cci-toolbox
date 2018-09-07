#!/bin/bash
set -e

. /group_workspaces/cems2/esacci_sst/mms/software/quality-assessment/bin/mms-env-svr.sh

usecase=$1
sensor=$2
summary_report_pathname=$3
figure_dirpath=$4

${SVR_PYTHON_EXEC} ${MMS_HOME}/cci/sst/qa/reportplotter.py $usecase $sensor $summary_report_pathname $figure_dirpath
