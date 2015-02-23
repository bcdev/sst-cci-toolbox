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
import math as math

files=['19950615175345-ESACCI-L3U_GHRSST-SSTskin-ATSR2-LT-v02.0-fv01.0.nc',\
           '19961023141222-ESACCI-L3U_GHRSST-SSTskin-ATSR1-LT-v02.0-fv01.0.nc',\
           '20020725033615-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc',\
           '20030608041252-ESACCI-L3U_GHRSST-SSTskin-ATSR2-LT-v02.0-fv01.0.nc',\
           '20101122224238-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc',\
           '19910801083335-ESACCI-L3U_GHRSST-SSTskin-ATSR1-LT-v02.0-fv01.0.nc']

files = ['20120101120000-ESACCI-L3U_GHRSST-SSTskin-AVHRRMTA-DM-v02.0-fv01.0.nc']

root='/disk/scratch/local/cbulgin/SVR_manual_check'
root='/disk/scratch/local.2/cbulgin/SVR/CMS/'

fignum = 1

for f in files:

    filename = os.path.join(root,f)
    print filename

    ncdata = Dataset(filename,'r',format='NETCDF4')

    var = ncdata.variables['sea_surface_temperature']
    sst = var[0,:,:]
#    var = ncdata.variables['sea_surface_temperature_depth']
#    sstd = var[0,:,:]
    var = ncdata.variables['sst_dtime']
    sst_dtime = var[0,:,:]
    var = ncdata.variables['sses_bias']
    sses_bias = var[0,:,:] #should all be zeros
    var = ncdata.variables['sses_standard_deviation']
    sses_std = var[0,:,:]
#    var = ncdata.variables['sst_depth_total_uncertainty']
#    sstd_uncert = var[0,:,:]
    var = ncdata.variables['l2p_flags']
    l2p_flags = var[0,:,:]
    var = ncdata.variables['quality_level']
    qual_lev = var[0,:,:]
    var = ncdata.variables['wind_speed']
    wind_speed = var[0,:,:]
    var = ncdata.variables['large_scale_correlated_uncertainty']
    l_uncert = var[0,:,:]
    var = ncdata.variables['synoptically_correlated_uncertainty']
    s_uncert = var[0,:,:]
    var = ncdata.variables['uncorrelated_uncertainty']
    u_uncert = var[0,:,:]
#    var = ncdata.variables['adjustment_uncertainty']
#    a_uncert = var[0,:,:]

#    print np.min(var[0:2000,:]),np.max(var[0:2000,:])
#    print np.min(var[2000:4000,:]),np.max(var[2000:4000,:])
#    print np.min(var[4000:6000,:]),np.max(var[4000:6000,:])
#    print np.min(var[6000:8000,:]),np.max(var[6000:8000,:])
#    print np.min(var[8000:10000,:]),np.max(var[8000:10000,:])
#    if (var.shape[0] > 12000):
#        print np.min(var[10000:12000,:]),np.max(var[10000:12000,:])
#        print np.min(var[12000:,:]),np.max(var[12000:,:])
#    else:
#        print np.min(var[10000:,:]),np.max(var[10000:,:])

    print f

#    valid_data = (sst >= 271.15)*(sst <= 323.15)*(sstd >= 271.15)*(sstd <= 323.15)
    
#    uncert_total = np.sqrt((l_uncert[valid_data]*l_uncert[valid_data])+(s_uncert[valid_data]*s_uncert[valid_data])+(u_uncert[valid_data]*u_uncert[valid_data]))

#    uncert_total_depth = np.sqrt((l_uncert[valid_data]*l_uncert[valid_data])+(s_uncert[valid_data]*s_uncert[valid_data])+(u_uncert[valid_data]*u_uncert[valid_data])+(a_uncert[valid_data]*a_uncert[valid_data]))
        
#    diff1 = sses_std[valid_data]-uncert_total
#    diff2 = sstd_uncert[valid_data]-uncert_total_depth
#    sses_std_ext = sses_std[valid_data]
#    l_uncert_ext = l_uncert[valid_data]
#    s_uncert_ext = s_uncert[valid_data]
#    u_uncert_ext = u_uncert[valid_data]
#    a_uncert_ext = a_uncert[valid_data]
#    sstd_uncert_ext = sstd_uncert[valid_data]

#    num1 = 0.0
#    num2 = 0.0

#    for x in range(len(diff1)):
#        if (diff1[x] > 0.173 or diff1[x] < -0.173):
           # print sses_std_ext[x],l_uncert_ext[x],s_uncert_ext[x],u_uncert_ext[x],a_uncert_ext[x],diff1[x],'1'
#            num1 = num1+1
#        if (diff2[x] > 0.2 or diff2[x] < -0.2):
           # print sstd_uncert_ext[x],l_uncert_ext[x],s_uncert_ext[x],u_uncert_ext[x],a_uncert_ext[x],diff2[x],'2'
#            num2 = num2+1
            
#    print num1,num2,np.min(diff1),np.max(diff1),np.min(diff2),np.max(diff2),len(diff1)

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('Sea Surface Temperature')
    cs = plt.imshow(sst)
    cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('Sea Surface Temperature Depth')
    cs = plt.imshow(sstd)
    cbar = plt.colorbar(cs)
#    plt.show()

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('Sea Surface Temperature Delta Time')
    cs = plt.imshow(sst_dtime)
    cbar = plt.colorbar(cs)
#    plt.show()

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('SSES Bias')
    cs = plt.imshow(sses_bias)
    cbar = plt.colorbar(cs)
#    plt.show()

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('SSES Standard Deviation')
    cs = plt.imshow(sses_std)
    cbar = plt.colorbar(cs)
#    plt.show()

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('Sea Surface Temperature Depth Uncertainty')
    cs = plt.imshow(sstd_uncert)
    cbar = plt.colorbar(cs)
#    plt.show()

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('L2P Flags')
    cs = plt.imshow(l2p_flags)
    cbar = plt.colorbar(cs)
#    plt.show()

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('Quality Level')
    cs = plt.imshow(qual_lev)
    cbar = plt.colorbar(cs)
#    plt.show()

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('Wind Speed')
    cs = plt.imshow(wind_speed)
    cbar = plt.colorbar(cs)
#    plt.show()

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('Large Scale Uncorrelated Uncertainty')
    cs = plt.imshow(l_uncert)
    cbar = plt.colorbar(cs)
#    plt.show()

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('Synoptically Correlated Uncertainty')
    cs = plt.imshow(s_uncert)
    cbar = plt.colorbar(cs)
#    plt.show()

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('Uncorrelated Uncertainty')
    cs = plt.imshow(u_uncert)
    cbar = plt.colorbar(cs)
#    plt.show()

    fig = plt.figure(fignum,figsize=(20,14))
    fig.suptitle('Adjustment Uncertainty')
    cs = plt.imshow(a_uncert)
    cbar = plt.colorbar(cs)
#    plt.show()
