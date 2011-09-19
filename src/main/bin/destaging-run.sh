#!/bin/bash

. `dirname $0`/mms-env.sh

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` de-staging of $year/$months ..."

if [ "$year" = "" -or "$month" = "" ]; then
    echo "missing parameter, use $0 year month"
    exit 1
fi

if [ "$MMS_ARCHIVE" = "$MMS_NAS" ]; then
    echo "archive and nas are identical: $MMS_ARCHIVE"
    exit 1
fi

cd $MMS_ARCHIVE

for sv in avhrr_sub/v1 nwp/v1 arc3/v1
do
    if [ -e $sv/$year/$month ]
    then
        date
        echo "de-staging $sv/$year/$month ..."
        mkdir -p $MMS_NAS/$sv/$year
        rsync -av $sv/$year/$month $MMS_NAS/$sv/$year
    else
        echo "skipping $sv for $year/$month"
    fi
done

for sv in mmd/v1
do
    if [ -e $sv/$year/mmd-$year-$month*nc ]
    then
        date
        echo "de-staging $sv/$year/$month ..."
        mkdir -p $MMS_NAS/$sv/$year
        rsync -av $sv/$year/mmd-*$year-$month.nc $MMS_NAS/$sv/$year
    else
        echo "skipping $sv for $year/$month"
    fi
done

echo "`date -u +%Y%m%d-%H%M%S` de-staging of $year/$month ... done"
