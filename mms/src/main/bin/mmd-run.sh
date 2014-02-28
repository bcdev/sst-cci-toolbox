#!/bin/bash
# mmd-run.sh 2003 01 atsr.3 sub mms2
# mms/archive/atsr.3/v2.1/2003/01/17/ATS_TOA_1P...N1
# mms/archive/mms2/sub/atsr.3/2003/atsr.3-sub-2003-01.nc

year=$1
month=$2
sensor=$3
mmdtype=$4
usecase=$5

. mymms

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"
starttime=${year}-${month}-01T00:00:00Z
stoptime=`date +%Y-%m -u -d @${d1}`-01T00:00:00Z

mkdir -p $MMS_ARCHIVE/${usecase}/$mmdtype/$sensor/$year

mmd-tool.sh -c $usecase-config.xml \
-Dmms.target.startTime=$starttime \
-Dmms.target.stopTime=$stoptime \
-Dmms.db.useindex=false \
-Dmms.target.dimensions=$MMS_HOME/config/mmd-dimensions.properties \
-Dmms.target.variables=$MMS_HOME/config/$sensor-$mmdtype-variables.config \
-Dmms.target.dir=$MMS_ARCHIVE/$usecase/$mmdtype/$sensor/$year \
-Dmms.target.filename=$sensor-$mmdtype-$year-$month.nc
