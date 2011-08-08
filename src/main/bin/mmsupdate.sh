#! /bin/sh

# mmsupdate.sh expects the following configuration parameters:
#   mms.reingestion.filename  path to MMD' file
#   mms.archive.rootdir       optional prefix for relative filenames
#   openjpa.*                 parameters for database selection

if [ ! -d "$MMS_HOME" ]
then
    PRGDIR=`dirname $0`
    export MMS_HOME=`cd "$PRGDIR/.." ; pwd`
fi

if [ -z "$MMS_HOME" ]; then
    echo
    echo Error:
    echo MMS_HOME does not exists in your environment. Please
    echo set the MMS_HOME variable in your environment to the
    echo location of your CCI SST installation.
    echo
    exit 2
fi

MMS_OPTIONS=""
if [ ! -z $MMS_DEBUG ]; then
    MMS_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
fi

java \
    -Dmms.home="$MMS_HOME" \
    -Xmx1024M $MMS_OPTIONS \
    -javaagent:"$MMS_HOME/lib/openjpa-all-${openjpa.version}.jar" \
    -classpath "$MMS_HOME/lib/*" \
    org.esa.cci.sst.tools.ingestion.FlagsUpdateTool "$@"
