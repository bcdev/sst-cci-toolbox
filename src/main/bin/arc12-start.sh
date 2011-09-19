#!/bin/bash

. `dirname $0`/mms-env.sh

year=$1
month=$2
# optional parameters
parts=${3:-a b c d}
sensors=${4:-n10 n11 n12 n14 n15 n16 n17 n18 n19 m02}

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks for arc12 $year/$month"

read_task_jobs arc12

if [ -z $jobs ]; then
    for sensor in $sensors
    do
        if [ ! -e $MMS_ARCHIVE/avhrr.$sensor/v1/$year/$month ]; then
            continue;
        fi

        if [ $sensor == n15 -a $year == 2010 -a $month -gt 02 ]; then
            continue
        fi

        for part in $parts
        do
            echo "`date -u +%Y%m%d-%H%M%S` submitting job arc12 $year/$month quartal $part sensor $sensor"

            line=`qsub -l h_rt=24:00:00 -j y -cwd -o $MMS_LOG/arc12-$year-$month-$part-$sensor.out -N a1-$year$month$part-$sensor $MMS_HOME/bin/arc12-run.sh $year $month $part avhrr_orb.$sensor`
            echo $line
            job=`echo $line | awk '{ print $3 }'`
            if [ "$jobs" != "" ]
            then
                jobs="$jobs|$job"
            else
                jobs="$job"
            fi
            echo "$MMS_LOG/arc12-$year-$month-$part-$sensor.out/$job" >> $MMS_TASKS/arc12-$year-$month.tasks
            
        done
    done
fi

wait_for_task_jobs_completion arc12
