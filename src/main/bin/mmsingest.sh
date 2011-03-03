#! /bin/sh

if [ ! -d "$CCI_SST_HOME" ]
then
    PRGDIR=`dirname $0`
    export CCI_SST_HOME=`cd "$PRGDIR/.." ; pwd`
fi

if [ -z "$CCI_SST_HOME" ]; then
    echo
    echo Error:
    echo CCI_SST_HOME does not exists in your environment. Please
    echo set the CCI_SST_HOME variable in your environment to the
    echo location of your CCI SST installation.
    echo
    exit 2
fi

MMS_OPTIONS=""
if [ ! -z $MMS_DEBUG ]; then
    MMS_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
fi

java \
    -Xmx1024M $MMS_OPTIONS \
    -javaagent:"$CCI_SST_HOME/lib/openjpa-all.jar" \
    -classpath "$CCI_SST_HOME/lib/*" \
    org.esa.cci.sst.IngestionTool "$@"
