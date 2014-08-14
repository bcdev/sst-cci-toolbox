#!/bin/bash
set -e
# example usage: nwp-run.sh 2003 01 atsr.3 mms2

year=$1
month=$2
sensor=$3
usecase=$4

mkdir -p ${mms.archive.root}/${usecase}/nwp/${sensor}/${year}

echo "`date -u +%Y%m%d-%H%M%S` nwp ${year}/${month} sensor ${sensor}..."

${mms.home}/bin/nwp-tool.sh -c ${mms.home}/config/${usecase}-config.properties \
-Dmms.target.dimensions=${mms.home}/config/mmd-dimensions.properties \
-Dmms.nwp.forsensor=true \
-Dmms.nwp.sensor=${sensor} \
-Dmms.nwp.mmd.source=${mms.archive.root}/${usecase}/sub/${sensor}/${year}/${sensor}-sub-${year}-${month}.nc \
-Dmms.nwp.nwp.source=${mms.archive.root}/era-interim/v1 \
-Dmms.nwp.nwp.target=${mms.archive.root}/${usecase}/nwp/${sensor}/${year}/${sensor}-nwp-${year}-${month}.nc
