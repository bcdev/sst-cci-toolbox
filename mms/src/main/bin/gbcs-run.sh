#!/bin/bash
# gbcs-run.sh 2003 01 atsr.3 mms2
# mms/archive/mms2/sub/atsr.3/2003/atsr.3-sub-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-nwp-2003-01.nc
# mms/archive/mms2/arc/atsr.3/2003/atsr.3-nwp-2003-01.nc

year=$1
month=$2
sensor=$3
usecase=$4

. mymms

mkdir -p ${MMS_ARCHIVE}/${usecase}/arc/${sensor}/${year}

echo "`date -u +%Y%m%d-%H%M%S` gbcs ${year}/${month} sensor ${sensor}..."

gbcs-tool.sh -c ${MMS_HOME}/config/${usecase}-config.properties \
-Dmms.gbcs.sensor=${sensor} \
-Dmms.gbcs.mmd.source=${MMS_ARCHIVE}/${usecase}/sub/${sensor}/${year}/${sensor}-sub-${year}-${month}.nc \
-Dmms.gbcs.nwp.source=${MMS_ARCHIVE}/${usecase}/nwp/${sensor}/${year}/${sensor}-nwp-${year}-${month}.nc \
-Dmms.gbcs.mmd.target=${MMS_ARCHIVE}/${usecase}/arc/${sensor}/${year}/${sensor}-arc-${year}-${month}.nc \
