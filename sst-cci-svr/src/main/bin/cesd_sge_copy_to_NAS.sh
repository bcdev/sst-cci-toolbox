#!/bin/bash

# Grid Engine options
#$ -cwd
#$ -o sge_out/output/
#$ -e sge_out/error/
#$ -l h_rt=12:00:00
#$ -pe staging 1

. /etc/profile.d/modules.sh
. ~/.bash_profile

rsync -av $1/* $2

