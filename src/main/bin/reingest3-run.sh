#!/bin/bash

. mymms
. $MMS_HOME/bin/mms-env.sh

# call pattern: reingest3-run.sh <year> <month> <part-a-b-c-d>
# call example: reingest3-run.sh 2010 12 a

year=$1
month=$2
part=$3

echo "`date -u +%Y%m%d-%H%M%S` re-ingesting nwparc3 $year/$month-$part ..."

if [ "$year" = "" -or "$month" = "" -or "$part" = "" ]; then
    echo "missing parameter, use $0 year month part"
    exit 1
fi

for sensor in atsr.3 atsr.2 atsr.1 avhrr.n10 avhrr.n11 avhrr.n12 avhrr.n14 avhrr.n15 avhrr.n16 avhrr.n17 avhrr.n18 avhrr.n19 avhrr.m02
do
    if [ ! -e $MMS_TEMP/nwparc3-$year-$month-$part-$sensor ]; then
        continue;
    fi

    wd=$MMS_TEMP/nwparc3-$year-$month-$part-$sensor
    cd $wd

    f=`ls $sensor-arc3-*.nc`

    if [ ! -e "$f" ]; then
       echo "skipping nwp+arc3 $year-$month $part $sensor, arc3 file not found"
       continue
    fi

# archive result
        nwp=`echo $f | sed -e "s/arc3/nwp/"`
        arc3gz=$f.gz
        mkdir -p $MMS_ARCHIVE/nwp/v1/$year/$month
        mkdir -p $MMS_ARCHIVE/arc3/v1/$year/$month
        cp -f $nwp $MMS_ARCHIVE/nwp/v1/$year/$month
        gzip -cf $f > $MMS_ARCHIVE/arc3/v1/$year/$month/$arc3gz

        echo "$MMS_HOME/bin/reingestion-tool.sh -c $MMS_CONFIG \
            -Dmms.reingestion.filename=$MMS_ARCHIVE/nwp/v1/$year/$month/$nwp \
            -Dmms.reingestion.located=no \
            -Dmms.reingestion.overwrite=true \
            -Dmms.db.useindex=true \
            -Dmms.reingestion.sensor=nwp_$sensor \
            -Dmms.reingestion.pattern=$pattern"
        $MMS_HOME/bin/reingestion-tool.sh -c $MMS_CONFIG \
            -Dmms.reingestion.filename=$MMS_ARCHIVE/nwp/v1/$year/$month/$nwp \
            -Dmms.reingestion.located=no \
            -Dmms.reingestion.overwrite=true \
            -Dmms.db.useindex=true \
            -Dmms.reingestion.sensor=nwp_$sensor \
            -Dmms.reingestion.pattern=0

        echo "$MMS_HOME/bin/reingestion-tool.sh -c $MMS_CONFIG \
            -Dmms.reingestion.filename=$MMS_ARCHIVE/arc3/v1/$year/$month/$arc3gz \
            -Dmms.reingestion.located=no \
            -Dmms.reingestion.overwrite=true \
            -Dmms.db.useindex=true \
            -Dmms.reingestion.sensor=arc3_$sensor \
            -Dmms.reingestion.pattern=0"
        $MMS_HOME/bin/reingestion-tool.sh -c $MMS_CONFIG \
            -Dmms.reingestion.filename=$MMS_ARCHIVE/arc3/v1/$year/$month/$arc3gz \
            -Dmms.reingestion.located=no \
            -Dmms.reingestion.overwrite=true \
            -Dmms.db.useindex=true \
            -Dmms.reingestion.sensor=arc3_$sensor \
            -Dmms.reingestion.pattern=0

done

# ingest matchup nwp observations

if [ ! -e $MMS_TEMP/nwparc3-$year-$month-$part ]; then

    echo "skipping matchup nwp $year-$month $part, nwp matchup directory not found"

else

    wd=$MMS_TEMP/nwparc3-$year-$month-$part
    cd $wd
    nwpAn=`ls nwpAn-*.nc`
    nwpFc=`ls nwpFc-*.nc`
    if [ ! -e "$nwpAn" -o ! -e "$nwpFc" ]; then
        echo "skipping matchup nwp $year-$month $part, nwp matchup files not found"
    else

        mkdir -p $MMS_ARCHIVE/nwp/v1/$year/$month
        cp -f $nwpAn $MMS_ARCHIVE/nwp/v1/$year/$month
        cp -f $nwpFc $MMS_ARCHIVE/nwp/v1/$year/$month

        echo "$MMS_HOME/bin/reingestion-tool.sh -c $MMS_CONFIG \
            -Dmms.reingestion.filename=$MMS_ARCHIVE/nwp/v1/$year/$month/$nwpAn \
            -Dmms.reingestion.located=no \
            -Dmms.reingestion.overwrite=true \
            -Dmms.db.useindex=true \
            -Dmms.reingestion.sensor=nwp_an \
            -Dmms.reingestion.pattern=0"
        $MMS_HOME/bin/reingestion-tool.sh -c $MMS_CONFIG \
            -Dmms.reingestion.filename=$MMS_ARCHIVE/nwp/v1/$year/$month/$nwpAn \
            -Dmms.reingestion.located=no \
            -Dmms.reingestion.overwrite=true \
            -Dmms.db.useindex=true \
            -Dmms.reingestion.sensor=nwp_an \
            -Dmms.reingestion.pattern=0

        echo "$MMS_HOME/bin/reingestion-tool.sh -c $MMS_CONFIG \
            -Dmms.reingestion.filename=$MMS_ARCHIVE/nwp/v1/$year/$month/$nwpFc \
            -Dmms.reingestion.located=no \
            -Dmms.reingestion.overwrite=true \
            -Dmms.db.useindex=true \
            -Dmms.reingestion.sensor=nwp_fc \
            -Dmms.reingestion.pattern=0"
        $MMS_HOME/bin/reingestion-tool.sh -c $MMS_CONFIG \
            -Dmms.reingestion.filename=$MMS_ARCHIVE/nwp/v1/$year/$month/$nwpFc \
            -Dmms.reingestion.located=no \
            -Dmms.reingestion.overwrite=true \
            -Dmms.db.useindex=true \
            -Dmms.reingestion.sensor=nwp_fc \
            -Dmms.reingestion.pattern=0
    fi
fi

# to check the job wasn't terminated by being over the job time limit

echo "`date -u +%Y%m%d-%H%M%S` re-ingesting nwparc3 $year/$month-$part ... done"
