#!/bin/bash
set -x
# example usage: ingestion-run.sh 2003 01 mms2

year=$1
month=$2
usecase=$3

echo "`date -u +%Y%m%d-%H%M%S` ingestion ${year}/${month} ..."

${mms.home}/bin/ingestion-tool.sh -c ${mms.home}/config/${usecase}-config.properties \
-Dmms.source.11.inputDirectory=atsr.1/v2.1/${year}/${month} \
-Dmms.source.12.inputDirectory=atsr.2/v2.1/${year}/${month} \
-Dmms.source.13.inputDirectory=atsr.3/v2.1/${year}/${month} \
-Dmms.source.22.inputDirectory=avhrr.n10/v01/${year}/${month} \
-Dmms.source.23.inputDirectory=avhrr.n11/v01/${year}/${month} \
-Dmms.source.24.inputDirectory=avhrr.n12/v01/${year}/${month} \
-Dmms.source.26.inputDirectory=avhrr.n14/v01/${year}/${month} \
-Dmms.source.27.inputDirectory=avhrr.n15/v01/${year}/${month} \
-Dmms.source.28.inputDirectory=avhrr.n16/v01/${year}/${month} \
-Dmms.source.29.inputDirectory=avhrr.n17/v01/${year}/${month} \
-Dmms.source.30.inputDirectory=avhrr.n18/v01/${year}/${month} \
-Dmms.source.31.inputDirectory=avhrr.n19/v01/${year}/${month} \
-Dmms.source.32.inputDirectory=avhrr.m02/v01/${year}/${month} \
-Dmms.source.33.inputDirectory=avhrr.m01/v01/${year}/${month} \
-Dmms.source.34.inputDirectory=avhrr_f.m02/v01/${year}/${month} \
-Dmms.source.35.inputDirectory=avhrr_f.m01/v01/${year}/${month} \
-Dmms.source.36.inputDirectory=amsr2/v01/${year}/${month} \
-Dmms.source.43.inputDirectory=aerosol-aai/v01/${year}/${month} \
-Dmms.source.44.inputDirectory=sea-ice/v01/${year}/${month}
