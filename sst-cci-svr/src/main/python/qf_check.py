#!/usr/bin/env python
import sys, math
import numpy.ma as ma
from datetime import datetime
import traceback
import os
from netCDF4 import Dataset, num2date
import numpy as np
import optparse
__version__ = "0.1"

if __name__ == "__main__":
 
    usage = "usage: %prog [options] file"
    parser = optparse.OptionParser(version=__version__, usage=usage)
    (opts, args) = parser.parse_args()

    if len(args) != 1:
        parser.error("incorrect number of arguments")

    filename = args[0]

    ncdata = Dataset(filename,'r+',format='NETCDF4')

    skin_sst = ncdata.variables['sea_surface_temperature'][0,:,:]
    depth_sst = ncdata.variables['sea_surface_temperature_depth'][0,:,:]
    qual_flag = ncdata.variables['quality_level'][0,:,:]

    mask = skin_sst < 271.15
    mask &= qual_flag == 5
    qual_flag[mask] = 1

    mask2 = skin_sst == 273.15
    mask2 &= depth_sst == 273.15
    qual_flag[mask2] = 1

    ncdata.variables['quality_level'][0,:,:] = qual_flag

    ncdata.close()

    
