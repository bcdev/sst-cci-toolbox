#!/bin/bash
set -e
# example usage: coincidence-run.sh 2003 01 atsr.3 mms2

year=$1
month=$2
sensor=$3
usecase=$4

d=`date +%s -u -d "$year-$month-01 00:00:00"`
let d1="d + 32 * 86400"
starttime=$year-$month-01T00:00:00Z
stoptime=`date +%Y-%m -u -d @$d1`-01T00:00:00Z

echo "`date -u +%Y%m%d-%H%M%S` coincidence ${year}/${month} sensor ${sensor}..."

${mms_home}/bin/matchup-tool.sh -c ${mms_home}/config/${usecase}-config.properties \
-Dmms.matchup.startTime=${starttime} \
-Dmms.matchup.stopTime=${stoptime} \
-Dmms.matchup.primarysensor=${sensor}
