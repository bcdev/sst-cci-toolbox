#!/bin/bash
# MMS environment setup
# usage:  . mymms

umask 007

export PGPORT=${mms.pg.port}
export PGHOST=${mms.pg.host}
export PGDATA=${mms.pg.data}

export PATH=${mms.jdk.home}/bin:${PATH}
export PATH=${mms.software.root}/bin:${PATH}
export LD_LIBRARY_PATH=${mms.software.root}/lib:${mms.software.root}/common/lib:${LD_LIBRARY_PATH}

export MMS_HOME=${mms.home}
export MMS_ARCHIVE=${mms.archive.root}

export MMS_INST=${mms.work}/inst-mms3
export MMS_TASKS=${MMS_INST}/tasks
export MMS_LOG=${MMS_INST}/log
export TMPDIR=${MMS_INST}/tmp

export PYTHONPATH=${MMS_INST}:${MMS_HOME}/python:${PYTHONPATH}
export PATH=${MMS_HOME}/bin:${PATH}

echo "using MMS instance $MMS_INST"
echo "using MMS software $MMS_HOME"
