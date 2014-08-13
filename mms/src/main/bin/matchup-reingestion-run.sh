#!/bin/bash
set -e
# reingestion-run.sh 2003 01 atsr.3 sub mms2
# mms/archive/mms2/sub/atsr.3/2003/atsr.3-sub-2003-01.nc
# mms/archive/mms2/arc/atsr.3/2003/atsr.3-arc-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-nwp-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-ecmwf-2003-01.nc

year=$1
month=$2
sensor=$3
usecase=$4

. mymms

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"

echo "`date -u +%Y%m%d-%H%M%S` matchup-reingestion ${year}/${month} sensor ${sensor} ..."

reingestion-tool.sh -c ${MMS_HOME}/config/${usecase}-config.properties \
-Dmms.db.useindex=true \
-Dmms.reingestion.source=${MMS_ARCHIVE}/${usecase}/nwp/${sensor}/${year}/${sensor}-ecmwf-${year}-${month}.nc \
-Dmms.reingestion.sensor=ecmwf \
-Dmms.reingestion.pattern=0 \
-Dmms.reingestion.located=false \
-Dmms.reingestion.overwrite=true
