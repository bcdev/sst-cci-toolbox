#!/bin/bash

# run_arc1x2.sh /path/wd l1b_filename
# expects l1b.gz, l1b.latlon.txt, l1b.cmr.h5, l1b.prb.h5, and optionally l1b.nav.h5
# files in wd

#$ -j y

source /etc/profile
export MODULEPATH=/exports/work/geos_gits/geos_applications/modulefiles/SL5:$MODULEPATH
module load intel/compiler/11.0
module load geos/sciio/1/intel
module load geos/sciio-utils/1

set -e # one fails, all fail
set -a
ulimit -s unlimited
umask 007

avhrrDir=/exports/work/geos_gc_sst_cci/avhrr
mmsDir=/exports/work/geos_gc_sst_cci/mms

wd=$1
l1b=$2

#navFilename=$l1b.nav.h5 # may not exist
#if [[ -s $scratch/$navFilename ]]; then
#    echo 'copy clavr-nav' >&2
#    time cp -f $scratch/$navFilename $workDir
#fi

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

# uncompress l1b for ARC1
cd $wd
#gunzip -fc $l1b.gz > $l1b

# create the geo-locations from the L1B file
$avhrrDir/GBCS/bin/AVHRR_LOC_Linux $wd $l1b.gz
chgrp geos_gc_sst_cci $l1b.LOC.nc
chmod g+rw $l1b.LOC.nc

# convert latlon center positions into x/y using geo-locations
$mmsDir/sst-cci-toolbox-0.1-SNAPSHOT/bin/arcpixelpos.sh -Dmms.arcprocessing.latlonfile=$l1b.latlon.txt -Dmms.arcprocessing.locationfile=$l1b.LOC.nc
chgrp geos_gc_sst_cci $l1b.mmm.txt
chmod g+rw $l1b.mmm.txt
rm arcpixelpos.log

# create subscenes of L1B file for x/y positions
$avhrrDir/GBCS/bin/AVHRR_ARC2_Linux AVHRR_ARC2_${SAT}.inp $l1b.gz
chgrp geos_gc_sst_cci $l1b.MMM.nc
chmod g+rw $l1b.MMM.nc

# to check the job wasn't terminated by being over the job time limit
echo 'completed'
