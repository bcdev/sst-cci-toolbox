#! /bin/sh

. $MMS_INST/mymms
. $MMS_HOME/bin/mms-env.sh

# flags-tool.sh expects the following configuration parameters:
#   mms.reingestion.filename  path to MMD' file
#   mms.archive.rootdir       optional prefix for relative filenames
#   openjpa.*                 parameters for database selection

java \
    -Dmms.home="$MMS_HOME" \
    -Xmx1024M $MMS_OPTIONS \
    -javaagent:"$MMS_HOME/lib/openjpa-all-${openjpaversion}.jar" \
    -classpath "$MMS_HOME/lib/*" \
    org.esa.cci.sst.tools.ingestion.FlagsUpdateTool "$@"
