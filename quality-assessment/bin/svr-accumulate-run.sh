#!/bin/bash
set -e

sensor=$1
report_dirpath=$2
summary_report_pathname=$3

${SVR_PYTHON_EXEC} ${MMS_HOME}/cci/sst/qa/reportaccumulator.py $report_dirpath $summary_report_pathname
