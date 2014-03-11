#!/bin/bash

. mymms
. $MMS_HOME/bin/mms-env.sh

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` matchup related $year/$month ..."

if [ "$year" = "" -o "$month" = "" ]; then
    echo "missing parameter, use $0 year month"
    exit 1
fi

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

$MMS_HOME/bin/matchup2-tool.sh -c $MMS_CONFIG -debug \
    -Dmms.matchup.startTime=$year-$month-01T00:00:00Z \
    -Dmms.matchup.stopTime=$stopyear-$stopmonth-01T00:00:00Z

echo "`date -u +%Y%m%d-%H%M%S` matchup related  $year/$month ... done"
