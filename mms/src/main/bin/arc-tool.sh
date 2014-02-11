#!/bin/bash

. mymms
. $MMS_HOME/bin/mms-env.sh

# call pattern: arc-tool.sh <year> <month> <sensor>
# call example: arc-tool.sh 2010 12 avhrr.n18

year=$1
month=$2
sensor=$3

echo "`date -u +%Y%m%d-%H%M%S` arc3 $year/$month $sensor ..."

if [ "$year" = "" -o "$month" = "" -o "$sensor" = "" ]; then
    echo "missing parameter, use $0 year month sensor"
    exit 1
fi

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

# run ARC3

arc3files=`find $MMS_ARC3 -type f | grep -v 'dat/'`
ln -f $arc3files .
mkdir -p dat
ln -f $MMS_ARC3/dat/* dat

case $sensor in
    atsr.1) inp=MMD_ATSR1.inp ;;
    atsr.2) inp=MMD_ATSR2.inp ;;
    atsr.3) inp=MMD_AATSR.inp ;;
    avhrr.n10) inp=MMD_NOAA10.inp ;;
    avhrr.n11) inp=MMD_NOAA11.inp ;;
    avhrr.n12) inp=MMD_NOAA12.inp ;;
    avhrr.n14) inp=MMD_NOAA14.inp ;;
    avhrr.n15) inp=MMD_NOAA15.inp ;;
    avhrr.n16) inp=MMD_NOAA16.inp ;;
    avhrr.n17) inp=MMD_NOAA17.inp ;;
    avhrr.n18) inp=MMD_NOAA18.inp ;;
    avhrr.n19) inp=MMD_NOAA19.inp ;;
    avhrr.m02) inp=MMD_METOP02.inp ;;
    *) exit 1 ;;
esac

if ! ./MMD_SCREEN_Linux $inp $sensor-nwp-$startTimeCompact-$stopTimeCompact.nc $sensor-nwp-$startTimeCompact-$stopTimeCompact.nc $sensor-arc3-$startTimeCompact-$stopTimeCompact.nc; then
    echo "production gap: arc3-$year-$month-$sensor failed, MMD_SCREEN_Linux failed"
    echo "`date -u +%Y%m%d-%H%M%S` arc3 $year/$month $sensor ... failed"
    exit 1
fi

# to check the job wasn't terminated by being over the job time limit
echo "`date -u +%Y%m%d-%H%M%S` arc3 $year/$month $sensor ... done"
