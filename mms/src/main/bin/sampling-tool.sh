#! /bin/sh

. ${MMS_INST}/mymms

MMS_OPTIONS=""
if [ ! -z ${MMS_DEBUG} ]; then
    MMS_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
fi

java \
    -Dmms.home="${mms.home}" \
    -Xmx8G ${MMS_OPTIONS} \
    -javaagent:"${mms.home}/lib/openjpa-all-${openjpaversion}.jar" \
    -Djava.io.tmpdir=${TMPDIR} \
    -classpath "${mms.home}/lib/*" \
    org.esa.cci.sst.tools.SamplingPointGenerationTool "$@"
