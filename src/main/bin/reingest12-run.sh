#!/bin/bash

. mymms
. $MMS_HOME/bin/mms-env.sh

# call pattern: reingest12-run.sh <year> <month> <part-a-b-c-d>
# call example: reingest12-run.sh 2010 12 a

year=$1
month=$2
part=$3
sensors=${4:-n10 n11 n12 n14 n15 n16 n17 n18 n19 m02}

echo "`date -u +%Y%m%d-%H%M%S` re-ingesting arc12 $year/$month-$part ..."

if [ "$year" = "" -o "$month" = "" -o "$part" = "" ]; then
    echo "missing parameter, use $0 year month part"
    exit 1
fi

for sensor in $sensors
do
    if [ ! -e $MMS_TEMP/arc12-$year-$month-$part-avhrr_orb.$sensor ]; then
        continue;
    fi

    wd=$MMS_TEMP/arc12-$year-$month-$part-avhrr_orb.$sensor
    cd $wd

    pattern=`cat $MMS_CONFIG | awk "/mms.pattern.avhrr.$sensor/ { print \\$3 }"`

    for f in *.MMM.nc
    do
        if [ "$f" = '*.MMM.nc' ]; then
            echo "production gap: no subscenes found for $year $month $part $sensor"
            continue
        fi
# archive result
        output=${f%.nc}.$part.nc.gz
        mkdir -p $MMS_ARCHIVE/avhrr_sub/v1/$year/$month
        gzip -cf $f > $MMS_ARCHIVE/avhrr_sub/v1/$year/$month/$output

        echo "$MMS_HOME/bin/reingestion-tool.sh -c $MMS_CONFIG \
            -Dmms.reingestion.filename=$MMS_ARCHIVE/avhrr_sub/v1/$year/$month/$output \
            -Dmms.reingestion.located=no \
            -Dmms.reingestion.overwrite=true \
            -Dmms.db.useindex=true \
            -Dmms.reingestion.sensor=avhrr.$sensor \
            -Dmms.reingestion.pattern=$pattern"
        $MMS_HOME/bin/reingestion-tool.sh -c $MMS_CONFIG \
            -Dmms.reingestion.filename=$MMS_ARCHIVE/avhrr_sub/v1/$year/$month/$output \
            -Dmms.reingestion.located=no \
            -Dmms.reingestion.overwrite=true \
            -Dmms.db.useindex=true \
            -Dmms.reingestion.sensor=avhrr.$sensor \
            -Dmms.reingestion.pattern=$pattern
    done

done

# to check the job wasn't terminated by being over the job time limit

echo "`date -u +%Y%m%d-%H%M%S` re-ingesting arc12 $year/$month-$part ... done"
