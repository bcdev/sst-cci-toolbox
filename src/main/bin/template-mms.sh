#!/bin/bash
. mymms
nohup bash -c 'python mms.py' > mms.out &
sleep 2
cat mms.status
