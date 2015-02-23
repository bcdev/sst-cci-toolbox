#!/bin/bash

files=/disk/scratch/local.2/cbulgin/SVR_DV/new/20100310221800-ESACCI-L2P_GHRSST-SSTskin-AVHRR17_G-LT-v02.0-fv01.0.nc

for f in $files
do 
    python qf_check.py $f
done
