#!/bin/bash

# Control whether to debug...
testing="" #"echo "
#
######################################################
#
# All input data is held here..
AVHRR_DIR=/exports/nas/exports/cse/geos/scratch/gc/sst-cci/archive/cciout/lt/l2p
ATSR_DIR=/exports/nas/exports/cse/geos/scratch/gc/sst-cci/archive/cciout/lt/l3u/
TYPE=ATSR
DATA_DIR=$(eval echo \${${TYPE}_DIR})
# Declare a function to handle one month of data at a time, creating a qsub
#  job for each 'day' subdirectory (on which we run the python code).
#
function process_mon {
  first=0
  for day in $(ls ${DATA_DIR}/${channel_prev}/v01/${yr_prev}/${mon_prev} )
  do
    OUTTXTDIR=/exports/work/geos_gc_sst_cci/${USER}_TMP/${channel_prev}/${yr_prev}/${mon_prev}/${day}
    job_id=process_${channel_prev}_${yr_prev}_${mon_prev}_${day}
    ${testing} qsub -hold_jid ${copy_id_prev} -N ${job_id} svr-process_daydir.sh ${TMP_DIR_prev}/${day} ${OUTTXTDIR} ${TYPE}
    ${testing}
    if [ ${first} == 0 ]
    then
      hold_list="-hold_jid "${job_id}
      first=1
    else
      hold_list=${hold_list}","${job_id}
    fi
  done
}
#
### MAIN CODE BELOW ###
#
# variable to keep track of nos of times we've been through the loop..
counter=0
#
# loop 1, over channels..
#   loop 2, over years..
#     loop 3, over months
#for channel in $(ls ${DATA_DIR})
#do
#channel=AVHRR18_G
#channel=ATSR1
#channel=ATSR2
channel=AATSR
  for yr in $(ls ${DATA_DIR}/${channel}/v01)
  do
    for mon in $(ls ${DATA_DIR}/${channel}/v01/${yr})
    do
      #
      # Directory to copy from..
      COPY_DIR=${DATA_DIR}/${channel}/v01/${yr}/${mon}
      #
      # Directory to copy to (create if doesn't exist)..
      TMP_DIR=/exports/work/tmp/$USER/${channel}/${yr}/${mon}
      mkdir -p ${TMP_DIR}
      #
      # job-id for the copy..
      copy_id=copy_${channel}_${yr}_${mon}
      #
      # First time through: don't worry about what came before, just copy copy copy :)
      # 2nd time through: when 1st copy finishes..
      #                            start new copy
      #                            start processing of 1st copy
      # 3rd (+ all subsequent) times through: when previous processing finished..
      #                            start new copy
      #                            start processing of previous copy
      #
      if [ ${counter} == 0 ]
      then
	# first time through..
	${testing} qsub -N ${copy_id} svr-cesd_sge_copy_to_NAS.sh ${COPY_DIR} ${TMP_DIR}
	${testing}
      elif [ ${counter} == 1 ]
      then
	# second time through..
	${testing} qsub -hold_jid ${copy_id_prev} -N ${copy_id} svr-cesd_sge_copy_to_NAS.sh ${COPY_DIR} ${TMP_DIR}
	${testing}
        # and start processing the data from that previous copy job (ie month) that has completed..
        process_mon
      else
	# every other time through..
	${testing} qsub ${hold_list} -N ${copy_id} svr-cesd_sge_copy_to_NAS.sh ${COPY_DIR} ${TMP_DIR}
	${testing}
	# and processing the previous copy..
        process_mon
      fi
      ((counter+=1))
      #
      # Save some vars for use next iteration..
      #
      copy_id_prev=${copy_id}
      mon_prev=${mon}
      yr_prev=${yr}
      channel_prev=${channel}
      TMP_DIR_prev=/exports/work/tmp/$USER/${channel_prev}/${yr_prev}/${mon_prev}
      #
    done
  done
#done
#
# And remember to process that last copy..
#
process_mon