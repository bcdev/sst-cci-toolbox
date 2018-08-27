#!/bin/bash
set -e

usecase=$1
sensor=$2
summary_report_pathname=$3
figure_dirpath=$4

${SVR_PYTHON_EXEC} ${MMS_HOME}/cci/sst/qa/reportplotter.py $usecase $sensor $summary_report_pathname $figure_dirpath
