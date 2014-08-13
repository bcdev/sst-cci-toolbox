#! /bin/sh

. $MMS_INST/mymms
. ${mms.home}/bin/mms-env.sh

java \
    -Dmms.home="${mms.home}" \
    -Djava.io.tmpdir=${TMPDIR} \
    -Xmx1G $MMS_OPTIONS \
    -javaagent:"${mms.home}/lib/openjpa-all-${openjpaversion}.jar" \
    -classpath "${mms.home}/lib/*" \
    org.esa.cci.sst.tools.nwp.NwpTool $@
