#!/bin/bash

. `dirname $0`/mms-env.sh

# call pattern: arc12-run.sh <year> <month> <part-a-b-c-d> <sensor>
# call example: arc12-run.sh 2010 12 a avhrr_orb.n18

year=$1
month=$2
part=$3
sensor=$4

echo "`date -u +%Y%m%d-%H%M%S` arc1+arc2 $year/$month-$part $sensor ..."

if [ "$year" = "" -or "$month" = "" -or "$part" = "" -or "$sensor" = "" ]; then
    echo "missing parameter, use $0 year month part sensor"
    exit 1
fi

# expects l1b.gz in $MMS_ARCHIVE/avhrr.*/v*/$year/$month/$day/
# expects clavrx cld, prb and optionally nav files in $MMS_ARCHIVE/clavrx/
# generates latlon files in $MMS_TEMP/arc12-$year-$month/
# generates temporary files in $MMS_TEMP/arc12-$year-$month/

source /etc/profile
export MODULEPATH=/exports/work/geos_gits/geos_applications/modulefiles/SL5:$MODULEPATH
module load intel/compiler/11.0
module load geos/sciio/1/intel
module load geos/sciio-utils/1

export LD_LIBRARY_PATH=/exports/work/geos_gits/geos_applications/SL5/sciio-utils/1/lib:/exports/work/geos_gits/geos_applications/SL5/sciio/1/intel/11.0/lib:/exports/applications/apps/SL5/intel/Compiler/11.0/081/lib/intel64:$LD_LIBRARY_PATH

wd=$MMS_TEMP/arc12-$year-$month-$part-$sensor
mkdir -p $wd
cd $wd
rm -f $wd/latlon.txt $wd/*trace $wd/NSS*GC $wd/*.LOC.nc $wd/*.mmm.txt $wd/*.MMM.nc

# export latlon files from database

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

if [ "$part" = "a" ]; then
    startTime=$year-$month-01T00:00:00Z
    stopTime=$year-$month-08T00:00:00Z
elif [ "$part" = "b" ]; then
    startTime=$year-$month-08T00:00:00Z
    stopTime=$year-$month-16T00:00:00Z
elif [ "$part" = "c" ]; then
    startTime=$year-$month-16T00:00:00Z
    stopTime=$year-$month-24T00:00:00Z
elif [ "$part" = "d" ]; then
    startTime=$year-$month-24T00:00:00Z
    stopTime=$stopyear-$stopmonth-01T00:00:00Z
else 
    startTime=$year-$month-01T00:00:00Z
    stopTime=$stopyear-$stopmonth-01T00:00:00Z
fi

$MMS_HOME/bin/arc12-tool.sh -c $MMS_CONFIG -debug \
-Dmms.arc1x2.startTime=$startTime \
-Dmms.arc1x2.endTime=$stopTime \
-Dmms.arc1x2.tmpdir=$wd \
-Dmms.db.useindex=true \
-Dmms.arc1x2.sensor=$sensor

# process latlon files

result=1
for latlon in *latlon.txt
do
    if [ "$latlon" = '*latlon.txt' ]; then
        continue
    fi
    echo "`date -u +%Y%m%d-%H%M%S` processing $latlon ..."

    l1b=${latlon%.latlon.txt}

# find satellite name
# example L1B filename: NSS.GHRR.NN.D09213.S2206.E2354.B2164041.GC
    code=${l1b:9:2}
    case $code in
        TN) satDir='tiros-n'
	    datDir='tirosn'
	    arcDir='tirosn'
	    SAT='TIROSN';;
        NA) satDir='noaa-06'
	    datDir='noaa06'
	    arcDir='avhrr.n06'
	    SAT='NOAA06';;
        NB) satDir=''
	    datDir=''
	    arcDir=''
	    SAT='';;
        NC) satDir='noaa-07'
	    datDir='noaa07'
	    arcDir='avhrr.n07'
	    SAT='NOAA07';;
        ND) satDir='noaa-12'
	    datDir='noaa12'
	    arcDir='avhrr.n12'
	    SAT='NOAA12';;
        NE) satDir='noaa-08'
	    datDir='noaa08'
	    arcDir='avhrr.n08'
	    SAT='NOAA08';;
        NF) satDir='noaa-09'
	    datDir='noaa09'
	    arcDir='avhrr.n09'
	    SAT='NOAA09';;
        NG) satDir='noaa-10'
	    datDir='noaa10'
	    arcDir='avhrr.n10'
	    SAT='NOAA10';;
        NH) satDir='noaa-11'
	    datDir='noaa11'
	    arcDir='avhrr.n11'
	    SAT='NOAA11';;
        NI) satDir='noaa-13'
	    datDir='noaa13'
	    arcDir='avhrr.n13'
	    SAT='NOAA13';;
        NJ) satDir='noaa-14'
	    datDir='noaa14'
	    arcDir='avhrr.n14'
	    SAT='NOAA14';;
        NK) satDir='noaa-15'
	    datDir='noaa15'
	    arcDir='avhrr.n15'
	    SAT='NOAA15';;
        NL) satDir='noaa-16'
	    datDir='noaa16'
	    arcDir='avhrr.n16'
	    SAT='NOAA16';;
        NM) satDir='noaa-17'
	    datDir='noaa17'
	    arcDir='avhrr.n17'
	    SAT='NOAA17';;
        NN) satDir='noaa-18'
	    datDir='noaa18'
	    arcDir='avhrr.n18'
	    SAT='NOAA18';;
        NP) satDir='noaa-19'
	    datDir='noaa19'
	    arcDir='avhrr.n19'
	    SAT='NOAA19';;
        M2) satDir='metop-02'
	    datDir='metop02'
	    arcDir='avhrr.m02'
	    SAT='METOP02';;
    esac

# parse l1b filename to get year
# example l1b filename NSS.GHRR.NN.D09213.S2206.E2354.B2164041.GC
    year=${l1b:13:2}
# need the 10# to make 08,09 be interpreted as decimal not octal
    year=$((10#${year}<78?20:19))$year

# find l1b file in archive
    l1bpath=`find $MMS_ARCHIVE/$arcDir/v1/$year -name $l1b.gz`
    ln -sf $l1bpath $wd

# find and copy clavr-x time-corrected lon/lat (may not exist)
    navDir=$MMS_ARCHIVE/clavrx/nav/$satDir/$year
    navFilename=$l1b.nav.h5
    if [[ -s $navDir/$navFilename ]]; then
        echo 'copy clavr-nav'
        ln -sf $navDir/$navFilename $wd
    fi

# find and copy clavr-x cloud mask (must exist)
    cldDir=$MMS_ARCHIVE/clavrx/cld/$satDir/$year
    cldFilename=$l1b.cmr.h5
    if [[ ! -s $cldDir/$cldFilename ]]; then
        echo "production gap: $l1b for arc12-$year-$month-$part-$sensor failed, missing clavr-cld clavrx/cld/$satDir/$year/$cldFilename"
        continue
    fi
    echo 'copy clavr-cld'
    ln -sf $cldDir/$cldFilename $wd

# find and copy clavr-x cloud probability (must exist)
    prbDir=$MMS_ARCHIVE/clavrx/prb/$satDir/$year
    prbFilename=$l1b.prb.h5
    if [[ ! -s $prbDir/$prbFilename ]]; then
        echo "production gap: $l1b for arc12-$year-$month-$part-$sensor failed, missing clavr-cprb clavrx/prb/$satDir/$year/$prbFilename"
        continue
    fi
    echo 'copy clavr-prb'
    ln -sf $prbDir/$prbFilename $wd

# link aux data for ARC2 into wd
    ln -sf $MMS_GBCS/avhrr_${datDir}_dat/AVHRR_${SAT}.inp $wd/AVHRR_ARC2_${SAT}.inp
    ln -sf $MMS_GBCS/avhrr_${datDir}_dat $wd/

# create geo-locations from the L1B file
    if ! $MMS_GBCS/bin/AVHRR_LOC_Linux $wd $l1b.gz ; then
        echo "production gap: $l1b for arc12-$year-$month-$part-$sensor failed in AVHRR_LOC_Linux"
        continue
    fi
    chgrp geos_gc_sst_cci $l1b.LOC.nc
    chmod g+rw $l1b.LOC.nc

# convert latlon center positions into x/y using geo-locations
    if ! $MMS_HOME/bin/pixelpos-tool.sh -Dmms.pixelpos.latlonfile=$l1b.latlon.txt -Dmms.pixelpos.locationfile=$l1b.LOC.nc ; then
        echo "production gap: $l1b for arc12-$year-$month-$part-$sensor failed in pixelpos determination"
        continue
    fi
    chgrp geos_gc_sst_cci $l1b.mmm.txt
    chmod g+rw $l1b.mmm.txt
    rm -f arcpixelpos.log

# create subscenes of L1B file for x/y positions
    if ! $MMS_GBCS/bin/AVHRR_ARC2_Linux AVHRR_ARC2_${SAT}.inp $l1b.gz ; then
        echo "production gap: $l1b for arc12-$year-$month-$part-$sensor failed in AVHRR_ARC2_Linux"
        continue
    fi
    if ! chgrp geos_gc_sst_cci $l1b.MMM.nc ; then
        echo "production gap: $l1b for arc12-$year-$month-$part-$sensor failed, missing output $l1b.MMM.nc"
        continue
    fi
    chmod g+rw $l1b.MMM.nc

    echo "`date -u +%Y%m%d-%H%M%S` output $l1b.MMM.nc"
    result=0
done

# to check the job wasn't terminated by being over the job time limit
if [ $result != 0 ]; then
    echo "`date -u +%Y%m%d-%H%M%S` arc1+arc2 $year/$month-$part $sensor ... failed"
else
    echo "`date -u +%Y%m%d-%H%M%S` arc1+arc2 $year/$month-$part $sensor ... done"
fi
exit $result
