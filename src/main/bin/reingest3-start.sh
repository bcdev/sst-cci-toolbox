#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2
# optional parameters
#parts=${3:-a b c d e f}
#sensors=$4
parts='a b c d e f'
sensors=

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks for reingest3 $year/$month"

read_task_jobs reingest3

if [ -z $jobs ]; then
    for part in $parts
    do
        
        echo "`date -u +%Y%m%d-%H%M%S` submitting job reingest3 $year/$month quartal $part"
        line=`qsub -l h_rt=24:00:00,sages_1ppn=1 -j y -cwd -o $MMS_LOG/reingest3-$year-$month-$part.out -N r3-$year$month$part $MMS_HOME/bin/reingest3-run.sh $year $month $part $sensors`
        echo $line
        job=`echo $line | awk '{ print $3 }'`
        if [ "$jobs" != "" ]
        then
            jobs="$jobs|$job"
        else
            jobs="$job"
        fi
        echo "$MMS_LOG/reingest3-$year-$month-$part.out/$job" >> $MMS_TASKS/reingest3-$year-$month.tasks
    done
fi

wait_for_task_jobs_completion reingest3
