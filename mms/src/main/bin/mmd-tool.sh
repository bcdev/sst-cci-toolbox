#! /bin/sh

. $MMS_INST/mymms
. $MMS_HOME/bin/mms-env.sh

java \
    -Dmms.home="$MMS_HOME" \
    -Xmx4G $MMS_OPTIONS \
    -javaagent:"$MMS_HOME/lib/openjpa-all-${openjpaversion}.jar" \
    -classpath "$MMS_HOME/lib/*" \
    org.esa.cci.sst.tools.mmdgeneration.MmdTool "$@"
