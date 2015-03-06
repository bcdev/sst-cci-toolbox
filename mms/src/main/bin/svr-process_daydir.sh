#!/bin/bash

DATA_DIR=$1
OUT_DIR=$2
TYPE=$3

# check directory exists..
mkdir -p ${OUT_DIR}

# loop over files in the directoy..
for file in $(ls ${DATA_DIR}/*.nc)
do
    # run python code..
    python svr-verify_SST_CCI_data.py ${file} ${TYPE} ${OUT_DIR}
done
