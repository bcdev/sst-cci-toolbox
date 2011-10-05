#! /bin/sh

. $MMS_INST/mymms
. $MMS_HOME/bin/mms-env.sh

java \
    -Dmms.home="$MMS_HOME" \
    -Xmx3072M $MMS_OPTIONS \
    -javaagent:"$MMS_HOME/lib/openjpa-all-2.1.0.jar" \
    -Djava.io.tmpdir=$TMPDIR \
    -classpath "$MMS_HOME/lib/*" \
    org.esa.cci.sst.tools.mmdgeneration.MmdUpdater "$@"
