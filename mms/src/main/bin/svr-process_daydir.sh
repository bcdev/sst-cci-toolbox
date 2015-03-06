#!/bin/bash

DATA_DIR=$1
OUT_DIR=$2
TYPE=$3

# check directory exists..
mkdir -p ${OUT_DIR}

# loop over files in the directoy..
for input_file in $(ls ${DATA_DIR}/*.nc)
do
    # run python code..
    ${mms.python.exec} ${mms.home}/python/svr-verify_SST_CCI_data.py ${input_file} ${TYPE} ${OUT_DIR}
done
