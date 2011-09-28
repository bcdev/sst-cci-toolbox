#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2
# optional parameters
#parts=${3:-a b c d e f}
#sensors=${4:-atsr.3 atsr.2 atsr.1 avhrr.n10 avhrr.n11 avhrr.n12 avhrr.n14 avhrr.n15 avhrr.n16 avhrr.n17 avhrr.n18 avhrr.n19 avhrr.m02}
parts='a b c d e f'
sensors='atsr.3 atsr.2 atsr.1 avhrr.n10 avhrr.n11 avhrr.n12 avhrr.n14 avhrr.n15 avhrr.n16 avhrr.n17 avhrr.n18 avhrr.n19 avhrr.m02'

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks for nwparc3 $year/$month"

read_task_jobs nwparc3

if [ -z $jobs ]; then
    for sensor in $sensors
    do
        if [ ! -e $MMS_ARCHIVE/$sensor/v1/$year/$month ]; then
            continue;
        fi

        if [ $sensor == n15 -a $year == 2010 -a $month -gt 02 ]; then
            continue
        fi

        for part in $parts
        do
            echo "`date -u +%Y%m%d-%H%M%S` submitting job nwparc3 $year/$month quartal $part sensor $sensor"

            line=`qsub -l h_rt=24:00:00,sages_1ppn=1 -j y -cwd -o $MMS_LOG/nwparc3-$year-$month-$part-$sensor.out -N a3-$year$month$part-$sensor $MMS_HOME/bin/nwparc3-run.sh $year $month $part $sensor`
            echo $line
            job=`echo $line | awk '{ print $3 }'`
            if [ "$jobs" != "" ]
            then
                jobs="$jobs|$job"
            else
                jobs="$job"
            fi
            echo "$MMS_LOG/nwparc3-$year-$month-$part-$sensor.out/$job" >> $MMS_TASKS/nwparc3-$year-$month.tasks
            
        done
    done
fi

wait_for_task_jobs_completion nwparc3
