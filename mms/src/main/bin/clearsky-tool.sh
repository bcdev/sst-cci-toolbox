#! /bin/sh

. $MMS_INST/mymms
. $MMS_HOME/bin/mms-env.sh

java \
    -Dmms.home="$MMS_HOME" \
    -Xmx3G $MMS_OPTIONS \
    -javaagent:"$MMS_HOME/lib/openjpa-all-${openjpaversion}.jar" \
    -Djava.io.tmpdir=$TMPDIR \
    -classpath "$MMS_HOME/lib/*" \
    org.esa.cci.sst.tools.MatchupGenerator "$@"
