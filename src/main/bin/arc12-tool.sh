#! /bin/sh

. `dirname $0`/mms-env.sh

java \
    -Dmms.home="$MMS_HOME" \
    -Xmx1024M $MMS_OPTIONS \
    -javaagent:"$MMS_HOME/lib/openjpa-all-${openjpa.version}.jar" \
    -classpath "$MMS_HOME/lib/*" \
    org.esa.cci.sst.tools.arcprocessing.Arc1ProcessingTool "$@"
