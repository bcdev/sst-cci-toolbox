#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks to generate rrdp algsel mmd $year/$month"

read_task_jobs rrdpalgsel

if [ -z $jobs ]; then
    line=`qsub -l h_rt=47:00:00,s_fsize=10G,sages_2ppn=1 -pe memory-2G 2 -j y -cwd -o $MMS_LOG/rrdpalgsel-$year-$month.out -N in-$year$month $MMS_HOME/bin/rrdpalgsel-run.sh $year $month`
    echo $line
    jobs=`echo $line | awk '{ print $3 }'`
    echo "$MMS_LOG/rrdpalgsel-$year-$month.out/$job" > $MMS_TASKS/rrdpalgsel-$year-$month.tasks
fi

wait_for_task_jobs_completion rrdpalgsel
