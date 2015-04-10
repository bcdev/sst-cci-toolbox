#!/bin/bash
set -e

report_dirpath=$1
summary_report_pathname=$2

${mms.python.exec} ${mms.home}/python/reportaccumulator.py $report_dirpath $summary_report_pathname
