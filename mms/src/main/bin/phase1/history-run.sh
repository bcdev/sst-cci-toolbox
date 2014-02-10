#!/bin/bash

. mymms
. $MMS_HOME/bin/mms-env.sh

echo "`date -u +%Y%m%d-%H%M%S` ingesting history ..."

$MMS_HOME/bin/ingestion-tool.sh -c $MMS_CONFIG -debug \
-Dmms.source.startTime=1990-01-01T00:00:00Z \
-Dmms.source.endTime=2012-01-01T00:00:00Z \
-Dmms.source.45.inputDirectory=insitu-history/v01

echo "`date -u +%Y%m%d-%H%M%S` ingesting history ... done"
