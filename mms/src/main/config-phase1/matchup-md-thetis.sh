#!/bin/bash
set -e

echo "`date -u +%Y%m%d-%H%M%S` matchup MDs $year/$month"

year=2005
month=12
endofloopyear=1990

while [ true ]
do
    startyear=$year
    startday=31
    stopyear=$year
    if [ $month = 01 ]; then
        let startyear="$year - 1"
        startmonth=12
        startday=31
        stopyear=$year
        stopmonth=02
    elif [ $month = 02 ]; then
        startyear=$year
        startmonth=01
        startday=31
        stopyear=$year
        stopmonth=03
    elif [ $month = 03 ]; then
        startyear=$year
        startmonth=02
        startday=28
        stopyear=$year
        stopmonth=04
    elif [ $month = 04 ]; then
        startyear=$year
        startmonth=03
        startday=31
        stopyear=$year
        stopmonth=05
    elif [ $month = 05 ]; then
        startyear=$year
        startmonth=04
        startday=30
        stopyear=$year
        stopmonth=06
    elif [ $month = 06 ]; then
        startyear=$year
        startmonth=05
        startday=31
        stopyear=$year
        stopmonth=07
    elif [ $month = 07 ]; then
        startyear=$year
        startmonth=06
        startday=30
        stopyear=$year
        stopmonth=08
    elif [ $month = 08 ]; then
        startyear=$year
        startmonth=07
        startday=31
        stopyear=$year
        stopmonth=09
    elif [ $month = 09 ]; then
        startyear=$year
        startmonth=08
        startday=31
        stopyear=$year
        stopmonth=10
    elif [ $month = 10 ]; then
        startyear=$year
        startmonth=09
        startday=30
        stopyear=$year
        stopmonth=11
    elif [ $month = 11 ]; then
        startyear=$year
        startmonth=10
        startday=31
        stopyear=$year
        stopmonth=12
    elif [ $month = 12 ]; then
        startyear=$year
        startmonth=11
        startday=30
        let stopyear="$year + 1"
        stopmonth=01
    fi
    echo "`date -u +%Y%m%d-%H%M%S` matchup MDs $year/$month ..."
    $MMS_HOME/bin/matchup-tool.sh -c mms-thetis.properties -debug \
        -Dmms.matchup.startTime=$startyear-$startmonth-${startday}T00:00:00Z \
        -Dmms.matchup.stopTime=$stopyear-$stopmonth-01T00:00:00Z
    year=$startyear
    month=$startmonth
    if [ $year -le $endofloopyear ]; then
        break
    fi
done

echo "`date -u +%Y%m%d-%H%M%S` matchup MDs finished"
