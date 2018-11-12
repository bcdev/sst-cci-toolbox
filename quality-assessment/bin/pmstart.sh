#!/bin/bash

echo "pmstart"

if [ -z "$1" ]; then
    echo "call   : pmstart.sh <workflow>"
    echo "example: pmstart.sh usecase-17.py"
    exit 1
fi

WORKING_DIR=`pwd`
echo "working dir: $WORKING_DIR"

workflow=$(basename ${1%.py})
echo "workflow: $workflow"

if [ -e ${workflow}.pid ]
then
    if kill -0 $(cat ${workflow}.pid) 2> /dev/null
    then
        ps -elf | grep $(cat ${workflow}.pid) | grep -v grep
        echo "process already running"
        echo "delete $workflow.pid file if running process is not the workflow"
        exit 1
    fi
fi

nohup ${PM_PYTHON_EXEC} ${PM_EXE_DIR}/python/${workflow}.py > ${WORKING_DIR}/${workflow}.out 2>&1 &
echo $! > ${WORKING_DIR}/${workflow}.pid
sleep 8
cat $WORKING_DIR/$workflow.status