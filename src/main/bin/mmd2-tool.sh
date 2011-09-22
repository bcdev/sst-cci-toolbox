#! /bin/sh

. $MMS_INST/mymms
. $MMS_HOME/bin/mms-env.sh

ulimit -Sv unlimited
ulimit unlimited

java \
    -Dmms.home="$MMS_HOME" \
    -Xmx3072M $MMS_OPTIONS \
    -javaagent:"$MMS_HOME/lib/openjpa-all-2.1.0.jar" \
    -classpath "$MMS_HOME/lib/*" \
    org.esa.cci.sst.tools.mmdgeneration.MmdTool "$@"
