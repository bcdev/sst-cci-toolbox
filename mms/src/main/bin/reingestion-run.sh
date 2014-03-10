#!/bin/bash
# reingestion-run.sh 2003 01 atsr.3 sub mms2
# mms/archive/atsr.3/v2.1/2003/01/17/ATS_TOA_1P...N1
# mms/archive/mms2/sub/atsr.3/2003/atsr.3-sub-2003-01.nc

year=$1
month=$2
sensor=$3
mmdtype=$4
usecase=$5

. mymms

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"

pattern=`cat ${MMS_HOME}/config/${usecase}-config.properties | awk "/mms.pattern.$sensor/ { print \\$3 }"`

echo "`date -u +%Y%m%d-%H%M%S` reingestion ${year}/${month} sensor ${sensor} type ${mmdtype} pattern ${pattern}..."

reingestion-tool.sh -c ${MMS_HOME}/config/${usecase}-config.properties \
-Dmms.reingestion.filename=${MMS_ARCHIVE}/${usecase}/${mmdtype}/${sensor}/${year}/${sensor}-${mmdtype}-${year}-${month}.nc \
-Dmms.reingestion.located=no \
-Dmms.reingestion.overwrite=true \
-Dmms.db.useindex=true \
-Dmms.reingestion.sensor=${mmdtype}_${sensor} \
-Dmms.reingestion.pattern=0
