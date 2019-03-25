#!/bin/bash
set -e

. /gws/nopw/j04/esacci_sst/mms/software/quality-assessment/bin/mms-env-svr.sh

usecase=$1
sensor=$2
summary_report_pathname=$3
figure_dirpath=$4

${PM_PYTHON_EXEC} ${PM_EXE_DIR}/cci/sst/qa/reportplotter.py $usecase $sensor $summary_report_pathname $figure_dirpath
