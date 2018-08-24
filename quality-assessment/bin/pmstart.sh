#!/bin/bash

if [ -z "$1" ]; then
    echo "call   : pmstartup <workflow>"
    echo "example: pmstartup modis.py"
    exit 1
fi

if [ -z "$MMS_INST" ]; then
    MMS_INST=`pwd`
fi

workflow=$(basename ${1%.py})

if [ -e ${workflow}.pid ]
then
    if kill -0 $(cat $workflow.pid) 2> /dev/null
    then
        ps -elf | grep $(cat $workflow.pid) | grep -v grep
        echo "process already running"
        echo "delete $workflow.pid file if running process is not the workflow"
        exit 1
    fi
fi

nohup ${SVR_PYTHON_EXEC} ${mms.home}/python/$workflow.py > $MMS_INST/$workflow.out 2>&1 &
echo $! > $MMS_INST/$workflow.pid
sleep 8
cat $MMS_INST/$workflow.status
