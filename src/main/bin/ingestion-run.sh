#!/bin/bash

. mymms
. $MMS_HOME/bin/mms-env.sh

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` ingesting $year/$month ..."

if [ "$year" = "" -or "$month" = "" ]; then
    echo "missing parameter, use $0 year month"
    exit 1
fi

$MMS_HOME/bin/ingestion-tool.sh -c $MMS_CONFIG -debug \
-Dmms.source.11.inputDirectory=atsr.1/v1/$year/$month \
-Dmms.source.12.inputDirectory=atsr.2/v1/$year/$month \
-Dmms.source.13.inputDirectory=atsr.3/v1/$year/$month \
-Dmms.source.21.inputDirectory=avhrr.m02/v1/$year/$month \
-Dmms.source.22.inputDirectory=avhrr.n10/v1/$year/$month \
-Dmms.source.23.inputDirectory=avhrr.n11/v1/$year/$month \
-Dmms.source.24.inputDirectory=avhrr.n12/v1/$year/$month \
-Dmms.source.25.inputDirectory=avhrr.n14/v1/$year/$month \
-Dmms.source.26.inputDirectory=avhrr.n15/v1/$year/$month \
-Dmms.source.27.inputDirectory=avhrr.n16/v1/$year/$month \
-Dmms.source.28.inputDirectory=avhrr.n17/v1/$year/$month \
-Dmms.source.29.inputDirectory=avhrr.n18/v1/$year/$month \
-Dmms.source.30.inputDirectory=avhrr.n19/v1/$year/$month \
-Dmms.source.41.inputDirectory=amsr-e/v05/$year/$month \
-Dmms.source.42.inputDirectory=tmi/v04/$year/$month \
-Dmms.source.43.inputDirectory=aerosol-aai/v01/$year/$month \
-Dmms.source.44.inputDirectory=sea-ice/v01/$year/$month

echo "`date -u +%Y%m%d-%H%M%S` ingesting $year/$month ... done"
