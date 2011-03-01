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

java \
    -Xmx1024M \
    -javaagent:"$CCI_SST_HOME/lib/openjpa-all.jar" \
    -classpath "$CCI_SST_HOME/lib/*" \
    org.esa.cci.sst.IngestionClient "$@"

exit 0
