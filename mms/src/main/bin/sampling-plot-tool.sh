#! /bin/sh

. $MMS_INST/mymms

MMS_OPTIONS=""
if [ ! -z ${MMS_DEBUG} ]; then
    MMS_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
fi

java \
    -Dmms.home="${mms.home}" \
    -Xmx1G ${MMS_OPTIONS} \
    -Djava.io.tmpdir=${mms.tmpdir} \
    -classpath "${mms.home}/lib/*" \
    org.esa.cci.sst.tools.PlotSamplingPointFileTool "$@"