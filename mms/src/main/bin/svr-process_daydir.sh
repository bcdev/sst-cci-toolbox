#!/bin/bash

# Use ABSOLUTE directory paths!
SOURCE_DIR=$1
TARGET_DIR=$2
TYPE=$3

# check directory exists..
mkdir -p ${TARGET_DIR}

# loop over files in the directoy..
for f in $(ls ${SOURCE_DIR}/*.nc)
do
    ${mms.python.exec} ${mms.home}/python/svr-verify_SST_CCI_data.py ${f} ${TYPE} ${TARGET_DIR}
done
