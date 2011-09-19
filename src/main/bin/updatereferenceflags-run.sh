#!/bin/bash

. `dirname $0`/mms-env.sh

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` referenceflag MMD' re-ingestion $year/$month ..."

if [ "$year" = "" -or "$month" = "" ]; then
    echo "missing parameter, use $0 year month"
    exit 1
fi

input=`ls $MMS_ARCHIVE/referenceflags/v1/$year/referenceflags-mmd-$year-$month-v????????.nc | tail -n1`

$MMS_HOME/bin/flags-tool.sh -c $MMS_CONFIG -Dmms.reingestion.filename=$input -debug

echo "`date -u +%Y%m%d-%H%M%S` referenceflag MMD' re-ingestion $year/$month ... done"
