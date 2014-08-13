#!/bin/bash
set -e
# nwp-matchup-run.sh 2003 01 atsr.3 mms2
# mms/archive/ecmwf-era-interim/v01/2003/01/...
# mms/archive/mms2/sub/atsr.3/2003/atsr.3-sub-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-nwp-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-ecmwf-2003-01.nc

year=$1
month=$2
sensor=$3
usecase=$4

. mymms

mkdir -p ${mms.archive.root}/${usecase}/nwp/${sensor}/${year}

echo "`date -u +%Y%m%d-%H%M%S` matchup-nwp ${year}/${month} sensor ${sensor}..."

nwp-tool.sh -c ${mms.home}/config/${usecase}-config.properties \
-Dmms.target.dimensions=${mms.home}/config/mmd-dimensions.properties \
-Dmms.nwp.forsensor=false \
-Dmms.nwp.mmd.source=${mms.archive.root}/${usecase}/sub/${sensor}/${year}/${sensor}-sub-${year}-${month}.nc \
-Dmms.nwp.nwp.source=${mms.archive.root}/era-interim/v1 \
-Dmms.nwp.nwp.target=${mms.archive.root}/${usecase}/nwp/${sensor}/${year}/${sensor}-ecmwf-${year}-${month}.nc
