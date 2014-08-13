#!/bin/bash
set -e
# mapplot-run.sh 2003 01 dum_atsr.3 lonlat mms2
# mapplot-run.sh 2003 01 dum_atsr.3 timlat mms2
# mms/archive/mms2/plt/dum_atsr.3/2003/atsr.3-lonlat-2003-01.png
# mms/archive/mms2/plt/dum_atsr.3/2003/atsr.3-timlat-2003-01.png

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

mapplot-tool.sh -c ${mms.home}/config/${usecase}-config.properties \
-Dmms.mapplot.starttime=${starttime} \
-Dmms.mapplot.stoptime=${stoptime} \
-Dmms.mapplot.sensor=${sensor} \
-Dmms.mapplot.show=false \
-Dmms.mapplot.strategy=${strategy} \
-Dmms.mapplot.target.dir=${MMS_ARCHIVE}/${usecase}/plt/${sensor}/${year} \
-Dmms.mapplot.target.filename=${sensor}-${strategy}-${year}-${month}.png
