#!/bin/bash
set -e

usecase=$1
sensor=$2
summary_report_pathname=$3
figure_dirpath=$4

${mms_python_exec} ${mms_home}/python/reportplotter.py $usecase $sensor $summary_report_pathname $figure_dirpath
