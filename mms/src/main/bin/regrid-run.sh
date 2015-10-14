#!/bin/bash
set -e

# 2015-05-15 GH Adapted for use on CEMS.

# Example usage:
#   - regrid-run.sh 2007 06 AAVHRMTA_G
#   - regrid-run.sh 2005 10 AATSR
#   - regrid-run.sh 2015 05 all_sensors

# Note: this has some hardcoded parts, such as the L2/L3 version numbers
# that need to be regridded.  Adapt as needed.

# FIXME: this should not be hardcoded, but somehow coming from
# configuration variables or from cmdline input
propertiesrootdir="/home/users/gholl/projects/2015_esacci_sst_processing/sst_cci_test_results"
regrid="/group_workspaces/cems2/esacci_sst/software/sst-cci-tools/v2.0/bin/regrid"

# Code starts here (you may have to adapt paths according to the organisation of input and output data)

year=$1
month=$2
sensor=$3
archive_root=$4
target_root=$5

case "${sensor:0:5}" in 
    'AATSR' | 'ATSR1' | 'ATSR2')
        sensortype="ATSR"
        version="v01.1"
        type="L3U"
        ;;
    'AVHRR' )
        sensortype="AVHRR"
        version="v01.0"
        type="L2P"
        ;;
    'all_sensors' )
        sensortype=""
        version=""
        type="L2P"
esac

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"
enddate=`date +%Y-%m-01 -u -d @${d1}`
subdir="${sensortype}/${type}/${version}/${sensor}/${year}/${month}"

mkdir -p ${target_root}/${subdir}

# Note: If this fails with a Java error “Could not reserve enough
# space for object heap”, have a look at:
#
# http://stackoverflow.com/q/4401396/974555

$regrid -e -l INFO -c ${propertiesrootdir}/regrid_${type}.properties \
	--startDate ${year}-${month}-01 \
	--endDate ${enddate} \
    --CCI_${type}.dir ${archive_root}/${subdir} \
    --outputDir ${target_root}/${subdir}
