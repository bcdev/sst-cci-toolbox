#!/bin/bash
# nwpmatchup-run2.sh 2003 01 atsr.3 mms2
# mms/archive/ecmwf-era-interim/v01/2003/01/...
# mms/archive/mms2/sub/atsr.3/2003/atsr.3-sub-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-nwp-2003-01.nc

year=$1
month=$2
sensor=$3
usecase=$4

d=`date +%s -u -d "$year-$month-01 00:00:00"`
let d1="d + 32 * 86400"
starttime=$year-$month-01T00:00:00Z
stoptime=`date +%Y-%m -u -d @$d1`-01T00:00:00Z

pattern=`cat $MMS_HOME/configuration/$usecase-configuration.xml | awk "/mms.pattern.$sensor/ { print \\$3 }"`

nwp-tool.sh -c $usecase-configuration.xml \
false \
$sensor \
$pattern \
$MMS_HOME/configuration/mmd-dimensions.properties \
$MMS_ARCHIVE/$usecase/sub/$sensor/$year/$sensor-sub-$year-$month.nc \
$MMS_ARCHIVE/ecmwf-era-interim/v01 \
$MMS_ARCHIVE/$usecase/nwp/$sensor/$year/$sensor-nwp-$year-$month.nc
