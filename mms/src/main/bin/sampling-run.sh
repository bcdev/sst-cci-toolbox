#!/bin/bash
# sampling-run.sh 2003 01 atsr.3 300000 0 mms2
# mms/archive/atsr.3/v2.1/2003/01/17/ATS_TOA_1P...N1
# mms/archive/mms2/smp/atsr.3/2003/atsr.3-smp-2003-01-b.txt

year=$1
month=$2
sensor=$3
count=$4
skip=$5
usecase=$6

. mymms

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"
starttime=${year}-${month}-01T00:00:00Z
stoptime=`date +%Y-%m -u -d @${d1}`-01T00:00:00Z

echo "`date -u +%Y%m%d-%H%M%S` sampling ${year}/${month} sensor ${sensor} starttime ${starttime} stoptime ${stoptime}..."

sampling-tool.sh -c ${MMS_HOME}/config/${usecase}-config.properties \
-Dmms.usecase=${usecase} \
-Dmms.sampling.startTime=${starttime} \
-Dmms.sampling.stopTime=${stoptime} \
-Dmms.sampling.sensor=${sensor} \
-Dmms.sampling.count=${count} \
-Dmms.sampling.skip=${skip}

echo "`date -u +%Y%m%d-%H%M%S` sampling ${year}/${month} sensor ${sensor} completed"
