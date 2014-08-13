#! /bin/sh

. ${MMS_INST}/mymms
. ${mms.home}/bin/mms-env.sh

java \
    -Dmms.home="${mms.home}" \
    -Xmx8G ${MMS_OPTIONS} \
    -javaagent:"${mms.home}/lib/openjpa-all-${openjpaversion}.jar" \
    -Djava.io.tmpdir=${TMPDIR} \
    -classpath "${mms.home}/lib/*" \
    org.esa.cci.sst.tools.MapPlotTool "$@"
