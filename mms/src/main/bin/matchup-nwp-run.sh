#!/bin/bash
set -e
# example usage: nwp-matchup-run.sh 2003 01 atsr.3 mms2

year=$1
month=$2
sensor=$3
usecase=$4

mkdir -p ${mms_archive_root}/${usecase}/matchup/${sensor}/${year}

echo "`date -u +%Y%m%d-%H%M%S` matchup-nwp ${year}/${month} sensor ${sensor}..."

${mms_home}/bin/nwp-tool.sh -c ${mms_home}/config/${usecase}-config.properties \
-Dmms.nwp.forsensor=false \
-Dmms.nwp.mmd.source=${mms_archive_root}/${usecase}/sub/${sensor}/${year}/${sensor}-sub-${year}-${month}.nc \
-Dmms.nwp.nwp.source=${mms_archive_root}/era-interim/v1 \
-Dmms.nwp.nwp.target=${mms_archive_root}/${usecase}/matchup/${sensor}/${year}/${sensor}-matchup-${year}-${month}.nc
