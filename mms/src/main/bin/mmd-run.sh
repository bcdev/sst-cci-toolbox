#!/bin/bash
set -e
# example usage: mmd-run.sh 2003 01 atsr.3 sub mms2

year=$1
month=$2
sensors=$3
mmdtype=$4
usecase=$5

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"
starttime=${year}-${month}-01T00:00:00Z
stoptime=`date +%Y-%m -u -d @${d1}`-01T00:00:00Z

mkdir -p ${mms_archive_root}/${usecase}/${mmdtype}/${sensors}/${year}

echo "`date -u +%Y%m%d-%H%M%S` mmd ${year}/${month} sensors ${sensors} type ${mmdtype} starttime ${starttime} stoptime ${stoptime}..."

${mms_home}/bin/mmd-tool.sh -c ${mms_home}/config/${usecase}-config.properties \
-Dmms.target.startTime=${starttime} \
-Dmms.target.stopTime=${stoptime} \
-Dmms.mmd.sensors=${sensors} \
-Dmms.db.useindex=false \
-Dmms.target.variables=${mms_home}/config/${mmdtype}-variables.config \
-Dmms.target.dir=${mms_archive_root}/${usecase}/${mmdtype}/${sensors}/${year} \
-Dmms.target.filename=${sensors}-${mmdtype}-${year}-${month}.nc
