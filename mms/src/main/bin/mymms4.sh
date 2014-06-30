#!/bin/bash
# MMS environment setup
# usage:  . mymms

umask 007

export PGPORT=${mms.pg.port}
export PGHOST=${mms.pg.host}
export PGDATA=/data/mboettcher/mms/db/mmdb

export PATH=${mms.jdk.home}/bin:${PATH}
export PATH=/group_workspaces/cems/esacci_sst/mms/software/bin:${PATH}
export LD_LIBRARY_PATH=/group_workspaces/cems/esacci_sst/mms/software/lib:/group_workspaces/cems/esacci_sst/software/common/lib:${LD_LIBRARY_PATH}

export MMS_HOME=${mms.home}
export MMS_ARCHIVE=${mms.archive.root}

export MMS_INST=/group_workspaces/cems/esacci_sst/mms/inst-mms4
export MMS_TASKS=${MMS_INST}/tasks
export MMS_LOG=${MMS_INST}/log
export TMPDIR=${MMS_INST}/tmp

export PYTHONPATH=${MMS_INST}:${MMS_HOME}/python:${PYTHONPATH}
export PATH=${MMS_HOME}/bin:${PATH}

echo "using MMS instance $MMS_INST"
echo "using MMS software $MMS_HOME"
