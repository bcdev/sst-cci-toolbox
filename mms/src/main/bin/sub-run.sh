#!/bin/bash
set -e
# example usage: mmd-run.sh 2003 01 atsr.3 sub mms2

year=$1
month=$2
sensor=$3
mmdtype=$4
usecase=$5

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"
starttime=${year}-${month}-01T00:00:00Z
stoptime=`date +%Y-%m -u -d @${d1}`-01T00:00:00Z

mkdir -p ${mms.archive.root}/${usecase}/${mmdtype}/${sensor}/${year}

echo "`date -u +%Y%m%d-%H%M%S` sub ${year}/${month} sensor ${sensor} mmdtype ${mmdtype} starttime ${starttime} stoptime ${stoptime}..."

${mms.home}/bin/mmd-tool.sh -c ${mms.home}/config/${usecase}-config.properties \
-Dmms.target.startTime=${starttime} \
-Dmms.target.stopTime=${stoptime} \
-Dmms.mmd.sensors=${sensor} \
-Dmms.db.useindex=false \
-Dmms.target.variables=${mms.home}/config/${sensor}-${mmdtype}-variables.config \
-Dmms.target.dir=${mms.archive.root}/${usecase}/${mmdtype}/${sensor}/${year} \
-Dmms.target.filename=${sensor}-${mmdtype}-${year}-${month}.nc
