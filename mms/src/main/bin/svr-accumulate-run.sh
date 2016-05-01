#!/bin/bash
set -e

sensor=$1
report_dirpath=$2
summary_report_pathname=$3

${mms_python_exec} ${mms_home}/python/reportaccumulator.py $report_dirpath $summary_report_pathname
