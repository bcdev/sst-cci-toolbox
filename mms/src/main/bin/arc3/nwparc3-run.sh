#!/bin/bash

. mymms
. $MMS_HOME/bin/mms-env.sh

# call pattern: nwparc3-run.sh <year> <month> <sensor>
# call example: nwparc3-run.sh 2010 12 avhrr.n18

year=$1
month=$2
sensor=$3

echo "`date -u +%Y%m%d-%H%M%S` nwparc3 $year/$month $sensor ..."

if [ "$year" = "" -o "$month" = "" -o "$sensor" = "" ]; then
    echo "missing parameter, use $0 year month sensor"
    exit 1
fi

export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:$MMS_CDO/lib
export PATH=${PATH}:$MMS_CDO/bin

wd=$MMS_TEMP/arc3-$year-$month-$sensor
mkdir -p $wd
cd $wd

stopyear=$year
if [ $month = 01 ]; then
    stopmonth=02
elif [ $month = 02 ]; then
    stopmonth=03
elif [ $month = 03 ]; then
    stopmonth=04
elif [ $month = 04 ]; then
    stopmonth=05
elif [ $month = 05 ]; then
    stopmonth=06
elif [ $month = 06 ]; then
    stopmonth=07
elif [ $month = 07 ]; then
    stopmonth=08
elif [ $month = 08 ]; then
    stopmonth=09
elif [ $month = 09 ]; then
    stopmonth=10
elif [ $month = 10 ]; then
    stopmonth=11
elif [ $month = 11 ]; then
    stopmonth=12
elif [ $month = 12 ]; then
    let stopyear="$year + 1"
    stopmonth=01
fi

startTime=$year-$month-01T00:00:00Z
stopTime=$stopyear-$stopmonth-01T00:00:00Z

startTimeForDate=`echo ${startTime%Z} | tr 'T' ' '`
stopTimeForDate=`echo ${stopTime%Z} | tr 'T' ' '`
startTimeCompact=`date -u +%Y%m%d%H%M%S -d "$startTimeForDate"`
stopTimeCompact=`date -u +%Y%m%d%H%M%S -d "$stopTimeForDate"`

pattern=`cat $MMS_CONFIG | awk "/mms.pattern.$sensor/ { print \\$3 }"`

# generate NWP files for previously generated sensor MMD

echo "$MMS_HOME/bin/nwp-tool.sh false $sensor $pattern \
        $MMS_HOME/configuration/mmd-dimensions.properties \
        $sensor-sub-$startTimeCompact-$stopTimeCompact.nc \
        $MMS_ARCHIVE/ecmwf-era-interim/v01 \
        $sensor-nwp-$startTimeCompact-$stopTimeCompact.nc"
if ! $MMS_HOME/bin/nwp-tool.sh false $sensor $pattern \
        $MMS_HOME/configuration/mmd-dimensions.properties \
        $sensor-sub-$startTimeCompact-$stopTimeCompact.nc \
        $MMS_ARCHIVE/ecmwf-era-interim/v01 \
        $sensor-nwp-$startTimeCompact-$stopTimeCompact.nc
then
    echo "production gap: nwparc3-$year-$month-$sensor failed, nwp generation failed"
    echo "`date -u +%Y%m%d-%H%M%S` nwparc3 $year/$month $sensor ... failed"
    exit 1
fi

# to check the job wasn't terminated by being over the job time limit
echo "`date -u +%Y%m%d-%H%M%S` nwparc3 $year/$month $sensor ... done"
