#!/bin/bash
set -e
# example usage: reingestion-run.sh 2003 01 atsr.3 sub mms2

year=$1
month=$2
sensor=$3
mmdtype=$4
usecase=$5

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"

echo "`date -u +%Y%m%d-%H%M%S` reingestion ${year}/${month} sensor ${sensor} type ${mmdtype} ..."

${mms_home}/bin/reingestion-tool.sh -c ${mms_home}/config/${usecase}-config.properties \
-Dmms.db.useindex=true \
-Dmms.reingestion.source=${mms_archive_root}/${usecase}/${mmdtype}/${sensor}/${year}/${sensor}-${mmdtype}-${year}-${month}.nc \
-Dmms.reingestion.sensor=${mmdtype}_${sensor} \
-Dmms.reingestion.pattern=0 \
-Dmms.reingestion.located=false \
-Dmms.reingestion.overwrite=true
