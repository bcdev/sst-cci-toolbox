#!/bin/bash

. $MMS_INST/mymms

MMS_OPTIONS=""
if [ ! -z ${MMS_DEBUG} ]; then
    MMS_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
fi

${mms.jdk.home}/bin/java \
    -Dmms.home="${mms.home}" \
    -Djava.io.tmpdir=${TMPDIR} \
    -Xmx1G $MMS_OPTIONS \
    -javaagent:"${mms.home}/lib/openjpa-all-${openjpaversion}.jar" \
    -classpath "${mms.home}/lib/*" \
    org.esa.cci.sst.tools.GbcsTool $@
