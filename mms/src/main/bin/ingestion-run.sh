#!/bin/bash
# ingestion-run.sh 2003 01 mms2
year=$1
month=$2
usecase=$3

. mymms

ingestion-tool.sh -c ${MMS_HOME}/config/${usecase}-config.properties \
-Dmms.source.11.inputDirectory=atsr.1/v2.1/${year}/${month} \
-Dmms.source.12.inputDirectory=atsr.2/v2.1/${year}/${month} \
-Dmms.source.13.inputDirectory=atsr.3/v2.1/${year}/${month} \
-Dmms.source.43.inputDirectory=aerosol-aai/v01/${year}/${month} \
-Dmms.source.44.inputDirectory=sea-ice/v01/${year}/${month}
