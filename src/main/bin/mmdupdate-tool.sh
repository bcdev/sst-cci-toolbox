#! /bin/sh

if [ ! -d "$MMS_HOME" ]
then
    PRGDIR=`dirname $0`
    export MMS_HOME=`cd "$PRGDIR/.." ; pwd`
fi

MMS_OPTIONS=""
if [ ! -z $MMS_DEBUG ]; then
    MMS_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
fi

java \
    -Dmms.home="$MMS_HOME" \
    -Xmx3072M $MMS_OPTIONS \
    -javaagent:"$MMS_HOME/lib/openjpa-all-2.1.0.jar" \
    -classpath "$MMS_HOME/lib/*" \
    org.esa.cci.sst.tools.mmdgeneration.MmdUpdater "$@"
