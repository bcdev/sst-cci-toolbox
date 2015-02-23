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
from netCDF4 import Dataset, num2date
from mpl_toolkits.basemap import Basemap
import numpy as np
import array
import optparse
__version__ = "0.1"

#files=['19950615175345-ESACCI-L3U_GHRSST-SSTskin-ATSR2-LT-v02.0-fv01.0.nc',\
#           '19961023141222-ESACCI-L3U_GHRSST-SSTskin-ATSR1-LT-v02.0-fv01.0.nc',\
#           '20020725033615-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc',\
#           '20030608041252-ESACCI-L3U_GHRSST-SSTskin-ATSR2-LT-v02.0-fv01.0.nc',\
#           '20101122224238-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc',\
#           '19910801083335-ESACCI-L3U_GHRSST-SSTskin-ATSR1-LT-v02.0-fv01.0.nc']

#files=['20020725033615-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc']
#files=['20101122224238-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc']


root1='/disk/scratch/local.2/cbulgin/SVR_DV'
#root='/disk/scratch/local/cbulgin/SVR_manual_check'

#files = os.listdir(root1)


hours1 = ['07','08','09','10','11','12']
hours2 = ['19','20','21','22','23','24']
mins = ['04','13','21','29','38','46','54','63','71','79','88','96']
save_directory = root1+'/jun_jul_aug_06/'

if __name__ == "__main__":
 
    usage = "usage: %prog [options] file"
    parser = optparse.OptionParser(version=__version__, usage=usage)
    (opts, args) = parser.parse_args()

    if len(args) != 1:
        parser.error("incorrect number of arguments")

    #   filename = os.path.join(root1,f)
    filename = args[0]
    print filename
    
    ncdata = Dataset(filename,'r',format='NETCDF4')

    var = ncdata.variables['sea_surface_temperature']
    sst = var[0,:,:]
    var = ncdata.variables['sea_surface_temperature_depth']
    sstd = var[0,:,:]
    var = ncdata.variables['sst_dtime']
    sst_dtime = var[0,:,:]
    var = ncdata.variables['time']
    time = var[0]
    var = ncdata.variables['lon']
    l = var[:]
    var = ncdata.variables['l2p_flags']
    l2p = var[0,:,:]

    ref_time_days = math.floor(float(time)/86400)
    ref_time = time - (ref_time_days*86400)

    utc_time = sst_dtime + ref_time 
    utc_time_hour = np.floor(utc_time/3600)
    utc_time_min = np.floor(utc_time - (utc_time_hour*3600))/60
    utc_time_min = np.around(utc_time_min,decimals=0) 

    utc_time = utc_time_hour + (utc_time_min/60)
    
    l = np.vstack((l,l,l,l,l,l,l,l,l,l))
    li = np.vstack((l,l,l,l,l,l,l,l,l,l))
    l = np.vstack((li,li,li,li,li,li,li,li,li,li))
    lons = np.vstack((l,l,l,li,li,li,li,li,li))

    local_time = utc_time + ((lons/360) * 24)

    local_time_less = (local_time < 0.0)
    local_time_more = (local_time > 24.0)

    local_time[local_time_less] = local_time[local_time_less] + 24
    local_time[local_time_more] = local_time[local_time_more] - 24


    valid_data_d2_1 = (local_time >= 7.00)*(local_time < 7.08)
    valid_data_d2_2 = (local_time >= 7.08)*(local_time < 7.17)
    valid_data_d2_3 = (local_time >= 7.17)*(local_time < 7.25)
    valid_data_d2_4 = (local_time >= 7.25)*(local_time < 7.33)
    valid_data_d2_5 = (local_time >= 7.33)*(local_time < 7.42)
    valid_data_d2_6 = (local_time >= 7.42)*(local_time < 7.50)
    valid_data_d2_7 = (local_time >= 7.50)*(local_time < 7.58)
    valid_data_d2_8 = (local_time >= 7.58)*(local_time < 7.67)
    valid_data_d2_9 = (local_time >= 7.67)*(local_time < 7.75)
    valid_data_d2_10 = (local_time >= 7.75)*(local_time < 7.83)
    valid_data_d2_11 = (local_time >= 7.83)*(local_time < 7.92)
    valid_data_d2_12 = (local_time >= 7.92)*(local_time < 8.00)
    valid_data_d2_13 = (local_time >= 8.00)*(local_time < 8.08)
    valid_data_d2_14 = (local_time >= 8.08)*(local_time < 8.17)
    valid_data_d2_15 = (local_time >= 8.17)*(local_time < 8.25)
    valid_data_d2_16 = (local_time >= 8.25)*(local_time < 8.33)
    valid_data_d2_17 = (local_time >= 8.33)*(local_time < 8.42)
    valid_data_d2_18 = (local_time >= 8.42)*(local_time < 8.50)
    valid_data_d2_19 = (local_time >= 8.50)*(local_time < 8.58)
    valid_data_d2_20 = (local_time >= 8.58)*(local_time < 8.67)
    valid_data_d2_21 = (local_time >= 8.67)*(local_time < 8.75)
    valid_data_d2_22 = (local_time >= 8.75)*(local_time < 8.83)
    valid_data_d2_23 = (local_time >= 8.83)*(local_time < 8.92)
    valid_data_d2_24 = (local_time >= 8.92)*(local_time < 9.00)
    valid_data_d2_25 = (local_time >= 9.00)*(local_time < 9.08)
    valid_data_d2_26 = (local_time >= 9.08)*(local_time < 9.17)
    valid_data_d2_27 = (local_time >= 9.17)*(local_time < 9.25)
    valid_data_d2_28 = (local_time >= 9.25)*(local_time < 9.33)
    valid_data_d2_29 = (local_time >= 9.33)*(local_time < 9.42)
    valid_data_d2_30 = (local_time >= 9.42)*(local_time < 9.50)
    valid_data_d2_31 = (local_time >= 9.50)*(local_time < 9.58)
    valid_data_d2_32 = (local_time >= 9.58)*(local_time < 9.67)
    valid_data_d2_33 = (local_time >= 9.67)*(local_time < 9.75)
    valid_data_d2_34 = (local_time >= 9.75)*(local_time < 9.83)
    valid_data_d2_35 = (local_time >= 9.83)*(local_time < 9.92)
    valid_data_d2_36 = (local_time >= 9.92)*(local_time < 10.00)
    valid_data_d2_37 = (local_time >= 10.00)*(local_time < 10.08)
    valid_data_d2_38 = (local_time >= 10.08)*(local_time < 10.17)
    valid_data_d2_39 = (local_time >= 10.17)*(local_time < 10.25)
    valid_data_d2_40 = (local_time >= 10.25)*(local_time < 10.33)
    valid_data_d2_41 = (local_time >= 10.33)*(local_time < 10.42)
    valid_data_d2_42 = (local_time >= 10.42)*(local_time < 10.50)
    valid_data_d2_43 = (local_time >= 10.50)*(local_time < 10.58)
    valid_data_d2_44 = (local_time >= 10.58)*(local_time < 10.67)
    valid_data_d2_45 = (local_time >= 10.67)*(local_time < 10.75)
    valid_data_d2_46 = (local_time >= 10.75)*(local_time < 10.83)
    valid_data_d2_47 = (local_time >= 10.83)*(local_time < 10.92)
    valid_data_d2_48 = (local_time >= 10.92)*(local_time < 11.00)
    valid_data_d2_49 = (local_time >= 11.00)*(local_time < 11.08)
    valid_data_d2_50 = (local_time >= 11.08)*(local_time < 11.17)
    valid_data_d2_51 = (local_time >= 11.17)*(local_time < 11.25)
    valid_data_d2_52 = (local_time >= 11.25)*(local_time < 11.33)
    valid_data_d2_53 = (local_time >= 11.33)*(local_time < 11.42)
    valid_data_d2_54 = (local_time >= 11.42)*(local_time < 11.50)
    valid_data_d2_55 = (local_time >= 11.50)*(local_time < 11.58)
    valid_data_d2_56 = (local_time >= 11.58)*(local_time < 11.67)
    valid_data_d2_57 = (local_time >= 11.67)*(local_time < 11.75)
    valid_data_d2_58 = (local_time >= 11.75)*(local_time < 11.83)
    valid_data_d2_59 = (local_time >= 11.83)*(local_time < 11.92)
    valid_data_d2_60 = (local_time >= 11.92)*(local_time < 12.00)
    valid_data_d2_61 = (local_time >= 12.00)*(local_time < 12.08)
    valid_data_d2_62 = (local_time >= 12.08)*(local_time < 12.17)
    valid_data_d2_63 = (local_time >= 12.17)*(local_time < 12.25)
    valid_data_d2_64 = (local_time >= 12.25)*(local_time < 12.33)
    valid_data_d2_65 = (local_time >= 12.33)*(local_time < 12.42)
    valid_data_d2_66 = (local_time >= 12.42)*(local_time < 12.50)
    valid_data_d2_67 = (local_time >= 12.50)*(local_time < 12.58)
    valid_data_d2_68 = (local_time >= 12.58)*(local_time < 12.67)
    valid_data_d2_69 = (local_time >= 12.67)*(local_time < 12.75)
    valid_data_d2_70 = (local_time >= 12.75)*(local_time < 12.83)
    valid_data_d2_71 = (local_time >= 12.83)*(local_time < 12.92)
    valid_data_d2_72 = (local_time >= 12.92)*(local_time < 13.00)

    valid_data_d3_1 = (local_time >= 19.00)*(local_time < 19.08)
    valid_data_d3_2 = (local_time >= 19.08)*(local_time < 19.17)
    valid_data_d3_3 = (local_time >= 19.17)*(local_time < 19.25)
    valid_data_d3_4 = (local_time >= 19.25)*(local_time < 19.33)
    valid_data_d3_5 = (local_time >= 19.33)*(local_time < 19.42)
    valid_data_d3_6 = (local_time >= 19.42)*(local_time < 19.50)
    valid_data_d3_7 = (local_time >= 19.50)*(local_time < 19.58)
    valid_data_d3_8 = (local_time >= 19.58)*(local_time < 19.67)
    valid_data_d3_9 = (local_time >= 19.67)*(local_time < 19.75)
    valid_data_d3_10 = (local_time >= 19.75)*(local_time < 19.83)
    valid_data_d3_11 = (local_time >= 19.83)*(local_time < 19.92)
    valid_data_d3_12 = (local_time >= 19.92)*(local_time < 20.00)
    valid_data_d3_13 = (local_time >= 20.00)*(local_time < 20.08)
    valid_data_d3_14 = (local_time >= 20.08)*(local_time < 20.17)
    valid_data_d3_15 = (local_time >= 20.17)*(local_time < 20.25)
    valid_data_d3_16 = (local_time >= 20.25)*(local_time < 20.33)
    valid_data_d3_17 = (local_time >= 20.33)*(local_time < 20.42)
    valid_data_d3_18 = (local_time >= 20.42)*(local_time < 20.50)
    valid_data_d3_19 = (local_time >= 20.50)*(local_time < 20.58)
    valid_data_d3_20 = (local_time >= 20.58)*(local_time < 20.67)
    valid_data_d3_21 = (local_time >= 20.67)*(local_time < 20.75)
    valid_data_d3_22 = (local_time >= 20.75)*(local_time < 20.83)
    valid_data_d3_23 = (local_time >= 20.83)*(local_time < 20.92)
    valid_data_d3_24 = (local_time >= 20.92)*(local_time < 21.00)
    valid_data_d3_25 = (local_time >= 21.00)*(local_time < 21.08)
    valid_data_d3_26 = (local_time >= 21.08)*(local_time < 21.17)
    valid_data_d3_27 = (local_time >= 21.17)*(local_time < 21.25)
    valid_data_d3_28 = (local_time >= 21.25)*(local_time < 21.33)
    valid_data_d3_29 = (local_time >= 21.33)*(local_time < 21.42)
    valid_data_d3_30 = (local_time >= 21.42)*(local_time < 21.50)
    valid_data_d3_31 = (local_time >= 21.50)*(local_time < 21.58)
    valid_data_d3_32 = (local_time >= 21.58)*(local_time < 21.67)
    valid_data_d3_33 = (local_time >= 21.67)*(local_time < 21.75)
    valid_data_d3_34 = (local_time >= 21.75)*(local_time < 21.83)
    valid_data_d3_35 = (local_time >= 21.83)*(local_time < 21.92)
    valid_data_d3_36 = (local_time >= 21.92)*(local_time < 22.00)
    valid_data_d3_37 = (local_time >= 22.00)*(local_time < 22.08)
    valid_data_d3_38 = (local_time >= 22.08)*(local_time < 22.17)
    valid_data_d3_39 = (local_time >= 22.17)*(local_time < 22.25)
    valid_data_d3_40 = (local_time >= 22.25)*(local_time < 22.33)
    valid_data_d3_41 = (local_time >= 22.33)*(local_time < 22.42)
    valid_data_d3_42 = (local_time >= 22.42)*(local_time < 22.50)
    valid_data_d3_43 = (local_time >= 22.50)*(local_time < 22.58)
    valid_data_d3_44 = (local_time >= 22.58)*(local_time < 22.67)
    valid_data_d3_45 = (local_time >= 22.67)*(local_time < 22.75)
    valid_data_d3_46 = (local_time >= 22.75)*(local_time < 22.83)
    valid_data_d3_47 = (local_time >= 22.83)*(local_time < 22.92)
    valid_data_d3_48 = (local_time >= 22.92)*(local_time < 23.00)
    valid_data_d3_49 = (local_time >= 23.00)*(local_time < 23.08)
    valid_data_d3_50 = (local_time >= 23.08)*(local_time < 23.17)
    valid_data_d3_51 = (local_time >= 23.17)*(local_time < 23.25)
    valid_data_d3_52 = (local_time >= 23.25)*(local_time < 23.33)
    valid_data_d3_53 = (local_time >= 23.33)*(local_time < 23.42)
    valid_data_d3_54 = (local_time >= 23.42)*(local_time < 23.50)
    valid_data_d3_55 = (local_time >= 23.50)*(local_time < 23.58)
    valid_data_d3_56 = (local_time >= 23.58)*(local_time < 23.67)
    valid_data_d3_57 = (local_time >= 23.67)*(local_time < 23.75)
    valid_data_d3_58 = (local_time >= 23.75)*(local_time < 23.83)
    valid_data_d3_59 = (local_time >= 23.83)*(local_time < 23.92)
    valid_data_d3_60 = (local_time >= 23.92)*(local_time <= 24.00)
    valid_data_d3_61 = (local_time > 00.00)*(local_time < 00.08)
    valid_data_d3_62 = (local_time >= 00.08)*(local_time < 00.17)
    valid_data_d3_63 = (local_time >= 00.17)*(local_time < 00.25)
    valid_data_d3_64 = (local_time >= 00.25)*(local_time < 00.33)
    valid_data_d3_65 = (local_time >= 00.33)*(local_time < 00.42)
    valid_data_d3_66 = (local_time >= 00.42)*(local_time < 00.50)
    valid_data_d3_67 = (local_time >= 00.50)*(local_time < 00.58)
    valid_data_d3_68 = (local_time >= 00.58)*(local_time < 00.67)
    valid_data_d3_69 = (local_time >= 00.67)*(local_time < 00.75)
    valid_data_d3_70 = (local_time >= 00.75)*(local_time < 00.83)
    valid_data_d3_71 = (local_time >= 00.83)*(local_time < 00.92)
    valid_data_d3_72 = (local_time >= 00.92)*(local_time < 01.00)

    sst_diff_d2_1 = sst[valid_data_d2_1] - sstd[valid_data_d2_1]
    sst_diff_d2_2 = sst[valid_data_d2_2] - sstd[valid_data_d2_2]
    sst_diff_d2_3 = sst[valid_data_d2_3] - sstd[valid_data_d2_3]
    sst_diff_d2_4 = sst[valid_data_d2_4] - sstd[valid_data_d2_4]
    sst_diff_d2_5 = sst[valid_data_d2_5] - sstd[valid_data_d2_5]
    sst_diff_d2_6 = sst[valid_data_d2_6] - sstd[valid_data_d2_6]
    sst_diff_d2_7 = sst[valid_data_d2_7] - sstd[valid_data_d2_7]
    sst_diff_d2_8 = sst[valid_data_d2_8] - sstd[valid_data_d2_8]
    sst_diff_d2_9 = sst[valid_data_d2_9] - sstd[valid_data_d2_9]
    sst_diff_d2_10 = sst[valid_data_d2_10] - sstd[valid_data_d2_10]
    sst_diff_d2_11 = sst[valid_data_d2_11] - sstd[valid_data_d2_11]
    sst_diff_d2_12 = sst[valid_data_d2_12] - sstd[valid_data_d2_12]
    sst_diff_d2_13 = sst[valid_data_d2_13] - sstd[valid_data_d2_13]
    sst_diff_d2_14 = sst[valid_data_d2_14] - sstd[valid_data_d2_14]
    sst_diff_d2_15 = sst[valid_data_d2_15] - sstd[valid_data_d2_15]
    sst_diff_d2_16 = sst[valid_data_d2_16] - sstd[valid_data_d2_16]
    sst_diff_d2_17 = sst[valid_data_d2_17] - sstd[valid_data_d2_17]
    sst_diff_d2_18 = sst[valid_data_d2_18] - sstd[valid_data_d2_18]
    sst_diff_d2_19 = sst[valid_data_d2_19] - sstd[valid_data_d2_19]
    sst_diff_d2_20 = sst[valid_data_d2_20] - sstd[valid_data_d2_20]
    sst_diff_d2_21 = sst[valid_data_d2_21] - sstd[valid_data_d2_21]
    sst_diff_d2_22 = sst[valid_data_d2_22] - sstd[valid_data_d2_22]
    sst_diff_d2_23 = sst[valid_data_d2_23] - sstd[valid_data_d2_23]
    sst_diff_d2_24 = sst[valid_data_d2_24] - sstd[valid_data_d2_24]
    sst_diff_d2_25 = sst[valid_data_d2_25] - sstd[valid_data_d2_25]
    sst_diff_d2_26 = sst[valid_data_d2_26] - sstd[valid_data_d2_26]
    sst_diff_d2_27 = sst[valid_data_d2_27] - sstd[valid_data_d2_27]
    sst_diff_d2_28 = sst[valid_data_d2_28] - sstd[valid_data_d2_28]
    sst_diff_d2_29 = sst[valid_data_d2_29] - sstd[valid_data_d2_29]
    sst_diff_d2_30 = sst[valid_data_d2_30] - sstd[valid_data_d2_30]
    sst_diff_d2_31 = sst[valid_data_d2_31] - sstd[valid_data_d2_31]
    sst_diff_d2_32 = sst[valid_data_d2_32] - sstd[valid_data_d2_32]
    sst_diff_d2_33 = sst[valid_data_d2_33] - sstd[valid_data_d2_33]
    sst_diff_d2_34 = sst[valid_data_d2_34] - sstd[valid_data_d2_34]
    sst_diff_d2_35 = sst[valid_data_d2_35] - sstd[valid_data_d2_35]
    sst_diff_d2_36 = sst[valid_data_d2_36] - sstd[valid_data_d2_36]
    sst_diff_d2_37 = sst[valid_data_d2_37] - sstd[valid_data_d2_37]
    sst_diff_d2_38 = sst[valid_data_d2_38] - sstd[valid_data_d2_38]
    sst_diff_d2_39 = sst[valid_data_d2_39] - sstd[valid_data_d2_39]
    sst_diff_d2_40 = sst[valid_data_d2_40] - sstd[valid_data_d2_40]
    sst_diff_d2_41 = sst[valid_data_d2_41] - sstd[valid_data_d2_41]
    sst_diff_d2_42 = sst[valid_data_d2_42] - sstd[valid_data_d2_42]
    sst_diff_d2_43 = sst[valid_data_d2_43] - sstd[valid_data_d2_43]
    sst_diff_d2_44 = sst[valid_data_d2_44] - sstd[valid_data_d2_44]
    sst_diff_d2_45 = sst[valid_data_d2_45] - sstd[valid_data_d2_45]
    sst_diff_d2_46 = sst[valid_data_d2_46] - sstd[valid_data_d2_46]
    sst_diff_d2_47 = sst[valid_data_d2_47] - sstd[valid_data_d2_47]
    sst_diff_d2_48 = sst[valid_data_d2_48] - sstd[valid_data_d2_48]
    sst_diff_d2_49 = sst[valid_data_d2_49] - sstd[valid_data_d2_49]
    sst_diff_d2_50 = sst[valid_data_d2_50] - sstd[valid_data_d2_50]
    sst_diff_d2_51 = sst[valid_data_d2_51] - sstd[valid_data_d2_51]
    sst_diff_d2_52 = sst[valid_data_d2_52] - sstd[valid_data_d2_52]
    sst_diff_d2_53 = sst[valid_data_d2_53] - sstd[valid_data_d2_53]
    sst_diff_d2_54 = sst[valid_data_d2_54] - sstd[valid_data_d2_54]
    sst_diff_d2_55 = sst[valid_data_d2_55] - sstd[valid_data_d2_55]
    sst_diff_d2_56 = sst[valid_data_d2_56] - sstd[valid_data_d2_56]
    sst_diff_d2_57 = sst[valid_data_d2_57] - sstd[valid_data_d2_57]
    sst_diff_d2_58 = sst[valid_data_d2_58] - sstd[valid_data_d2_58]
    sst_diff_d2_59 = sst[valid_data_d2_59] - sstd[valid_data_d2_59]
    sst_diff_d2_60 = sst[valid_data_d2_60] - sstd[valid_data_d2_60]
    sst_diff_d2_61 = sst[valid_data_d2_61] - sstd[valid_data_d2_61]
    sst_diff_d2_62 = sst[valid_data_d2_62] - sstd[valid_data_d2_62]
    sst_diff_d2_63 = sst[valid_data_d2_63] - sstd[valid_data_d2_63]
    sst_diff_d2_64 = sst[valid_data_d2_64] - sstd[valid_data_d2_64]
    sst_diff_d2_65 = sst[valid_data_d2_65] - sstd[valid_data_d2_65]
    sst_diff_d2_66 = sst[valid_data_d2_66] - sstd[valid_data_d2_66]
    sst_diff_d2_67 = sst[valid_data_d2_67] - sstd[valid_data_d2_67]
    sst_diff_d2_68 = sst[valid_data_d2_68] - sstd[valid_data_d2_68]
    sst_diff_d2_69 = sst[valid_data_d2_69] - sstd[valid_data_d2_69]
    sst_diff_d2_70 = sst[valid_data_d2_70] - sstd[valid_data_d2_70]
    sst_diff_d2_71 = sst[valid_data_d2_71] - sstd[valid_data_d2_71]
    sst_diff_d2_72 = sst[valid_data_d2_72] - sstd[valid_data_d2_72]


    sst_diff_d3_1 = sst[valid_data_d3_1] - sstd[valid_data_d3_1]
    sst_diff_d3_2 = sst[valid_data_d3_2] - sstd[valid_data_d3_2]
    sst_diff_d3_3 = sst[valid_data_d3_3] - sstd[valid_data_d3_3]
    sst_diff_d3_4 = sst[valid_data_d3_4] - sstd[valid_data_d3_4]
    sst_diff_d3_5 = sst[valid_data_d3_5] - sstd[valid_data_d3_5]
    sst_diff_d3_6 = sst[valid_data_d3_6] - sstd[valid_data_d3_6]
    sst_diff_d3_7 = sst[valid_data_d3_7] - sstd[valid_data_d3_7]
    sst_diff_d3_8 = sst[valid_data_d3_8] - sstd[valid_data_d3_8]
    sst_diff_d3_9 = sst[valid_data_d3_9] - sstd[valid_data_d3_9]
    sst_diff_d3_10 = sst[valid_data_d3_10] - sstd[valid_data_d3_10]
    sst_diff_d3_11 = sst[valid_data_d3_11] - sstd[valid_data_d3_11]
    sst_diff_d3_12 = sst[valid_data_d3_12] - sstd[valid_data_d3_12]
    sst_diff_d3_13 = sst[valid_data_d3_13] - sstd[valid_data_d3_13]
    sst_diff_d3_14 = sst[valid_data_d3_14] - sstd[valid_data_d3_14]
    sst_diff_d3_15 = sst[valid_data_d3_15] - sstd[valid_data_d3_15]
    sst_diff_d3_16 = sst[valid_data_d3_16] - sstd[valid_data_d3_16]
    sst_diff_d3_17 = sst[valid_data_d3_17] - sstd[valid_data_d3_17]
    sst_diff_d3_18 = sst[valid_data_d3_18] - sstd[valid_data_d3_18]
    sst_diff_d3_19 = sst[valid_data_d3_19] - sstd[valid_data_d3_19]
    sst_diff_d3_20 = sst[valid_data_d3_20] - sstd[valid_data_d3_20]
    sst_diff_d3_21 = sst[valid_data_d3_21] - sstd[valid_data_d3_21]
    sst_diff_d3_22 = sst[valid_data_d3_22] - sstd[valid_data_d3_22]
    sst_diff_d3_23 = sst[valid_data_d3_23] - sstd[valid_data_d3_23]
    sst_diff_d3_24 = sst[valid_data_d3_24] - sstd[valid_data_d3_24]
    sst_diff_d3_25 = sst[valid_data_d3_25] - sstd[valid_data_d3_25]
    sst_diff_d3_26 = sst[valid_data_d3_26] - sstd[valid_data_d3_26]
    sst_diff_d3_27 = sst[valid_data_d3_27] - sstd[valid_data_d3_27]
    sst_diff_d3_28 = sst[valid_data_d3_28] - sstd[valid_data_d3_28]
    sst_diff_d3_29 = sst[valid_data_d3_29] - sstd[valid_data_d3_29]
    sst_diff_d3_30 = sst[valid_data_d3_30] - sstd[valid_data_d3_30]
    sst_diff_d3_31 = sst[valid_data_d3_31] - sstd[valid_data_d3_31]
    sst_diff_d3_32 = sst[valid_data_d3_32] - sstd[valid_data_d3_32]
    sst_diff_d3_33 = sst[valid_data_d3_33] - sstd[valid_data_d3_33]
    sst_diff_d3_34 = sst[valid_data_d3_34] - sstd[valid_data_d3_34]
    sst_diff_d3_35 = sst[valid_data_d3_35] - sstd[valid_data_d3_35]
    sst_diff_d3_36 = sst[valid_data_d3_36] - sstd[valid_data_d3_36]
    sst_diff_d3_37 = sst[valid_data_d3_37] - sstd[valid_data_d3_37]
    sst_diff_d3_38 = sst[valid_data_d3_38] - sstd[valid_data_d3_38]
    sst_diff_d3_39 = sst[valid_data_d3_39] - sstd[valid_data_d3_39]
    sst_diff_d3_40 = sst[valid_data_d3_40] - sstd[valid_data_d3_40]
    sst_diff_d3_41 = sst[valid_data_d3_41] - sstd[valid_data_d3_41]
    sst_diff_d3_42 = sst[valid_data_d3_42] - sstd[valid_data_d3_42]
    sst_diff_d3_43 = sst[valid_data_d3_43] - sstd[valid_data_d3_43]
    sst_diff_d3_44 = sst[valid_data_d3_44] - sstd[valid_data_d3_44]
    sst_diff_d3_45 = sst[valid_data_d3_45] - sstd[valid_data_d3_45]
    sst_diff_d3_46 = sst[valid_data_d3_46] - sstd[valid_data_d3_46]
    sst_diff_d3_47 = sst[valid_data_d3_47] - sstd[valid_data_d3_47]
    sst_diff_d3_48 = sst[valid_data_d3_48] - sstd[valid_data_d3_48]
    sst_diff_d3_49 = sst[valid_data_d3_49] - sstd[valid_data_d3_49]
    sst_diff_d3_50 = sst[valid_data_d3_50] - sstd[valid_data_d3_50]
    sst_diff_d3_51 = sst[valid_data_d3_51] - sstd[valid_data_d3_51]
    sst_diff_d3_52 = sst[valid_data_d3_52] - sstd[valid_data_d3_52]
    sst_diff_d3_53 = sst[valid_data_d3_53] - sstd[valid_data_d3_53]
    sst_diff_d3_54 = sst[valid_data_d3_54] - sstd[valid_data_d3_54]
    sst_diff_d3_55 = sst[valid_data_d3_55] - sstd[valid_data_d3_55]
    sst_diff_d3_56 = sst[valid_data_d3_56] - sstd[valid_data_d3_56]
    sst_diff_d3_57 = sst[valid_data_d3_57] - sstd[valid_data_d3_57]
    sst_diff_d3_58 = sst[valid_data_d3_58] - sstd[valid_data_d3_58]
    sst_diff_d3_59 = sst[valid_data_d3_59] - sstd[valid_data_d3_59]
    sst_diff_d3_60 = sst[valid_data_d3_60] - sstd[valid_data_d3_60]
    sst_diff_d3_61 = sst[valid_data_d3_61] - sstd[valid_data_d3_61]
    sst_diff_d3_62 = sst[valid_data_d3_62] - sstd[valid_data_d3_62]
    sst_diff_d3_63 = sst[valid_data_d3_63] - sstd[valid_data_d3_63]
    sst_diff_d3_64 = sst[valid_data_d3_64] - sstd[valid_data_d3_64]
    sst_diff_d3_65 = sst[valid_data_d3_65] - sstd[valid_data_d3_65]
    sst_diff_d3_66 = sst[valid_data_d3_66] - sstd[valid_data_d3_66]
    sst_diff_d3_67 = sst[valid_data_d3_67] - sstd[valid_data_d3_67]
    sst_diff_d3_68 = sst[valid_data_d3_68] - sstd[valid_data_d3_68]
    sst_diff_d3_69 = sst[valid_data_d3_69] - sstd[valid_data_d3_69]
    sst_diff_d3_70 = sst[valid_data_d3_70] - sstd[valid_data_d3_70]
    sst_diff_d3_71 = sst[valid_data_d3_71] - sstd[valid_data_d3_71]
    sst_diff_d3_72 = sst[valid_data_d3_72] - sstd[valid_data_d3_72]

    d2 = (sst_diff_d2_1,sst_diff_d2_2,sst_diff_d2_3,sst_diff_d2_4,sst_diff_d2_5,sst_diff_d2_6,sst_diff_d2_7,sst_diff_d2_8,sst_diff_d2_9,sst_diff_d2_10,sst_diff_d2_11,sst_diff_d2_12,sst_diff_d2_13,sst_diff_d2_14,sst_diff_d2_15,sst_diff_d2_16,sst_diff_d2_17,sst_diff_d2_18,sst_diff_d2_19,sst_diff_d2_20,sst_diff_d2_21,sst_diff_d2_22,sst_diff_d2_23,sst_diff_d2_24,sst_diff_d2_25,sst_diff_d2_26,sst_diff_d2_27,sst_diff_d2_28,sst_diff_d2_29,sst_diff_d2_30,sst_diff_d2_31,sst_diff_d2_32,sst_diff_d2_33,sst_diff_d2_34,sst_diff_d2_35,sst_diff_d2_36,sst_diff_d2_37,sst_diff_d2_38,sst_diff_d2_39,sst_diff_d2_40,sst_diff_d2_41,sst_diff_d2_42,sst_diff_d2_43,sst_diff_d2_44,sst_diff_d2_45,sst_diff_d2_46,sst_diff_d2_47,sst_diff_d2_48,sst_diff_d2_49,sst_diff_d2_50,sst_diff_d2_51,sst_diff_d2_52,sst_diff_d2_53,sst_diff_d2_54,sst_diff_d2_55,sst_diff_d2_56,sst_diff_d2_57,sst_diff_d2_58,sst_diff_d2_59,sst_diff_d2_60,sst_diff_d2_61,sst_diff_d2_62,sst_diff_d2_63,sst_diff_d2_64,sst_diff_d2_65,sst_diff_d2_66,sst_diff_d2_67,sst_diff_d2_68,sst_diff_d2_69,sst_diff_d2_70,sst_diff_d2_71,sst_diff_d2_72)

    d3 = (sst_diff_d3_1,sst_diff_d3_2,sst_diff_d3_3,sst_diff_d3_4,sst_diff_d3_5,sst_diff_d3_6,sst_diff_d3_7,sst_diff_d3_8,sst_diff_d3_9,sst_diff_d3_10,sst_diff_d3_11,sst_diff_d3_12,sst_diff_d3_13,sst_diff_d3_14,sst_diff_d3_15,sst_diff_d3_16,sst_diff_d3_17,sst_diff_d3_18,sst_diff_d3_19,sst_diff_d3_20,sst_diff_d3_21,sst_diff_d3_22,sst_diff_d3_23,sst_diff_d3_24,sst_diff_d3_25,sst_diff_d3_26,sst_diff_d3_27,sst_diff_d3_28,sst_diff_d3_29,sst_diff_d3_30,sst_diff_d3_31,sst_diff_d3_32,sst_diff_d3_33,sst_diff_d3_34,sst_diff_d3_35,sst_diff_d3_36,sst_diff_d3_37,sst_diff_d3_38,sst_diff_d3_39,sst_diff_d3_40,sst_diff_d3_41,sst_diff_d3_42,sst_diff_d3_43,sst_diff_d3_44,sst_diff_d3_45,sst_diff_d3_46,sst_diff_d3_47,sst_diff_d3_48,sst_diff_d3_49,sst_diff_d3_50,sst_diff_d3_51,sst_diff_d3_52,sst_diff_d3_53,sst_diff_d3_54,sst_diff_d3_55,sst_diff_d3_56,sst_diff_d3_57,sst_diff_d3_58,sst_diff_d3_59,sst_diff_d3_60,sst_diff_d3_61,sst_diff_d3_62,sst_diff_d3_63,sst_diff_d3_64,sst_diff_d3_65,sst_diff_d3_66,sst_diff_d3_67,sst_diff_d3_68,sst_diff_d3_69,sst_diff_d3_70,sst_diff_d3_71,sst_diff_d3_72)

  

    array_count = 0

    for h in range(len(hours1)):
        for m in range(len(mins)):               

            fh = open(save_directory+hours1[h]+mins[m]+'_d2.dat','a+b')
            info = array.array('f',d2[array_count])
            info.write(fh)
            fh.close()
            
            fh = open(save_directory+hours2[h]+mins[m]+'_d3.dat','a+b')
            info = array.array('f',d3[array_count])
            info.write(fh)
            fh.close()

            array_count = array_count+1

#    f = open(outfile_d2,'rb')
#    g = array.array('f')
    #
#    f.seek(0,2)
#    x = f.tell()
#    f.seek(0)
#    g.read(f,(x/4))
    #
#    d2 = np.array(g)

#    print g
#    print d2

#    f.close()

  
#cor_d2_1 = np.sum(diff_d2_1)/diff_d2_1.size
#cor_d2_2 = np.sum(diff_d2_2)/diff_d2_2.size
#cor_d2_3 = np.sum(diff_d2_3)/diff_d2_3.size
#cor_d2_4 = np.sum(diff_d2_4)/diff_d2_4.size
#cor_d2_5 = np.sum(diff_d2_5)/diff_d2_5.size
#cor_d2_6 = np.sum(diff_d2_6)/diff_d2_6.size
#cor_d2_7 = np.sum(diff_d2_7)/diff_d2_7.size
#cor_d2_8 = np.sum(diff_d2_8)/diff_d2_8.size
#cor_d2_9 = np.sum(diff_d2_9)/diff_d2_9.size
#cor_d2_10 = np.sum(diff_d2_10)/diff_d2_10.size
#cor_d2_11 = np.sum(diff_d2_11)/diff_d2_11.size
#cor_d2_12 = np.sum(diff_d2_12)/diff_d2_12.size
#cor_d2_13 = np.sum(diff_d2_13)/diff_d2_13.size
#cor_d2_14 = np.sum(diff_d2_14)/diff_d2_14.size
#cor_d2_15 = np.sum(diff_d2_15)/diff_d2_15.size
#cor_d2_16 = np.sum(diff_d2_16)/diff_d2_16.size
#cor_d2_17 = np.sum(diff_d2_17)/diff_d2_17.size
#cor_d2_18 = np.sum(diff_d2_18)/diff_d2_18.size
#cor_d2_19 = np.sum(diff_d2_19)/diff_d2_19.size
#cor_d2_20 = np.sum(diff_d2_20)/diff_d2_20.size
#cor_d2_21 = np.sum(diff_d2_21)/diff_d2_21.size
#cor_d2_22 = np.sum(diff_d2_22)/diff_d2_22.size
#cor_d2_23 = np.sum(diff_d2_23)/diff_d2_23.size
#cor_d2_24 = np.sum(diff_d2_24)/diff_d2_24.size
#cor_d2_25 = np.sum(diff_d2_25)/diff_d2_25.size
#cor_d2_26 = np.sum(diff_d2_26)/diff_d2_26.size
#cor_d2_27 = np.sum(diff_d2_27)/diff_d2_27.size
#cor_d2_28 = np.sum(diff_d2_28)/diff_d2_28.size
#cor_d2_29 = np.sum(diff_d2_29)/diff_d2_29.size
#cor_d2_30 = np.sum(diff_d2_30)/diff_d2_30.size
#cor_d2_31 = np.sum(diff_d2_31)/diff_d2_31.size
#cor_d2_32 = np.sum(diff_d2_32)/diff_d2_32.size
#cor_d2_33 = np.sum(diff_d2_33)/diff_d2_33.size
#cor_d2_34 = np.sum(diff_d2_34)/diff_d2_34.size
#cor_d2_35 = np.sum(diff_d2_35)/diff_d2_35.size
#cor_d2_36 = np.sum(diff_d2_36)/diff_d2_36.size
#cor_d2_37 = np.sum(diff_d2_37)/diff_d2_37.size
#cor_d2_38 = np.sum(diff_d2_38)/diff_d2_38.size
#cor_d2_39 = np.sum(diff_d2_39)/diff_d2_39.size
#cor_d2_40 = np.sum(diff_d2_40)/diff_d2_40.size
#cor_d2_41 = np.sum(diff_d2_41)/diff_d2_41.size
#cor_d2_42 = np.sum(diff_d2_42)/diff_d2_42.size
#cor_d2_43 = np.sum(diff_d2_43)/diff_d2_43.size
#cor_d2_44 = np.sum(diff_d2_44)/diff_d2_44.size
#cor_d2_45 = np.sum(diff_d2_45)/diff_d2_45.size
#cor_d2_46 = np.sum(diff_d2_46)/diff_d2_46.size
#cor_d2_47 = np.sum(diff_d2_47)/diff_d2_47.size
#cor_d2_48 = np.sum(diff_d2_48)/diff_d2_48.size
#cor_d2_49 = np.sum(diff_d2_49)/diff_d2_49.size
#cor_d2_50 = np.sum(diff_d2_50)/diff_d2_50.size
#cor_d2_51 = np.sum(diff_d2_51)/diff_d2_51.size
#cor_d2_52 = np.sum(diff_d2_52)/diff_d2_52.size
#cor_d2_53 = np.sum(diff_d2_53)/diff_d2_53.size
#cor_d2_54 = np.sum(diff_d2_54)/diff_d2_54.size
#cor_d2_55 = np.sum(diff_d2_55)/diff_d2_55.size
#cor_d2_56 = np.sum(diff_d2_56)/diff_d2_56.size
#cor_d2_57 = np.sum(diff_d2_57)/diff_d2_57.size
#cor_d2_58 = np.sum(diff_d2_58)/diff_d2_58.size
#cor_d2_59 = np.sum(diff_d2_59)/diff_d2_59.size
#cor_d2_60 = np.sum(diff_d2_60)/diff_d2_60.size
#cor_d2_61 = np.sum(diff_d2_61)/diff_d2_61.size
#cor_d2_62 = np.sum(diff_d2_62)/diff_d2_62.size
#cor_d2_63 = np.sum(diff_d2_63)/diff_d2_63.size
#cor_d2_64 = np.sum(diff_d2_64)/diff_d2_64.size
#cor_d2_65 = np.sum(diff_d2_65)/diff_d2_65.size
#cor_d2_66 = np.sum(diff_d2_66)/diff_d2_66.size
#cor_d2_67 = np.sum(diff_d2_67)/diff_d2_67.size
#cor_d2_68 = np.sum(diff_d2_68)/diff_d2_68.size
#cor_d2_69 = np.sum(diff_d2_69)/diff_d2_69.size
#cor_d2_70 = np.sum(diff_d2_70)/diff_d2_70.size
#cor_d2_71 = np.sum(diff_d2_71)/diff_d2_71.size
#cor_d2_72 = np.sum(diff_d2_72)/diff_d2_72.size

#d2 = [cor_d2_1,cor_d2_2,cor_d2_3,cor_d2_4,cor_d2_5,cor_d2_6,cor_d2_7,cor_d2_8,cor_d2_9,cor_d2_10,cor_d2_11,cor_d2_12,cor_d2_13,cor_d2_14,cor_d2_15,cor_d2_16,cor_d2_17,cor_d2_18,cor_d2_19,cor_d2_20,cor_d2_21,cor_d2_22,cor_d2_23,cor_d2_24,cor_d2_25,cor_d2_26,cor_d2_27,cor_d2_28,cor_d2_29,cor_d2_30,cor_d2_31,cor_d2_32,cor_d2_33,cor_d2_34,cor_d2_35,cor_d2_36,cor_d2_37,cor_d2_38,cor_d2_39,cor_d2_40,cor_d2_41,cor_d2_42,cor_d2_43,cor_d2_44,cor_d2_45,cor_d2_46,cor_d2_47,cor_d2_48,cor_d2_49,cor_d2_50,cor_d2_51,cor_d2_52,cor_d2_53,cor_d2_54,cor_d2_55,cor_d2_56,cor_d2_57,cor_d2_58,cor_d2_59,cor_d2_60,cor_d2_61,cor_d2_62,cor_d2_63,cor_d2_64,cor_d2_65,cor_d2_66,cor_d2_67,cor_d2_68,cor_d2_69,cor_d2_70,cor_d2_71,cor_d2_72]

#nums_d2 = [diff_d2_1.size,diff_d2_2.size,diff_d2_3.size,diff_d2_4.size,diff_d2_5.size,diff_d2_6.size,diff_d2_7.size,diff_d2_8.size,diff_d2_9.size,diff_d2_10.size,diff_d2_11.size,diff_d2_12.size,diff_d2_13.size,diff_d2_14.size,diff_d2_15.size,diff_d2_16.size,diff_d2_17.size,diff_d2_18.size,diff_d2_19.size,diff_d2_20.size,diff_d2_21.size,diff_d2_22.size,diff_d2_23.size,diff_d2_24.size,diff_d2_25.size,diff_d2_26.size,diff_d2_27.size,diff_d2_28.size,diff_d2_29.size,diff_d2_30.size,diff_d2_31.size,diff_d2_32.size,diff_d2_33.size,diff_d2_34.size,diff_d2_35.size,diff_d2_36.size,diff_d3_37.size,diff_d2_38.size,diff_d2_39.size,diff_d2_40.size,diff_d2_41.size,diff_d2_42.size,diff_d2_43.size,diff_d2_44.size,diff_d2_45.size,diff_d2_46.size,diff_d2_47.size,diff_d2_48.size,diff_d2_49.size,diff_d2_50.size,diff_d2_51.size,diff_d2_52.size,diff_d2_53.size,diff_d2_54.size,diff_d2_55.size,diff_d2_56.size,diff_d2_57.size,diff_d2_58.size,diff_d2_59.size,diff_d2_60.size,diff_d2_61.size,diff_d2_62.size,diff_d2_63.size,diff_d2_64.size,diff_d2_65.size,diff_d2_66.size,diff_d2_67.size,diff_d2_68.size,diff_d2_69.size,diff_d2_70.size,diff_d2_71.size,diff_d2_72.size]

#times_d2=[7.04,7.13,7.21,7.29,7.38,7.46,7.54,7.63,7.71,7.79,7.88,7.96,8.04,8.13,8.21,8.29,8.38,8.46,8.54,8.63,8.71,8.79,8.88,8.96,9.04,9.13,9.21,9.29,9.38,9.46,9.54,9.63,9.71,9.79,9.88,9.96,10.04,10.13,10.21,10.29,10.38,10.46,10.54,10.63,10.71,10.79,10.88,10.96,11.04,11.13,11.21,11.29,11.38,11.46,11.54,11.63,11.71,11.79,11.88,11.96,12.04,12.13,12.21,12.29,12.38,12.46,12.54,12.63,12.71,12.79,12.88,12.96]

  #  fignum=1
  #  fig = plt.figure(fignum,figsize=(20,14))
  #  plt.scatter(local_time_d2,sst_diff_d2,c='b')
  #  plt.scatter(local_time_d3,sst_diff_d3,c='r')
  #  plt.show()

#A = np.array([times_d2,np.ones(len(times_d2))]).T
#m2, c2 = np.linalg.lstsq(A,d2)[0]


#fignum=1
#fig = plt.figure(fignum,figsize=(20,14))
#ax1 = fig.add_subplot(111)
#ax1.set_ylim([-0.5,0.2])
#ax1.plot(times_d2,d2,'o')
#ax1.set_ylabel('SST - SST Depth')
#ax1.set_xlabel('Local Time')
#ax1.legend(['SST - SST Depth'],loc=2)
#ax1.plot(times_d2, m2*times_d2 + c2, 'b-')
#ax2 = ax1.twinx()
#ax2.plot(times_d2,nums_d2,'ro')
#ax2.set_ylabel('Number of Observations')
#ax2.legend(['Number of Obs'],loc=1)
#plt.show()
