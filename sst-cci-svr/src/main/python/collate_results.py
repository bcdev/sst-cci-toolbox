#!/usr/bin/env python
import sys, math
import numpy.ma as ma
import scipy as sp
from PIL import Image
import matplotlib
import matplotlib.pyplot as plt
from datetime import datetime
import traceback
import os

instrument = ['AVHRR12_G','AVHRR14_G','AVHRR15_G','AVHRR16_G','AVHRR17_G','AVHRR18_G','AVHRRMTA_G']
#instrument = ['AVHRR12_G','AVHRR14_G','AVHRRMTA_G']
#instrument = ['AVHRR17_G','AVHRR18_G']
#instrument = ['AVHRRMTA','SEVIRI_SST']
#instrument = ['ATSR1','AATSR']



for inst in instrument:

    SST_limit = 0.0
    SST_depth_limit = 0.0
    SST_diff = 0.0
    SST_dtime_limit = 0.0
    uncorr_uncert_limit = 0.0
    uncorr_uncert_masked = 0.0
    uncorr_uncert_avail_fail = 0.0
    uncorr_uncert_avail = 0.0
    uncorr_uncert_avail_total = 0.0
    large_scl_uncert_limit = 0.0
    large_scl_uncert_masked = 0.0
    large_scl_uncert_avail_fail = 0.0
    large_scl_uncert_avail = 0.0
    large_scl_uncert_avail_total = 0.0
    sses_std_limit = 0.0
    sses_std_masked = 0.0
    sses_std_avail_fail = 0.0
    sses_std_avail = 0.0
    sses_std_avail_total = 0.0
    synop_uncert_limit = 0.0
    synop_uncert_masked = 0.0
    synop_uncert_avail_fail = 0.0
    synop_uncert_avail = 0.0
    synop_uncert_avail_total = 0.0
    sses_bias_limit = 0.0
    sses_bias_masked = 0.0
    sses_bias_avail_fail = 0.0
    sses_bias_avail = 0.0
    sses_bias_avail_total = 0.0
    adjust_uncert_limit = 0.0
    adjust_uncert_masked = 0.0
    adjust_uncert_avail_fail = 0.0
    adjust_uncert_avail = 0.0
    adjust_uncert_avail_total = 0.0
    sst_depth_uncert_limit = 0.0
    latitude_limit = 0.0
    latitude_bands_limit = 0.0
    longitude_limit = 0.0
    longitude_bands_limit = 0.0
    time_limit = 0.0
    time_bands_limit = 0.0
    wind_speed_limit = 0.0
    quality_level_flag_limit = 0.0
    l2p_flag_limit = 0.0

    sst_total = 0.0
    sstd_total = 0.0
    SST_diff_total = 0.0
    SST_dtime_total = 0.0
    uncorr_uncert_total = 0.0
    uncorr_uncert_m_total = 0.0
    large_scl_total = 0.0
    large_scl_m_total = 0.0
    sses_std_total = 0.0
    sses_std_m_total = 0.0
    synop_uncert_total = 0.0
    synop_uncert_m_total = 0.0
    sses_bias_total = 0.0
    sses_bias_m_total = 0.0
    adjust_uncert_total = 0.0
    adjust_uncert_m_total = 0.0
    sst_depth_uncert_total = 0.0
    lat_total = 0.0
    lat_bnds_total = 0.0
    lon_total = 0.0
    lon_bnds_total = 0.0
    time_total = 0.0
    time_bnds_total = 0.0
    wind_speed_total = 0.0
    quality_level_total = 0.0
    l2p_flag_total = 0.0

    print inst
    
    file_count = 0.0
    empty_file_count = 0.0
    sst_corrupt = 0.0
    sst_depth_corrupt = 0.0
    adjust_uncert_corrupt = 0.0
    uncorr_uncert_corrupt = 0.0
    synop_uncert_corrupt = 0.0
    large_uncert_corrupt = 0.0
    synop_uncert = 0.0
    uncorr_uncert = 0.0
    large_uncert = 0.0
    adjust_uncert = 0.0
    no_data_count = 0.0
    
    root = '/disk/scratch/local.2/cbulgin/SVR/'+inst+'/'
    
    for dirpath, dirs, files in os.walk(root,topdown=True):

        for f in files:
            
            filename = os.path.join(dirpath,f)

            name = f

            f = open(filename,'r')

            file_count = file_count+1

            #Reads the header information
            f.readline()
            f.readline()
            f.readline()

            count = 0.0
            corruption_flag = 0.0
            print_file = 0.0


            for line in f:
                x = line.strip().split(',')
                error_code = float(x[0])
                error_count = float(x[1])
                total_count = float(x[2])
                error_percent = float(x[3])

                count = count + 1.0

#                if (error_percent > 50.0 and error_count != -1.0):
#                    print 'Error code ',error_code,' percentage exceeds 50%: ',\
#                        error_percent,' in file ',filename
            
                #SST Checks
                if (error_code == 0.0 and error_count != -1):
                    if (error_count == total_count and total_count != 0):
                        sst_corrupt = sst_corrupt + 1
                        corruption_flag = 1.0
                        if (print_file == 0.0):
 #                           print name
                            print_file = 1.0
                    elif (total_count == 0):
                        no_data_count = no_data_count + 1
                        corruption_flag = 1.0
                        if (print_file == 0.0):
 #                           print name
                            print_file = 1.0
                    else:
                        SST_limit = SST_limit + error_count
                        sst_total = sst_total + total_count
                if (error_code == 5.0 and error_count != -1):
                    if (error_count == total_count and total_count != 0):
                        sst_depth_corrupt = sst_depth_corrupt + 1
                        corruption_flag = 1.0
                        if (print_file == 0.0):
     #                       print name
                            print_file = 1.0
                    else:
                        SST_depth_limit = SST_depth_limit + error_count
                        sstd_total = sstd_total + total_count


            f.close()

            if (corruption_flag == 0.0):

                print name

                f = open(filename,'r')

            #Reads the header information
                f.readline()
                f.readline()
                f.readline()
                
                for line in f:
                    x = line.strip().split(',')
                    error_code = float(x[0])
                    error_count = float(x[1])
                    total_count = float(x[2])
                    error_percent = float(x[3])

                    if (error_code == 99.0 and error_count != -1):
                        SST_diff = SST_diff + error_count
                        SST_diff_total = SST_diff_total + total_count
                    if (error_code == 10.0 and error_count != -1):
                        SST_dtime_limit = SST_dtime_limit + error_count
                        SST_dtime_total = SST_dtime_total + total_count

                #Uncertainty Checks
                #Uncorrelated uncertainty
                    if (error_code == 40.0 and error_count != -1):
                        if (error_count == total_count and total_count != 0):
                            uncorr_uncert_corrupt = uncorr_uncert_corrupt + 1
                        else:
                            uncorr_uncert_limit = uncorr_uncert_limit + error_count
                            uncorr_uncert = 1
                            uncorr_uncert_total = uncorr_uncert_total + total_count
                    if (error_code == 41.0 and error_count != -1):
                        uncorr_uncert_masked = uncorr_uncert_masked + error_count
                        uncorr_uncert_m_total = uncorr_uncert_m_total + total_count
                    if (error_code == 42.0 and corruption_flag == 0.0):
                        if (error_count == -1.0):
                            uncorr_uncert_avail_fail = uncorr_uncert_avail_fail + 1
                        else:
                            uncorr_uncert_avail = uncorr_uncert_avail + error_count
                            uncorr_uncert_avail_total = uncorr_uncert_avail_total \
                                + total_count
                #Large scale correlated uncertainty
                    if (error_code == 30.0 and error_count != -1):
                        if (error_count == total_count and total_count != 0):
                            large_uncert_corrupt = large_uncert_corrupt + 1
                        else:
                            large_uncert = 1
                            large_scl_uncert_limit = large_scl_uncert_limit \
                                + error_count
                            large_scl_total = large_scl_total + total_count
                    if (error_code == 31.0 and error_count != -1):
                        large_scl_uncert_masked = large_scl_uncert_masked \
                            + error_count
                        large_scl_m_total = large_scl_m_total + total_count
                    if (error_code == 32.0 and corruption_flag == 0.0):
                        if (error_count == -1.0):
                            large_scl_uncert_avail_fail = \
                                large_scl_uncert_avail_fail + 1
                        else:
                            large_scl_uncert_avail = large_scl_uncert_avail \
                                + error_count
                            large_scl_uncert_avail_total = \
                                large_scl_uncert_avail_total + total_count
                #SSES standard deviation
                    if (error_code == 20.0 and error_count != -1):
                        sses_std_limit = sses_std_limit + error_count
                        sses_std_total = sses_std_total + total_count
                    if (error_code == 21.0 and error_count != -1):
                        sses_std_masked = sses_std_masked + error_count
                        sses_std_m_total = sses_std_m_total + total_count
                    if (error_code == 22.0 and corruption_flag == 0.0):
                        if (error_count == -1.0):
                            sses_std_avail_fail = sses_std_avail_fail + 1
                        else:
                            sses_std_avail = sses_std_avail + error_count
                            sses_std_avail_total = sses_std_avail_total \
                                + total_count
                #Synoptically correlated uncertainty
                    if (error_code == 35.0 and error_count != -1):
                        if (error_count == total_count and total_count != 0):
                            synop_uncert_corrupt = synop_uncert_corrupt + 1
                        else:
                            synop_uncert = 1
                            synop_uncert_limit = synop_uncert_limit + error_count
                            synop_uncert_total = synop_uncert_total + total_count
                    if (error_code == 36.0 and error_count != -1):
                        synop_uncert_masked = synop_uncert_masked + error_count
                        synop_uncert_m_total = synop_uncert_m_total + total_count
                    if (error_code == 37.0):
                        if (error_count == -1.0):
                            synop_uncert_avail_fail = synop_uncert_avail_fail + 1
                        else:
                            synop_uncert_avail = synop_uncert_avail + error_count
                            synop_uncert_avail_total = synop_uncert_avail_total \
                                + total_count
                #SSES bias 
                    if (error_code == 15.0 and error_count != -1):
                        sses_bias_limit = sses_bias_limit + error_count
                        sses_bias_total = sses_bias_total + total_count
                    if (error_code == 16.0 and error_count != -1):
                        sses_bias_masked = sses_bias_masked + error_count
                        sses_bias_m_total = sses_bias_m_total + total_count
                    if (error_code == 17.0):
                        if (error_count == -1.0):
                            sses_bias_avail_fail = sses_bias_avail_fail + 1
                        else:
                            sses_bias_avail = sses_bias_avail + error_count
                            sses_bias_avail_total = sses_bias_avail_total \
                                + total_count
                #Adjustment uncertainty
                    if (error_code == 45.0 and error_count != -1):
                        if (error_count == total_count and total_count != 0):
                            adjust_uncert_corrupt = adjust_uncert_corrupt + 1
                        else:
                            adjust_uncert = 1
                            adjust_uncert_limit = adjust_uncert_limit + error_count
                            adjust_uncert_total = adjust_uncert_total + total_count
                    if (error_code == 46.0 and error_count != -1):
                        adjust_uncert_masked = adjust_uncert_masked + error_count
                        adjust_uncert_m_total = adjust_uncert_m_total + total_count
                    if (error_code == 47.0):
                        if (error_count == -1.0):
                            adjust_uncert_avail_fail = adjust_uncert_avail_fail + 1
                        else:
                            adjust_uncert_avail = adjust_uncert_avail + error_count
                            adjust_uncert_avail_total = adjust_uncert_avail_total \
                                + total_count
                #SST depth uncertainty
                    if (error_code == 25.0 and error_count != -1):
                        sst_depth_uncert_limit = sst_depth_uncert_limit \
                            + error_count
                        sst_depth_uncert_total = sst_depth_uncert_total + total_count
                        
                #NWP variables
                #Lat, Lon and Time
                    if (error_code == 50.0 and error_count != -1):
                        latitude_limit = latitude_limit + error_count
                        lat_total = lat_total + total_count
                    if (error_code == 80.0 and error_count != -1):
                        latitude_bands_limit = latitude_bands_limit + error_count
                        lat_bnds_total = lat_bnds_total + total_count
                    if (error_code == 55.0 and error_count != -1):
                        longitude_limit = longitude_limit + error_count
                        lon_total = lon_total + total_count
                    if (error_code == 85.0 and error_count != -1):
                        longitude_bands_limit = longitude_bands_limit + error_count
                        lon_bnds_total = lon_bnds_total + total_count
                    if (error_code == 60.0 and error_count != -1):
                        time_limit = time_limit + error_count
                        time_total = time_total + total_count
                    if (error_code == 90.0 and error_count != -1):
                        time_bands_limit = time_bands_limit + error_count
                        time_bnds_total = time_bnds_total + total_count

                #Wind speed
                    if (error_code == 75.0 and error_count != -1):
                        wind_speed_limit = wind_speed_limit + error_count
                        wind_speed_total = wind_speed_total + total_count
                #Quality Flags
                    if (error_code == 70.0 and error_count != -1):
                        quality_level_flag_limit = quality_level_flag_limit \
                            + error_count
                        quality_level_total = quality_level_total + total_count
                    if (error_code == 65.0 and error_count != -1):
                        l2p_flag_limit = l2p_flag_limit + error_count
                        l2p_flag_total = l2p_flag_total + total_count

 #           if (inst == 'AVHRR12_G' or inst == 'AVHRR14_G' or \
 #                   inst == 'AVHRR15_G' or inst == 'AVHRR16_G' or \
 #                   inst == 'AVHRR17_G' or inst == 'AVHRR18_G' or \
 #                   inst == 'AVHRR18_G' or inst == 'AVHRRMTA_G'):
 #               if (count != 29):
 #                   print 'Not all tests were completed: ',filename
 #                   empty_file_count = empty_file_count+1
 #           if (inst == 'ATSR1' or inst == 'ATSR2' or inst == 'AATSR'):
 #               if (count != 32):
 #                   print 'Not all tests were completed: ',filename
 #                   empty_file_count = empty_file_count+1

    print 'SST Limit Failure: ',(SST_limit/sst_total)*100,'%'
    print 'SST Depth Limit Failure: ',(SST_depth_limit/sstd_total)*100,'%'
    print 'SST Difference Failure: ',(SST_diff/SST_diff_total)*100,'%'
    print 'SST Dtime Limit Failure: ',(SST_dtime_limit/SST_dtime_total)*100,'%'
    print 'Uncorrelated Uncertainty Limit Failure: ',\
        (uncorr_uncert_limit/uncorr_uncert_total)*100,'%'
    print 'Uncorrelated Uncertainty Masked for SST Pixel: ',\
        (uncorr_uncert_masked/uncorr_uncert_m_total)*100,'%'
    print 'No Uncorrelated Uncertainty Test against Masked SST: File Number: ',\
        uncorr_uncert_avail_fail 
    if (uncorr_uncert_avail_total != 0.0):
        print 'Uncorrelated Uncertainty Available where SST Masked: ',\
            (uncorr_uncert_avail/uncorr_uncert_avail_total)*100,'%'
    print 'Large Scale Uncertainty Limit Failure: ',\
        (large_scl_uncert_limit/large_scl_total)*100,'%'
    print 'Large Scale Uncertainty Masked for SST Pixel: ',\
        (large_scl_uncert_masked/large_scl_m_total)*100,'%'
    print 'No Large Scale Uncertainty Test against Masked SST: File Number: ',\
        large_scl_uncert_avail_fail
    if (large_scl_uncert_avail_total != 0.0):
        print 'Large Scale Uncertainty Available where SST Masked: ',\
            (large_scl_uncert_avail/large_scl_uncert_avail_total)*100,'%'
    print 'SSES Standard Deviation Limit Failure: ',(sses_std_limit/sses_std_total)*100,'%'
    print 'SSES Standard Deviation Masked for SST Pixel: ',\
        (sses_std_masked/sses_std_m_total)*100,'%'
    print 'No SSES Standard Deviation Test against Masked SST: File Number: ',\
        sses_std_avail_fail
    if (sses_std_avail_total != 0.0):
        print 'SSES Standard Deviation available where SST Masked: ',\
            (sses_std_avail/sses_std_avail_total)*100,'%'
    print 'Synoptically Correlated Uncertainty Limit Failure: ',\
        (synop_uncert_limit/synop_uncert_total)*100,'%'
    print 'Synoptically Correlated Uncertainty Masked for SST Pixel: ',\
        (synop_uncert_masked/synop_uncert_m_total)*100,'%'
    print 'No Synoptically Correlated Uncertainty Test against Masked SST:'+ \
        ' File Number: ',synop_uncert_avail_fail 
    if (synop_uncert_avail_total != 0.0):
        print 'Synoptically Correlated Uncertainty available where '+ \
            'SST Masked: ',(synop_uncert_avail/synop_uncert_avail_total)*100,'%'
    print 'SSES Bias Limit Failure: ',(sses_bias_limit/sses_bias_total)*100,'%'
    print 'SSES Bias Masked for SST Pixel: ',(sses_bias_masked/sses_bias_m_total)*100,'%'
    print 'No SSES Bias Test against Masked SST: File Number: ',\
        sses_bias_avail_fail
    if (sses_bias_avail_total != 0.0):
        print 'SSES Bias available where SST Masked:',\
            (sses_bias_avail/sses_bias_avail_total)*100 ,'%',sses_bias_avail,sses_bias_avail_total
    print 'Adjustment Uncertainty Limit Failure: ',(adjust_uncert_limit/adjust_uncert_total)*100,'%'
    print 'Adjustment Uncertainty Masked for SST Pixel: ',\
        (adjust_uncert_masked/adjust_uncert_m_total)*100,'%'
    print 'No Adjustment Uncertainty Test against Masked SST: File Number: ',\
        adjust_uncert_avail_fail 
    if (adjust_uncert_avail_total != 0.0):
        print 'Adjustment Uncertainty available where SST Masked: ',\
            (adjust_uncert_avail/adjust_uncert_avail_total)*100,'%'
    print 'SST Depth Uncert Limit Failure: ',(sst_depth_uncert_limit/sst_depth_uncert_total)*100,'%'
    print 'Latitude Limit Failure: ',(latitude_limit/lat_total)*100,'%'
    if (lat_bnds_total != 0.0):
        print 'Latitude Band Limit Failure: ',(latitude_bands_limit/lat_bnds_total)*100,\
            '%'
    print 'Longitude Limit Failure: ',(longitude_limit/lon_total)*100,'%'
    if (lon_bnds_total != 0.0):
        print 'Longitude Band Limit Failure: ',\
            (longitude_bands_limit/lon_bnds_total)*100,'%'
    print 'Time Limit Failure: ',(time_limit/time_total)*100,'%'
    if (time_bnds_total != 0.0):
        print 'Time Bands Limit Failure: ',(time_bands_limit/time_bnds_total)*100,'%'
    print 'Wind Speed Limit Failure: ',(wind_speed_limit/wind_speed_total)*100,'%'
    print 'Quality Level Flag Limit Failure: ',\
        (quality_level_flag_limit/quality_level_total)*100,'%'
    print 'L2P Flag Limit Failure: ',(l2p_flag_limit/l2p_flag_total)*100,'%'
 #   print empty_file_count,file_count
    print 'corrupt sst',sst_corrupt,file_count
    print 'corrupt sst_depth',sst_depth_corrupt,file_count
    print 'corrupt large_scale_uncert',large_uncert_corrupt,file_count,' field present',large_uncert
    print 'corrupt uncorrelated_uncert',uncorr_uncert_corrupt,file_count,' field present',uncorr_uncert
    print 'corrupt synoptic uncert',synop_uncert_corrupt,file_count,' field present',synop_uncert
    print 'corrupt adjustment_uncert',adjust_uncert_corrupt,file_count,' field present',adjust_uncert
    print 'file has no data',no_data_count,file_count
