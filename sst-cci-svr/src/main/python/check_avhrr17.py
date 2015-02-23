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

if __name__ == "__main__":

    filename = '/disk/scratch/local.2/cbulgin/SVR_DV/new/20100310221800-ESACCI-L2P_GHRSST-SSTskin-AVHRR17_G-LT-v02.0-fv01.0.nc'

    ncdata = Dataset(filename,'r',format='NETCDF4')
    sst_depth = ncdata.variables['sea_surface_temperature_depth'][0,:,:]
    sst = ncdata.variables['sea_surface_temperature'][0,:,:]
    flags = ncdata.variables['l2p_flags'][0,:,:]
    qual = ncdata.variables['quality_level'][0,:,:]
    lon = ncdata.variables['lon'][0,:,:]
    lat = ncdata.variables['lat'][0,:,:]
    ncdata.close()

    filename = '/disk/scratch/local.2/cbulgin/SVR_DV/ATS_AVG_3PAARC20100310_D_dD2m.nc'

    ncdata = Dataset(filename,'r',format='NETCDF4')
    sst_arc = ncdata.variables['sst_depth'][0,1,:,:]
    lats = ncdata.variables['lat'][:]
    lons = ncdata.variables['lon'][:]
    ncdata.close()

    lat_arc = np.zeros([1800,3600])
    lon_arc = np.zeros([1800,3600])

    for x in range(1800):
        lon_arc[x,:] = lons
    for x in range(3600):
        lat_arc[:,x] = lats       

    mask = sst_depth == 273.15 
    mask &= qual == 5

    print len(sst_depth[mask])
    print np.min(flags[mask]),np.max(flags[mask])
    
    subset_flags = flags[mask]
    
    flags_microwave = (subset_flags & 1)
    flags_land = (subset_flags & 2)
    flags_ice = (subset_flags & 4)
    flags_lake = (subset_flags & 8)
    flags_river = (subset_flags & 16)
    flags_spare = (subset_flags & 32)
    flags_views = (subset_flags & 64)
    flags_channels = (subset_flags & 128)

    mask = flags_microwave != 0
    print len(flags_microwave[mask]),' microwave'
    mask = flags_land != 0
    print len(flags_land[mask]),' land'
    mask = flags_ice != 0
    print len(flags_ice[mask]),' ice'
    mask = flags_lake != 0
    print len(flags_lake[mask]),' lake'
    mask = flags_river != 0
    print len(flags_river[mask]),' river'
    mask = flags_spare != 0
    print len(flags_spare[mask]),' spare'
    mask = flags_views != 0
    print len(flags_views[mask]),' views'
    mask = flags_channels != 0
    print len(flags_channels[mask]),' channels'

    tmp = lon[:,0]
    max_line = np.argmax(tmp)
    tmp = lon[:,408]
    min_line = np.argmin(tmp)

    lon_arr1 = lon[0:min_line-1,:]
    lat_arr1 = lat[0:min_line-1,:]
    sst_arr1 = sst_depth[0:min_line-1,:]
    qual_arr1 = qual[0:min_line-1,:]
    
    
    lon_arr2 = lon[max_line+1:lon.shape[0],:]
    lat_arr2 = lat[max_line+1:lon.shape[0],:]
    sst_arr2 = sst_depth[max_line+1:lon.shape[0],:]
    qual_arr2 = qual[max_line+1:lon.shape[0],:]

    fig = plt.figure()
    m = Basemap(projection='mill',lon_0=0)
    m.drawcoastlines()
    x,y = m(lon_arr1,lat_arr1)
    im1 = m.pcolormesh(x,y,qual_arr1,vmin=0,vmax=5)
    x,y = m(lon_arr2,lat_arr2)
    im2 = m.pcolormesh(x,y,qual_arr2,vmin=0,vmax=5)
    plt.colorbar(im2)

    print sst_arc
    print np.min(sst_arc),np.max(sst_arc)

#    x,y = m(lon_arc,lat_arc)
#    im1 = m.pcolormesh(x,y,sst_arc,vmin=260,vmax=300)
#    plt.colorbar(im1)
    plt.show()
