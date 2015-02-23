#!/bin/bash

files=/disk/scratch/local.2/cbulgin/SVR_DV/*.nc

for f in $files
do
    python dv_verification.py $f
done


