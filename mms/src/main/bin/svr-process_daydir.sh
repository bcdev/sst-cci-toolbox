#!/bin/bash

# Grid Engine options
#$ -l h_rt=01:00:00
#$ -cwd
#$ -o sge_out/output/
#$ -e sge_out/error/

# @todo remove dependency to unknown resources tb 2015-03-06
. /etc/profile.d/modules.sh
. ~/.bash_profile
module load python

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

# when complete, delete the temporary files here!!
# @todo define TDIR either from cmd line or other parameter file tb 2015-02-23
# @todo i couldn't even find any usage of this parameter in the SVR files, maybe it's redundant code?
rm -f ${TDIR}