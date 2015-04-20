#!/bin/bash
set -e

year=$1
month=$2
day=$3
sensor=$4
usecase=$5
version=$6
archive_root=$7
report_root=$8

${mms.python.exec} ${mms.home}/python/svrrunner.py $year $month $day $sensor $usecase $version $archive_root $report_root
