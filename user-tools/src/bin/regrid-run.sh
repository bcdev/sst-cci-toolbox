#!/bin/bash
set -e

# example usage: regrid-run.sh 2010 06 AATSR L2P

year=$1
month=$2
sensor=$3
type=$4
version$5

# define these variables to your needs

sourcerootdir="/absolute/path/to/your/input/root/directory"
targetrootdir="/absolute/path/to/your/output/root/directory"
propertiesrootdir="/absolute/path/to/your/properties/directory"

d=`date +%s -u -d "${year}-${month}-01 00:00:00"`
let d1="d + 32 * 86400"
enddate=`date +%Y-%m-01 -u -d @${d1}`

mkdir -p ${targetrootdir}/${type}/${sensor}/${year}/${month}

/group_workspaces/cems2/esacci_sst/software/sst-cci-tools/v2.0/bin/regrid -e -l ALL -c ${propertiesrootdir}/regrid_${type}.properties --startDate ${year}-${month}-01 --endDate ${enddate} --CCI_L2P.dir ${sourcerootdir}/${type}/${sensor}/<version>/${year}/${month} --outputDir ${targetrootdir}/${type}/${sensor}/${year}/${month}
