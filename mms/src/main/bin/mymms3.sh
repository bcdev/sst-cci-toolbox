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

if [ -n "${LD_LIBRARY_PATH}" ]
then
    export LD_LIBRARY_PATH=${mms.pg.home}/lib:${mms.usr.local}/lib:${LD_LIBRARY_PATH}
else
    export LD_LIBRARY_PATH=${mms.pg.home}/lib:${mms.usr.local}/lib
fi

export MMS_ARCHIVE=${mms.archive.root}

export MMS_INST=${mms.work}/inst-mms3
export MMS_TASKS=${MMS_INST}/tasks
export MMS_LOG=${MMS_INST}/log
export TMPDIR=${MMS_INST}/tmp

export PYTHONPATH=${MMS_INST}:${mms.home}/python:${PYTHONPATH}
export PATH=${mms.home}/bin:${PATH}

echo "using MMS instance $MMS_INST"
echo "using MMS software ${mms.home}"
