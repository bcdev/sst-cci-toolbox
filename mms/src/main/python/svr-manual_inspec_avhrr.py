#!/usr/bin/env python
import os
from netCDF4 import Dataset

import numpy as np
import matplotlib.pyplot as plt


files = ['20100701003842-ESACCI-L2P_GHRSST-SSTskin-AVHRRMTA_G-X1-v02.0-fvX1.0.nc']

root = '/Users/ralf/scratch/mms/examples/l2p/'

fignum = 1

for f in files:

    filename = os.path.join(root, f)
    print filename

    ncdata = Dataset(filename, 'r', format='NETCDF4')

    var = ncdata.variables['sea_surface_temperature']
    print var.shape
    sst = var[0, :, :]
    var = ncdata.variables['sea_surface_temperature_depth']
    sstd = var[0, :, :]
    var = ncdata.variables['sst_dtime']
    sst_dtime = var[0, :, :]
    var = ncdata.variables['sses_bias']
    sses_bias = var[0, :, :]  # should all be zeros
    var = ncdata.variables['sses_standard_deviation']
    sses_std = var[0, :, :]
    var = ncdata.variables['sst_depth_total_uncertainty']
    sstd_uncert = var[0, :, :]
    var = ncdata.variables['l2p_flags']
    l2p_flags = var[0, :, :]
    var = ncdata.variables['quality_level']
    qual_lev = var[0, :, :]
    var = ncdata.variables['wind_speed']
    wind_speed = var[0, :, :]
    var = ncdata.variables['large_scale_correlated_uncertainty']
    l_uncert = var[0, :, :]
    var = ncdata.variables['synoptically_correlated_uncertainty']
    s_uncert = var[0, :, :]
    var = ncdata.variables['uncorrelated_uncertainty']
    u_uncert = var[0, :, :]
    var = ncdata.variables['adjustment_uncertainty']
    a_uncert = var[0, :, :]

    # print np.min(var[0:2000,:]),np.max(var[0:2000,:])
    # print np.min(var[2000:4000,:]),np.max(var[2000:4000,:])
    #    print np.min(var[4000:6000,:]),np.max(var[4000:6000,:])
    #    print np.min(var[6000:8000,:]),np.max(var[6000:8000,:])
    #    print np.min(var[8000:10000,:]),np.max(var[8000:10000,:])
    #    if (var.shape[0] > 12000):
    #        print np.min(var[10000:12000,:]),np.max(var[10000:12000,:])
    #        print np.min(var[12000:,:]),np.max(var[12000:,:])
    #    else:
    #        print np.min(var[10000:,:]),np.max(var[10000:,:])

    print f

    valid_data = (sst >= 271.15) * (sst <= 323.15) * (sstd >= 271.15) * (sstd <= 323.15)

    uncert_total = np.sqrt(
        (l_uncert[valid_data] * l_uncert[valid_data]) + (s_uncert[valid_data] * s_uncert[valid_data]) + (
            u_uncert[valid_data] * u_uncert[valid_data]))

    uncert_total_depth = np.sqrt(
        (l_uncert[valid_data] * l_uncert[valid_data]) + (s_uncert[valid_data] * s_uncert[valid_data]) + (
            u_uncert[valid_data] * u_uncert[valid_data]) + (a_uncert[valid_data] * a_uncert[valid_data]))
    #    print np.min(sstd_uncert[valid_data]-uncert_total_depth)
    #    print np.max(sstd_uncert[valid_data]-uncert_total_depth)

    diff1 = sses_std[valid_data] - uncert_total
    diff2 = sstd_uncert[valid_data] - uncert_total_depth
    sses_std_ext = sses_std[valid_data]
    l_uncert_ext = l_uncert[valid_data]
    s_uncert_ext = s_uncert[valid_data]
    u_uncert_ext = u_uncert[valid_data]
    a_uncert_ext = a_uncert[valid_data]
    sstd_uncert_ext = sstd_uncert[valid_data]

    num1 = 0.0
    num2 = 0.0

    #    for x in range(len(diff1)):
    #        if (diff1[x] > 0.01 or diff1[x] < -0.01):
    # print sses_std_ext[x],l_uncert_ext[x],s_uncert_ext[x],u_uncert_ext[x],a_uncert_ext[x],diff1[x],'1'
    #            num1 = num1+1
    #        if (diff2[x] > 0.01 or diff2[x] < -0.01):
    # print sstd_uncert_ext[x],l_uncert_ext[x],s_uncert_ext[x],u_uncert_ext[x],a_uncert_ext[x],diff2[x],'2'
    #            num2 = num2+1

    #    print num1,num2,np.min(diff1),np.max(diff1),np.min(diff2),np.max(diff2),len(diff1)

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('Sea Surface Temperature')
    plt.subplot(171)
    cs = plt.imshow(sst[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(sst[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(sst[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(sst[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(sst[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(sst[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(sst[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(sst[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('Sea Surface Temperature Depth')
    plt.subplot(171)
    cs = plt.imshow(sstd[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(sstd[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(sstd[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(sstd[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(sstd[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(sstd[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(sstd[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(sstd[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('Sea Surface Temperature Delta Time')
    plt.subplot(171)
    cs = plt.imshow(sst_dtime[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(sst_dtime[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(sst_dtime[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(sst_dtime[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(sst_dtime[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(sst_dtime[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(sst_dtime[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(sst_dtime[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('SSES Bias')
    plt.subplot(171)
    cs = plt.imshow(sses_bias[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(sses_bias[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(sses_bias[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(sses_bias[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(sses_bias[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(sses_bias[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(sses_bias[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(sses_bias[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('SSES Standard Deviation')
    plt.subplot(171)
    cs = plt.imshow(sses_std[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(sses_std[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(sses_std[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(sses_std[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(sses_std[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(sses_std[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(sses_std[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(sses_std[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('Sea Surface Temperature Depth Uncertainty')
    plt.subplot(171)
    cs = plt.imshow(sstd_uncert[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(sstd_uncert[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(sstd_uncert[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(sstd_uncert[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(sstd_uncert[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(sstd_uncert[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(sstd_uncert[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(sstd_uncert[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('L2P Flags')
    plt.subplot(171)
    cs = plt.imshow(l2p_flags[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(l2p_flags[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(l2p_flags[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(l2p_flags[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(l2p_flags[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(l2p_flags[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(l2p_flags[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(l2p_flags[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('Quality Level')
    plt.subplot(171)
    cs = plt.imshow(qual_lev[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(qual_lev[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(qual_lev[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(qual_lev[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(qual_lev[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(qual_lev[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(qual_lev[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(qual_lev[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('Wind Speed')
    plt.subplot(171)
    cs = plt.imshow(wind_speed[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(wind_speed[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(wind_speed[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(wind_speed[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(wind_speed[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(wind_speed[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(wind_speed[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(wind_speed[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('Large Scale Uncorrelated Uncertainty')
    plt.subplot(171)
    cs = plt.imshow(l_uncert[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(l_uncert[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(l_uncert[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(l_uncert[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(l_uncert[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(l_uncert[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(l_uncert[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(l_uncert[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('Synoptically Correlated Uncertainty')
    plt.subplot(171)
    cs = plt.imshow(s_uncert[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(s_uncert[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(s_uncert[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(s_uncert[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(s_uncert[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(s_uncert[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(s_uncert[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(s_uncert[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('Uncorrelated Uncertainty')
    plt.subplot(171)
    cs = plt.imshow(u_uncert[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(u_uncert[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(u_uncert[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(u_uncert[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(u_uncert[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(u_uncert[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(u_uncert[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(u_uncert[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()

    fig = plt.figure(fignum, figsize=(20, 14))
    fig.suptitle('Adjustment Uncertainty')
    plt.subplot(171)
    cs = plt.imshow(a_uncert[0:2000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(172)
    cb = plt.imshow(a_uncert[2000:4000, :])
    cbar = plt.colorbar(cb)
    plt.subplot(173)
    cs = plt.imshow(a_uncert[4000:6000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(174)
    cs = plt.imshow(a_uncert[6000:8000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(175)
    cs = plt.imshow(a_uncert[8000:10000, :])
    cbar = plt.colorbar(cs)
    plt.subplot(176)
    if (var.shape[0] > 12000):
        cs = plt.imshow(a_uncert[10000:12000, :])
        cbar = plt.colorbar(cs)
        plt.subplot(177)
        cs = plt.imshow(a_uncert[12000:, :])
        cbar = plt.colorbar(cs)
    else:
        cs = plt.imshow(a_uncert[10000:, :])
        cbar = plt.colorbar(cs)
    plt.show()
