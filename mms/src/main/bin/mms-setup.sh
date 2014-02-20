#!/bin/bash

bindir=`dirname $0`
MMS_HOME=`cd ${bindir}/..;pwd`

. ${MMS_HOME}/bin/mms-env.sh

instdir=${1:?usage: mms-setup.sh <instance-dir>}
mkdir -p $instdir
MMS_INST=`cd ${instdir};pwd`

mkdir -p ${MMS_INST}/{log,tasks,trash}
for f in ${MMS_HOME}/{bin,config,python}/template-*
do
    n=`basename ${f}`
    n=${n#template-}
    test -e ${MMS_INST}/${n} || cp -p ${f} ${MMS_INST}/${n}
done

ls -l ${MMS_INST}
