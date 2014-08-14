#!/bin/bash
set -e
# example usage: clearsky-run.sh 2003 01 atsr.3 mms2

year=$1
month=$2
sensor=$3
usecase=$4

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"
starttime=${year}-${month}-01T00:00:00Z
stoptime=`date +%Y-%m -u -d @${d1}`-01T00:00:00Z

echo "`date -u +%Y%m%d-%H%M%S` clearsky ${year}/${month} sensor ${sensor} starttime ${starttime} stoptime ${stoptime}..."

${mms.home}/bin/clearsky-tool.sh -c ${mms.home}/config/${usecase}-config.properties \
-Dmms.usecase=${usecase} \
-Dmms.sampling.startTime=${starttime} \
-Dmms.sampling.stopTime=${stoptime} \
-Dmms.sampling.sensor=${sensor} \
-Dmms.sampling.cleanupinterval=true
