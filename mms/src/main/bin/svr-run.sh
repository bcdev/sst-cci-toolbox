#!/bin/bash
set -e

year=$1
month=$2
sensor=$3
usecase=$4
version=$5
archive_root=$6
report_root=$7

${mms.python.exec} ${mms.home}/python/svrrunner.py $year $month $sensor $usecase $version $archive_root $report_root
