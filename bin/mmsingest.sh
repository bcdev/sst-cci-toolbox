#!/bin/bash

#! /bin/sh

export BEAM4_HOME=${installer:sys.installationDir}

if [ ! -d "$BEAM4_HOME" ]
then
    PRGDIR=`dirname $0`
    export BEAM4_HOME=`cd "$PRGDIR/.." ; pwd`
fi

if [ -z "$BEAM4_HOME" ]; then
    echo
    echo Error:
    echo BEAM4_HOME does not exists in your environment. Please
    echo set the BEAM4_HOME variable in your environment to the
    echo location of your BEAM 4.x installation.
    echo
    exit 2
fi

. "$BEAM4_HOME/bin/detect_java.sh"

"$app_java_home/bin/java" \
    -Xmx1024M \
    -Dceres.context=beam \
    -Dbeam.splash.image \
    "-Dbeam.app=RgbQuickLookGeneratorMain" \
    "-Dbeam.home=$BEAM4_HOME" \
    -jar "$BEAM4_HOME/bin/ceres-launcher.jar" "$@"

exit 0


java org.esa.cci.sst.IngestionClient $*
