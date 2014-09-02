#!/bin/bash
set -e
# example usage: reingestion-run.sh 2003 01 atsr.3 sub mms2

year=$1
month=$2
sensor=$3
usecase=$4

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"

echo "`date -u +%Y%m%d-%H%M%S` matchup-reingestion ${year}/${month} sensor ${sensor} ..."

${mms.home}/bin/reingestion-tool.sh -c ${mms.home}/config/${usecase}-config.properties \
-Dmms.db.useindex=true \
-Dmms.reingestion.source=${mms.archive.root}/${usecase}/matchup/${sensor}/${year}/${sensor}-matchup-${year}-${month}.nc \
-Dmms.reingestion.sensor=matchup \
-Dmms.reingestion.pattern=0 \
-Dmms.reingestion.located=false \
-Dmms.reingestion.overwrite=true
