#! /bin/sh

MMS_OPTIONS=""
if [ ! -z ${MMS_DEBUG} ]; then
    MMS_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
fi

${mms_jdk_home}/bin/java \
    -Dmms_home="${mms_home}" \
    -Xms8G -Xmx8G ${MMS_OPTIONS} \
    -javaagent:"${mms_home}/lib/openjpa-all-${openjpaversion}.jar" \
    -classpath "${mms_home}/lib/*" \
    org.esa.cci.sst.tools.MatchupTool "$@"
