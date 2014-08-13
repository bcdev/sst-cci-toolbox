#!/bin/bash
set -e
# mmd-run.sh 2003 01 atsr.3 sub mms2
# mms/archive/atsr.3/v2.1/2003/01/17/ATS_TOA_1P...N1
# mms/archive/mms2/sub/atsr.3/2003/atsr.3-sub-2003-01.nc

year=$1
month=$2
sensors=$3
mmdtype=$4
usecase=$5

. mymms

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"
starttime=${year}-${month}-01T00:00:00Z
stoptime=`date +%Y-%m -u -d @${d1}`-01T00:00:00Z

mkdir -p ${MMS_ARCHIVE}/${usecase}/${mmdtype}/${sensors}/${year}

echo "`date -u +%Y%m%d-%H%M%S` mmd ${year}/${month} sensors ${sensors} type ${mmdtype} starttime ${starttime} stoptime ${stoptime}..."

mmd-tool.sh -c ${mms.home}/config/${usecase}-config.properties \
-Dmms.target.startTime=${starttime} \
-Dmms.target.stopTime=${stoptime} \
-Dmms.mmd.sensors=${sensors} \
-Dmms.db.useindex=false \
-Dmms.target.variables=${mms.home}/config/${mmdtype}-variables.config \
-Dmms.target.dir=${MMS_ARCHIVE}/${usecase}/${mmdtype}/${sensors}/${year} \
-Dmms.target.filename=${sensors}-${mmdtype}-${year}-${month}.nc
