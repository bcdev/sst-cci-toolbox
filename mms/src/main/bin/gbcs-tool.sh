#!/bin/bash

. $MMS_INST/mymms
. $MMS_HOME/bin/mms-env.sh

java \
    -Dmms.home="$MMS_HOME" \
    -Djava.io.tmpdir=${TMPDIR} \
    -Xmx1G $MMS_OPTIONS \
    -javaagent:"$MMS_HOME/lib/openjpa-all-${openjpaversion}.jar" \
    -classpath "$MMS_HOME/lib/*" \
    org.esa.cci.sst.tools.GbcsTool $@
