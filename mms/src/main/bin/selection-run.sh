#!/bin/bash
set -e
# example usage: gbcs-run.sh 2003 01 atsr.3 mms2

year=$1
month=$2
sensors=$3
mmdtype=$4
usecase=$5

echo "`date -u +%Y%m%d-%H%M%S` selection ${year}/${month} sensors ${sensors} type ${mmdtype} ..."

${mms.home}/bin/selection-tool.sh -c ${mms.home}/config/${usecase}-config.properties \
-Dmms.selection.source=${mms.archive.root}/${usecase}/${mmdtype}/${sensors}/${year}/${sensors}-${mmdtype}-${year}-${month}-all.nc \
-Dmms.selection.target=${mms.archive.root}/${usecase}/${mmdtype}/${sensors}/${year}/${sensors}-${mmdtype}-${year}-${month}-sel.nc
