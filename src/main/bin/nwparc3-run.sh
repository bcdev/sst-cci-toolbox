#!/bin/bash

. mymms
. $MMS_HOME/bin/mms-env.sh

# call pattern: nwparc3-run.sh <year> <month> <part-a-b-c-d-e-f> <sensor>
# call example: nwparc3-run.sh 2010 12 a avhrr.n18

year=$1
month=$2
part=$3
sensor=$4

echo "`date -u +%Y%m%d-%H%M%S` nwp+arc3 $year/$month-$part $sensor ..."

if [ "$year" = "" -or "$month" = "" -or "$part" = "" -or "$sensor" = "" ]; then
    echo "missing parameter, use $0 year month part sensor"
    exit 1
fi

export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:$MMS_CDO/lib
export PATH=${PATH}:$MMS_CDO/bin

wd=$MMS_TEMP/nwparc3-$year-$month-$part-$sensor
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

if [ "$part" = "t" ]; then
    startTime=$year-$month-01T00:00:00Z
    stopTime=$year-$month-02T00:00:00Z
elif [ "$part" = "a" ]; then
    startTime=$year-$month-01T00:00:00Z
    stopTime=$year-$month-05T00:00:00Z
elif [ "$part" = "b" ]; then
    startTime=$year-$month-05T00:00:00Z
    stopTime=$year-$month-10T00:00:00Z
elif [ "$part" = "c" ]; then
    startTime=$year-$month-10T00:00:00Z
    stopTime=$year-$month-15T00:00:00Z
elif [ "$part" = "d" ]; then
    startTime=$year-$month-15T00:00:00Z
    stopTime=$year-$month-20T00:00:00Z
elif [ "$part" = "e" ]; then
    startTime=$year-$month-20T00:00:00Z
    stopTime=$year-$month-25T00:00:00Z
elif [ "$part" = "f" ]; then
    startTime=$year-$month-25T00:00:00Z
    stopTime=$stopyear-$stopmonth-01T00:00:00Z
else 
    startTime=$year-$month-01T00:00:00Z
    stopTime=$stopyear-$stopmonth-01T00:00:00Z
fi

startTimeForDate=`echo ${startTime%Z} | tr 'T' ' '`
stopTimeForDate=`echo ${stopTime%Z} | tr 'T' ' '`
startTimeCompact=`date -u +%Y%m%d%H%M%S -d "$startTimeForDate"`
stopTimeCompact=`date -u +%Y%m%d%H%M%S -d "$stopTimeForDate"`

# generate temporary sensor MMD

pattern=`cat $MMS_CONFIG | awk "/mms.pattern.$sensor/ { print \\$3 }"`

echo "$MMS_HOME/bin/mmd-tool.sh -c $MMS_CONFIG -debug \
-Dmms.target.startTime=$startTime \
-Dmms.target.stopTime=$stopTime \
-Djava.io.tmpdir=$wd \
-Dmms.db.useindex=true \
-Dmms.target.pattern=$pattern \
-Dmms.target.variables=$MMS_HOME/config/mmd-variables_$sensor.config \
-Dmms.target.dimensions=$MMS_HOME/config/mmd-dimensions.properties \
-Dmms.target.filename=$sensor-sub-$startTimeCompact-$stopTimeCompact.nc"

$MMS_HOME/bin/mmd-tool.sh -c $MMS_CONFIG -debug \
-Dmms.target.startTime=$startTime \
-Dmms.target.stopTime=$stopTime \
-Djava.io.tmpdir=$wd \
-Dmms.db.useindex=true \
-Dmms.target.pattern=$pattern \
-Dmms.target.variables=$MMS_HOME/config/mmd-variables_$sensor.config \
-Dmms.target.dimensions=$MMS_HOME/config/mmd-dimensions.properties \
-Dmms.target.filename=$sensor-sub-$startTimeCompact-$stopTimeCompact.nc

# generate NWP files

echo "$MMS_HOME/bin/nwp-tool.sh false $sensor $pattern $sensor-sub-$startTimeCompact-$stopTimeCompact.nc \
$MMS_ARCHIVE/ecmwf-era-interim/v01 \
$sensor-nwp-$startTimeCompact-$stopTimeCompact.nc"
if ! $MMS_HOME/bin/nwp-tool.sh false $sensor $pattern $sensor-sub-$startTimeCompact-$stopTimeCompact.nc \
$MMS_ARCHIVE/ecmwf-era-interim/v01 \
$sensor-nwp-$startTimeCompact-$stopTimeCompact.nc ; then
    echo "production gap: nwparc3-$year-$month-$part-$sensor failed, nwp generation failed"
    echo "`date -u +%Y%m%d-%H%M%S` nwp+arc3 $year/$month-$part $sensor ... failed"
    exit 1
fi

# run ARC3

arc3files=`find $MMS_ARC3 -type f | grep -v 'dat/'`
ln -f $arc3files .
mkdir -p dat
ln -f $MMS_ARC3/dat/* dat

case $sensor in
    atsr.1 | atsr.2 | atsr.3) inp=MMD_AATSR.inp ;;
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
    echo "production gap: nwparc3-$year-$month-$part-$sensor failed, MMD_SCREEN_Linux failed"
    echo "`date -u +%Y%m%d-%H%M%S` nwp+arc3 $year/$month-$part $sensor ... failed"
    exit 1
fi

# to check the job wasn't terminated by being over the job time limit
echo "`date -u +%Y%m%d-%H%M%S` nwp+arc3 $year/$month-$part $sensor ... done"
