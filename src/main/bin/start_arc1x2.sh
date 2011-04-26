#!/bin/bash

# start_arc1x2.sh l1bgzpath latlonpath [ wd ]
# creates wd and puts there l1b.gz, l1b.latlon.txt, l1b.cmr.h5, l1b.prb.h5, and optionally l1b.nav.h5
# calls qsub run_arc1x2.sh

#$ -j y

set -e # one fails, all fail
set -a
umask 007

nas=/exports/nas/exports/cse/geos/scratch/gc/sst-cci/avhrr
avhrrDir=/exports/work/geos_gc_sst_cci/avhrr
wdbasedir=/exports/work/geos_gc_sst_cci/mms

pushd `dirname $0` > /dev/null
bindir=`pwd`
popd > /dev/null
l1bgzpath=$1
latlonpath=$2
l1b=`basename ${1%.gz}`
wd=${3:-$wdbasedir/task-$l1b}

echo $wd

# find satellite name
# example L1B filename: NSS.GHRR.NN.D09213.S2206.E2354.B2164041.GC
code=${l1b:9:2}
case $code in
    TN) satDir='tiros-n'
	datDir='tirosn'
	SAT='TIROSN';;
    NA) satDir='noaa-06'
	datDir='noaa06'
	SAT='NOAA06';;
    NB) satDir=''
	datDir=''
	SAT='';;
    NC) satDir='noaa-07'
	datDir='noaa07'
	SAT='NOAA07';;
    ND) satDir='noaa-12'
	datDir='noaa12'
	SAT='NOAA12';;
    NE) satDir='noaa-08'
	datDir='noaa08'
	SAT='NOAA08';;
    NF) satDir='noaa-09'
	datDir='noaa09'
	SAT='NOAA09';;
    NG) satDir='noaa-10'
	datDir='noaa10'
	SAT='NOAA10';;
    NH) satDir='noaa-11'
	datDir='noaa11'
	SAT='NOAA11';;
    NI) satDir='noaa-13'
	datDir='noaa13'
	SAT='NOAA13';;
    NJ) satDir='noaa-14'
	datDir='noaa14'
	SAT='NOAA14';;
    NK) satDir='noaa-15'
	datDir='noaa15'
	SAT='NOAA15';;
    NL) satDir='noaa-16'
	datDir='noaa16'
	SAT='NOAA16';;
    NM) satDir='noaa-17'
	datDir='noaa17'
	SAT='NOAA17';;
    NN) satDir='noaa-18'
	datDir='noaa18'
	SAT='NOAA18';;
    NP) satDir='noaa-19'
	datDir='noaa19'
	SAT='NOAA19';;
    M2) satDir='metop-02'
	datDir='metop02'
	SAT='METOP02';;
esac

# parse l1b filename to get year
# example l1b filename NSS.GHRR.NN.D09213.S2206.E2354.B2164041.GC
year=${l1b:13:2}
# need the 10# to make 08,09 be interpreted as decimal not octal
year=$((10#${year}<78?20:19))$year

# copy compressed l1b and latlon file
mkdir -p $wd
rm -f $wd/*trace $wd/NSS*GC $wd/*.LOC.nc $wd/*.mmm.txt $wd/*.MMM.nc

# copy compressed l1b and latlon file
echo 'copy l1b'
cp -f $l1bgzpath $wd
echo 'copy latlon'
cp -f $latlonpath $wd/$l1b.latlon.txt

# clavr-x time-corrected lon/lat (may not exist)
navDir=$nas/clavr-nav/$satDir/$year
navFilename=$l1b.nav.h5
if [[ -s $navDir/$navFilename ]]; then
    echo 'copy clavr-nav'
    cp -f $navDir/$navFilename $wd
fi

# clavr-x cloud mask (must exist)
cldDir=$nas/clavr-cld/$satDir/$year
cldFilename=$l1b.cmr.h5
echo 'copy clavr-cld'
cp -f $cldDir/$cldFilename $wd

# clavr-x cloud probability (must exist)
prbDir=$nas/clavr-prb/$satDir/$year
prbFilename=$l1b.prb.h5
echo 'copy clavr-prb'
cp -f $prbDir/$prbFilename $wd

# link aux data for ARC2 into wd
ln -sf $avhrrDir/GBCS/avhrr_${datDir}_dat/AVHRR_${SAT}.inp $wd/AVHRR_ARC2_${SAT}.inp
ln -sf $avhrrDir/GBCS/avhrr_${datDir}_dat $wd/

echo "qsub -o $wd/stdout.trace -j y -N task-$l1b $bindir/run_arc1x2.sh $wd $l1b"
qsub -o $wd/stdout.trace -j y -N task-$l1b $bindir/run_arc1x2.sh $wd $l1b

# to check the job wasn't terminated by being over the job time limit
echo 'submitted'
