#!/bin/bash
# coincidence-run.sh 2003 01 atsr.3 mms2

year=$1
month=$2
sensor=$3
usecase=$4

d=`date +%s -u -d "$year-$month-01 00:00:00"`
let d1="d + 32 * 86400"
starttime=$year-$month-01T00:00:00Z
stoptime=`date +%Y-%m -u -d @$d1`-01T00:00:00Z

matchup-tool.sh -c ${MMS_HOME}/config/${usecase}-config.properties \
-Dmms.matchup.startTime=${starttime} \
-Dmms.matchup.stopTime=${stoptime} \
-Dmms.matchup.primarysensor=${sensor}
