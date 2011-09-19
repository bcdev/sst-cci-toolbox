#!/bin/bash

. `dirname $0`/mms-env.sh

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` staging of $year/$months ..."

if [ "$year" = "" -or "$month" = "" ]; then
    echo "missing parameter, use $0 year month"
    exit 1
fi

if [ "$MMS_ARCHIVE" = "$MMS_NAS" ]; then
    echo "archive and nas are identical: $MMS_ARCHIVE"
    exit 1
fi

cd $MMS_NAS

for sv in atsr.1/v1 atsr.2/v1 atsr.3/v1 avhrr.m02/v1 avhrr.n10/v1 avhrr.n11/v1 avhrr.n12/v1 avhrr.n14/v1 avhrr.n15/v1 avhrr.n16/v1 avhrr.n17/v1 avhrr.n18/v1 avhrr.n19/v1 aerosol-aai/v01 sea-ice/v01 amsr-e/v05 tmi/v04 atsr_md/v01 metop_md/v01 seviri_md/v01 avhrr_md/v01
do
    if [ -e $sv/$year/$month ]
    then
        date
        echo "staging $sv/$year/$month ..."
        mkdir -p $MMS_ARCHIVE/$sv/$year
        rsync -av $sv/$year/$month $MMS_ARCHIVE/$sv/$year
    else
        echo "skipping $sv for $year/$month"
    fi
done

if [ -e ecmwf-era-interim/v01/$year/$month ]
then
    echo "`date -u +%Y%m%d-%H%M%S` staging ecmwf-era-interim/v01/$year/$month"
    mkdir -p $MMS_ARCHIVE/ecmwf-era-interim/v01/$year/$month
    rsync -av ecmwf-era-interim/v01/$year/$month/ $MMS_ARCHIVE/ecmwf-era-interim/v01/$year/$month
else
    echo "`date -u +%Y%m%d-%H%M%S` skipping non-existing ecmwf-era-interim/v01/$year/$month"
fi

if [ "$month" = "12" ]
then
    for d in clavrx/{cld,prb,nav}/*/$year
    do
        if [ -e $d ]
        then
            echo "`date -u +%Y%m%d-%H%M%S` staging $d"
            mkdir -p $MMS_ARCHIVE/$d
            rsync -av $d/ $MMS_ARCHIVE/$d
        else
            echo "`date -u +%Y%m%d-%H%M%S` skipping non-existing $d"
        fi
    done
fi

echo "`date -u +%Y%m%d-%H%M%S` staging of $year/$month ... done"
