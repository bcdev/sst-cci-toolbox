#!/bin/bash

# TODO - find out what this tool really does and cleanup

. mymms
. ${MMS_HOME}/bin/mms-env.sh

year=$1
month=$2
d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d2="d + 86400 * 32"
year2=`date +%Y -u -d @${d2}`
month2=`date +%m -u -d @${d2}`

echo "`date -u +%Y%m%d-%H%M%S` mapplot ${year}/${month} ..."

if [ "${year}" = "" -o "${month}" = "" ]; then
    echo "missing parameter, use $0 year month"
    exit 1
fi

${MMS_HOME}/bin/mapplot-tool.sh -c ${MMS_CONFIG} -debug \
-Dmms.sampling.startTime=${year}-${month}-01T00:00:00Z \
-Dmms.sampling.stopTime=${year2}-${month2}-01T00:00:00Z \
-Dmms.sampling.sensor=orb_atsr.3 \
-Dmms.sampling.showmaps=false
