#!/bin/bash

year=$1
month=$2
sensor=$3
strategy=$4
usecase=$5

. mymms

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"
starttime=${year}-${month}-01T00:00:00Z
stoptime=`date +%Y-%m -u -d @${d1}`-01T00:00:00Z

mkdir -p ${MMS_ARCHIVE}/${usecase}/plt/${sensor}/${year}

echo "`date -u +%Y%m%d-%H%M%S` mapplot ${year}/${month} sensor ${sensor} strategy ${strategy} starttime ${starttime} stoptime ${stoptime}..."

mapplot-tool.sh -c ${MMS_HOME}/config/${usecase}-config.properties \
-Dmms.mapplot.starttime=${starttime} \
-Dmms.mapplot.stoptime=${stoptime} \
-Dmms.mapplot.sensor=${sensor} \
-Dmms.sampling.show=false \
-Dmms.sampling.strategy=${strategy} \
-Dmms.sampling.target.dir=${MMS_ARCHIVE}/${usecase}/plt/${sensor}/${year} \
-Dmms.sampling.target.filename=${sensor}-${strategy}-${year}-${month}.png
