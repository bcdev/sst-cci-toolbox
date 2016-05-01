#!/bin/bash
set -e
# example usage: gbcs-run.sh 2003 01 atsr.3 mms2

year=$1
month=$2
sensor=$3
usecase=$4

mkdir -p ${mms_archive_root}/${usecase}/arc/${sensor}/${year}

echo "`date -u +%Y%m%d-%H%M%S` gbcs ${year}/${month} sensor ${sensor}..."

${mms_home}/bin/gbcs-tool.sh -c ${mms_home}/config/${usecase}-config.properties \
-Dmms.gbcs.sensor=${sensor} \
-Dmms.gbcs.mmd.source=${mms_archive_root}/${usecase}/sub/${sensor}/${year}/${sensor}-sub-${year}-${month}.nc \
-Dmms.gbcs.nwp.source=${mms_archive_root}/${usecase}/nwp/${sensor}/${year}/${sensor}-nwp-${year}-${month}.nc \
-Dmms.gbcs.mmd.target=${mms_archive_root}/${usecase}/arc/${sensor}/${year}/${sensor}-arc-${year}-${month}.nc
