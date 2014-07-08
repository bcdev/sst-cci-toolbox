#!/bin/bash
# MMS environment setup
# usage:  . mymms

umask 007

export PGPORT=${mms.pg.port}
export PGHOST=${mms.pg.host}
export PGDATA=${mms.pg.data}

export PATH=${mms.pg.home}/bin:${PATH}
export PATH=${mms.jdk.home}/bin:${PATH}
export PATH=${mms.usr.local}/bin:${PATH}
export LD_LIBRARY_PATH=${mms.usr.local}/lib:${LD_LIBRARY_PATH}

export MMS_HOME=${mms.home}
export MMS_ARCHIVE=${mms.archive.root}

export MMS_INST=${mms.work}/inst-mms1
export MMS_TASKS=${MMS_INST}/tasks
export MMS_LOG=${MMS_INST}/log
export TMPDIR=${MMS_INST}/tmp

export PYTHONPATH=${MMS_INST}:${MMS_HOME}/python:${PYTHONPATH}
export PATH=${MMS_HOME}/bin:${PATH}

echo "using MMS instance $MMS_INST"
echo "using MMS software $MMS_HOME"
