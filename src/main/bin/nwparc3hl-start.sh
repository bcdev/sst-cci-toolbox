#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2
# optional parameters
#sensors=${4:-atsr.3 atsr.2 atsr.1 avhrr.n10 avhrr.n11 avhrr.n12 avhrr.n14 avhrr.n15 avhrr.n16 avhrr.n17 avhrr.n18 avhrr.n19 avhrr.m02}
sensors='atsr.3 atsr.2 atsr.1 avhrr.n10 avhrr.n11 avhrr.n12 avhrr.n14 avhrr.n15 avhrr.n16 avhrr.n17 avhrr.n18 avhrr.n19 avhrr.m02'

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks for nwparc3 $year/$month"

read_task_jobs nwparc3hl

if [ -z $jobs ]; then
    for sensor in $sensors
    do
        if [ ! -e $MMS_ARCHIVE/$sensor/v1/$year/$month ]; then
            continue;
        fi

        if [ $sensor == n15 -a $year == 2010 -a $month -gt 02 ]; then
            continue
        fi

        echo "`date -u +%Y%m%d-%H%M%S` submitting job nwparc3hl $year/$month quartal sensor $sensor"

        line=`qsub -l h_rt=24:00:00,sages_1ppn=1 -j y -cwd -o $MMS_LOG/nwparc3hl-$year-$month-$sensor.out -N h3-$year$month-$sensor $MMS_HOME/bin/nwparc3hl-run.sh $year $month $sensor`
        echo $line
        job=`echo $line | awk '{ print $3 }'`
        if [ "$jobs" != "" ]
        then
            jobs="$jobs|$job"
        else
            jobs="$job"
        fi
        echo "$MMS_LOG/nwparc3hl-$year-$month-$sensor.out/$job" >> $MMS_TASKS/nwparc3hl-$year-$month.tasks
    done
fi

wait_for_task_jobs_completion nwparc3hl
