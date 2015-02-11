#!/bin/bash

export IO_TEST_DIR="$(dirname $0)"

#------------------------------------------------------------------
# You can adjust the Java minimum and maximum heap space here.
# Just change the Xms and Xmx options. Space is given in megabyte.
#    '-Xms64M' sets the minimum heap space to 64 megabytes
#    '-Xmx512M' sets the maximum heap space to 512 megabytes
#------------------------------------------------------------------
export JAVA_OPTS="-Xmx20G"
export JAVA_EXE="$(which java)"

# -======================-
# Other values
# -======================-

export LIBDIR="$IO_TEST_DIR"/lib
export OLD_CLASSPATH="$CLASSPATH"
CLASSPATH="$LIBDIR/*:$LIBDIR"

"$JAVA_EXE" "$JAVA_OPTS" -classpath "$CLASSPATH" org.esa.cci.sst.tools.matchup.MatchupOutMain "$@"

export CLASSPATH="$OLD_CLASSPATH"