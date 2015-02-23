#!/bin/bash

# Grid Engine options
#$ -l h_rt=01:00:00
#$ -cwd
#$ -o sge_out/output/
#$ -e sge_out/error/

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
    python verify_SST_CCI_data.py ${file} ${TYPE} ${OUT_DIR}
done

# when complete, delete the temporary files here!!
rm -f ${TDIR}