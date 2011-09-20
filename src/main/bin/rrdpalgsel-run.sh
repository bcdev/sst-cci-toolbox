#!/bin/bash

. mymms
. $MMS_HOME/bin/mms-env.sh

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` mmd rrdp test $year/$month ..."

if [ "$year" = "" -or "$month" = "" ]; then
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

mkdir -p $MMS_ARCHIVE/mmd/v1/$year

$MMS_HOME/bin/mmd-tool.sh -c $MMS_CONFIG \
    -Dmms.target.startTime=$year-$month-01T00:00:00Z \
    -Dmms.target.stopTime=$stopyear-$stopmonth-01T00:00:00Z \
    -Dmms.db.useindex=false \
    -Dmms.target.dimensions=$MMS_HOME/config/mmd-dimensions-rrdp.properties \
    -Dmms.target.variables=$MMS_HOME/config/mmd-variables-rrdpalgsel.config \
    -Dmms.target.condition='r.dataset = 0 and (r.referenceflag = 0 or r.referenceflag = 1)' \
    -Dmms.target.dir=$TMPDIR \
    -Dmms.target.filename=mmd-rrdp_algsel-$year-$month.nc

mv -f $TMPDIR/mmd-rrdp_algsel-$year-$month.nc $archiveroot/mmd/v1/$year

echo "`date -u +%Y%m%d-%H%M%S` mmd rrdp test $year/$month ... done"
