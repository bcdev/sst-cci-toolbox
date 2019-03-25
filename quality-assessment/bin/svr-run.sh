#!/bin/bash
set -e

. /gws/nopw/j04/esacci_sst/mms/software/quality-assessment/bin/mms-env-svr.sh

year=$1
month=$2
day=$3
sensor=$4
usecase=$5
version=$6
archive_root=$7
report_root=$8

${PM_PYTHON_EXEC} ${PM_EXE_DIR}/cci/sst/qa/svrrunner.py $year $month $day $sensor $usecase $version $archive_root $report_root
