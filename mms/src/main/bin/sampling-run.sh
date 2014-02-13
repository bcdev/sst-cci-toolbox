#!/bin/bash

. mymms
. $MMS_HOME/bin/mms-env.sh

year=$1
month=$2
d=`date +%s -u -d "$year-$month-01 00:00:00"`
let d1="d - 8640 * 175"
let d2="d + 8640 * 525"
starttime=`date +%Y-%m-%dT%H:%M:%SZ -u -d @$d1`
stoptime=`date +%Y-%m-%dT%H:%M:%SZ -u -d @$d2`

echo "`date -u +%Y%m%d-%H%M%S` sampling $year/$month ..."

if [ "$year" = "" -o "$month" = "" ]; then
    echo "missing parameter, use $0 year month"
    exit 1
fi

echo $MMS_HOME/bin/sampling-tool.sh -c $MMS_CONFIG -debug \
-Dmms.sampling.startTime=${starttime} \
-Dmms.sampling.stopTime=${stoptime} \
-Dmms.sampling.sensor=atsr_orb.3 \
-Dmms.sampling.sensor2=atsr_orb.2 \
-Dmms.sampling.count=20000000 \
-Dmms.sampling.cleanupinterval=true
$MMS_HOME/bin/sampling-tool.sh -c $MMS_CONFIG -debug \
-Dmms.sampling.startTime=${starttime} \
-Dmms.sampling.stopTime=${stoptime} \
-Dmms.sampling.sensor=atsr_orb.3 \
-Dmms.sampling.sensor2=atsr_orb.2 \
-Dmms.sampling.count=20000000 \
-Dmms.sampling.cleanupinterval=true

echo "`date -u +%Y%m%d-%H%M%S` sampling $year/$month ... done"
