#! /bin/sh

. `dirname $0`/mms-env.sh

# nwp-tool.sh atsr.1 8 true atsr.1-sub-19960602000000-19960603000000.nc \
#     /exports/nas/exports/cse/geos/scratch/gc/sst-cci/ecmwf-era-interim/v01 \
#     atsr.1-nwp-19960602000000-19960603000000.nc \
#     atsr.1-nwpAn-19960602000000-19960603000000.nc \
#     atsr.1-nwpFc-19960602000000-19960603000000.nc
# export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:...cdoinstalldir/lib
# export PATH=${PATH}:...cdoinstalldir/bin

java \
    -Dmms.home="$MMS_HOME" \
    -Djava.io.tmpdir=`pwd` \
    -Xmx1024M $MMS_OPTIONS \
    -javaagent:"$MMS_HOME/lib/openjpa-all-${openjpa.version}.jar" \
    -classpath "$MMS_HOME/lib/*" \
    org.esa.cci.sst.tools.nwp.Nwp $@
