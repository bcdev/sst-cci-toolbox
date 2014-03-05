#!/bin/bash
# nwp-run.sh 2003 01 atsr.3 mms2
# mms/archive/ecmwf-era-interim/v01/2003/01/...
# mms/archive/mms2/sub/atsr.3/2003/atsr.3-sub-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-nwp-2003-01.nc

year=$1
month=$2
sensor=$3
usecase=$4

. mymms

nwp-tool.sh -c ${MMS_HOME}/config/${usecase}-config.properties \
-Dmms.nwp.formatchuppoints=false \
-Dmms.nwp.sensor=${sensor} \
-Dmms.target.dimensions=${MMS_HOME}/config/mmd-dimensions.properties \
-Dmms.mmd.source=${MMS_ARCHIVE}/${usecase}/sub/${sensor}/${year}/${sensor}-sub-${year}-${month}.nc \
-Dmms.nwp.source=${MMS_ARCHIVE}/era-interim/v1 \
-Dmms.nwp.target=${MMS_ARCHIVE}/${usecase}/nwp/${sensor}/${year}/${sensor}-nwp-${year}-${month}.nc
