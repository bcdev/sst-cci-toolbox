#! /bin/sh

MMS_OPTIONS=""
if [ ! -z ${MMS_DEBUG} ]; then
    MMS_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
fi

java \
    -Dmms_home="${mms_home}" \
    -Xms1G -Xmx1G ${MMS_OPTIONS} \
    -Djava.io.tmpdir=${mms_tmpdir} \
    -classpath "${mms_home}/lib/*" \
    org.esa.cci.sst.tools.PlotSamplingPointFileTool "$@"