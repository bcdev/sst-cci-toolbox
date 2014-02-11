#!/bin/bash

. mymms
. $MMS_HOME/bin/mms-env.sh

# call pattern: mmdarc3-run.sh <year> <month> <sensor>
# call example: mmdarc3-run.sh 2010 12 avhrr.n18

year=$1
month=$2
sensor=$3

echo "`date -u +%Y%m%d-%H%M%S` mmdarc3 $year/$month $sensor ..."

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
-Dmms.db.useindex=false \
-Dmms.target.pattern=$pattern \
-Dmms.target.variables=$MMS_HOME/config/mmd-variables_$sensor.config \
-Dmms.target.dimensions=$MMS_HOME/config/mmd-dimensions.properties \
-Dmms.target.filename=$sensor-sub-$startTimeCompact-$stopTimeCompact.nc

# to check the job wasn't terminated by being over the job time limit
echo "`date -u +%Y%m%d-%H%M%S` mmdarc3 $year/$month $sensor ... done"
