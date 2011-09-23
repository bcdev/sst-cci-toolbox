#!/bin/bash

. $MMS_HOME/bin/mms-env.sh
cd $MMS_INST

year=$1
month=$2
# optional parameters
#sensors=${4:-n10 n11 n12 n14 n15 n16 n17 n18 n19 m02}
sensors='n10 n11 n12 n14 n15 n16 n17 n18 n19 m02'

echo "`date -u +%Y%m%d-%H%M%S` submitting tasks for arc12hl $year/$month"

read_task_jobs arc12hl

if [ -z $jobs ]; then
    for sensor in $sensors
    do
        if [ ! -e $MMS_ARCHIVE/avhrr.$sensor/v1/$year/$month ]; then
            continue;
        fi

        if [ $sensor == n15 -a $year == 2010 -a $month -gt 02 ]; then
            continue
        fi

        echo "`date -u +%Y%m%d-%H%M%S` submitting job arc12hl $year/$month quartal $part sensor $sensor"

        line=`qsub -l h_rt=24:00:00,sages_1ppn=1 -j y -cwd -o $MMS_LOG/arc12hl-$year-$month-$sensor.out -N h1-$year$month-$sensor $MMS_HOME/bin/arc12hl-run.sh $year $month avhrr_orb.$sensor`
        echo $line
        jobs=`echo $line | awk '{ print $3 }'`
        echo "$MMS_LOG/arc12hl-$year-$month-$sensor.out/$job" >> $MMS_TASKS/arc12hl-$year-$month.tasks
    done
fi

wait_for_task_jobs_completion arc12hl
