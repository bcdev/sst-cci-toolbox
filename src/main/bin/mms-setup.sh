#!/bin/bash

inst=${1:?usage: mms-setup <instance-dir>}

bindir=`dirname $0`
export MMS_HOME=`cd $bindir/..; pwd`

mkdir -p $1
cd $1
MMS_INST=`pwd`

. $MMS_HOME/bin/mms-env.sh

mkdir -p $MMS_INST/{log,tasks}
test -e $MMS_INST/mms.py || cp $MMS_HOME/python/template-mms.py $MMS_INST/mms.py
test -e $MMS_INST/mymms  || cp $MMS_HOME/bin/template-mymms $MMS_INST/mymms
test -e $MMS_INST/mms.sh || cp $MMS_HOME/bin/template-mms.sh $MMS_INST/mms.sh
test -e $MMS_INST/mms-eddie.properties || cp $MMS_HOME/config/template-mms-eddie.properties $MMS_INST/mms-eddie.properties
chmod +x $MMS_INST/mymms $MMS_INST/mms.sh

ls -l $MMS_INST
