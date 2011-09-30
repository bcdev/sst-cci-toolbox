#! /bin/sh

. $MMS_INST/mymms
. $MMS_HOME/bin/mms-env.sh

ulimit -a
echo $TMPDIR

exec java \
    -Dmms.home="$MMS_HOME" \
    -Xmx3872M $MMS_OPTIONS \
    -Djava.io.tmpdir=$TMPDIR \
    -javaagent:"$MMS_HOME/lib/openjpa-all-2.1.0.jar" \
    -classpath "$MMS_HOME/lib/*" \
    org.esa.cci.sst.tools.mmdgeneration.MmdTool "$@"
