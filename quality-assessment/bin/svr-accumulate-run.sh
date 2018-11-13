#!/bin/bash
set -e

. /group_workspaces/cems2/esacci_sst/mms/software/quality-assessment/bin/mms-env-svr.sh

sensor=$1
report_dirpath=$2
summary_report_pathname=$3

${PM_PYTHON_EXEC} ${PM_EXE_DIR}/cci/sst/qa/reportaccumulator.py $report_dirpath $summary_report_pathname
