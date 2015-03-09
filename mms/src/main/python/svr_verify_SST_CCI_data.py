#!/usr/bin/env python
"""
This module contains a function that checks a given file contains valid values.
It expects to be called, with a filename. 
"""

import datetime
import errno
import netCDF4 as nc
import numpy as np
import os

# limits dictionary is structured as per..
# "name:[ID,lower,upper,[missing-data-value]]"
# The id is a number, in increments of 5, that is used for identifying output.
# The missing-data-value is optional; we only include it for variables that don't have it
# declared (typically coordinate dimensions).  Note it is an *assumed* missing data
# value, we don't have an easy way to check it in within the file, other than by hand.
AVHRR_var_limits = {'sea_surface_temperature': [00, 271.15, 323.15],
                    'sea_surface_temperature_depth': [05, 271.15, 323.15],
                    'sst_dtime': [10, -32768, 32768],
                    'sses_bias': [15, 0, 0],
                    'sses_standard_deviation': [20, 0, 2.54],
                    'sst_depth_total_uncertainty': [25, 0, 50],
                    'large_scale_correlated_uncertainty': [30, 0, 5],
                    'synoptically_correlated_uncertainty': [35, 0, 5],
                    'uncorrelated_uncertainty': [40, 0, 5],
                    'adjustment_uncertainty': [45, 0, 5],
                    'lat': [50, -90, 90, -1e+30],
                    'lon': [55, -180, 180, -1e+30],
                    'time': [60, 0, np.inf, -1e+30],
                    'l2p_flags': [65, 0, 255],
                    'quality_level': [70, 0, 5],
                    'wind_speed': [75, 0, 25.4]}

# And a 2nd dictionary, which is pretty similar..
ATSR_var_limits = AVHRR_var_limits.copy()
# .. but has a few extra elements..
ATSR_var_limits.update({'lat_bnds': [80, -90, 90],
                        'lon_bnds': [85, -180, 180],
                        'time_bnds': [90, 0, np.inf],
                        # ..and a slight correction..
                        'sses_standard_deviation': [20, 0, 2.5]})

# The following vars need to be checked against SST.
# For some we want the masks to exactly align (True), for others we just
# want to make sure there is data wherever there are SST points (False)..
sst_var_checks = {'sses_bias': False,
                  'sses_standard_deviation': False,
                  'large_scale_correlated_uncertainty': True,
                  'synoptically_correlated_uncertainty': True,
                  'uncorrelated_uncertainty': True}

######################
class MyException(Exception):
    """Base class for exceptions in this module."""
    pass


######################
class LogFile():
    """My tweaked version of file to open a file to write any error messages to"""
    # a flag for recording whether we have written a particular input filename to the output..
    input_filename = ''
    out_code = -1

    def __init__(self, file1):
        "When module is initialised it opens a file, and writes the current date/time to it."
        self.open(file1)

    def open(self, file1):
        self.input_filename = file1
        self.file1 = open(file1.strip('.nc') + '.txt', 'w')
        self.file1.write('Output from verify_SST_CCI_data.py\n')
        now = datetime.datetime.now()
        self.file1.write("Produced on " + now.strftime('%Y-%m-%d at %H:%M:%S') + '\n')
        self.file1.write('Processing: ' + file1 + '\n')

    def write_out(self, err_count, numel):
        """ Write output msg.  If first write then include the
        input-filename to the output file"""
        if numel == 0:
            percent = 0.
        else:
            percent = ( err_count * 1.0 / numel ) * 100
        self.file1.write(str(self.out_code).zfill(2) + "," + str(err_count).zfill(2) + \
                         "," + str(numel) + "," + str(percent) + '\n')

    def reset_fnameCheck(self):
        """Reset the flag... """
        self.first_write = True

    def close(self):
        """Close the file we are writing to."""
        self.file1.close()


######################
def check_netCDF4_available():
    import sys

    try:
        import netCDF4
    except:
        print "ERROR: netCDF4 could not be loaded!"
        print "   Likely that, if on Eddie, you have not yet done the following.."
        print "    module load python "
        print "   Exiting."
        sys.exit(1)


#####################
def load_var_and_check_limits(check_var, vars_in_file, limits_list, log):
    #
    print "loading and checking " + check_var
    #
    # grab the limits associated with var..
    limits = limits_list.get(check_var)
    log.out_code = limits[0]
    #
    # check file varlist has the variable we want..
    if vars_in_file.has_key(check_var):
        #
        # file has var, load it..
        data = vars_in_file[check_var]
        #
        # apply a mask, based on an assumed fill-value
        # (ie we think the data lacks a fill-value)
        a = data[:]
        #
        if not isinstance(a, np.ma.masked_array):
            if (len(limits) == 4):
                a = np.ma.masked_values(a, limits[3])
            else:
                a = np.ma.masked_array(a)
        #
        # Find nos of places, if any, where data exceeds the prescribed limits..
        tmp = np.ma.masked_inside(a, limits[1], limits[2])
        log.write_out(tmp.count(), tmp.size)
        #
        # remove the var from the list of variables to check..
        limits_list.pop(check_var)
        return a, True
    else:
        #
        # file does not have the var we want..
        #  ( missing variable is )
        log.write_out(0, 0)
        return None, False


######################
def mask_align_check(base_data, new_data, log, check_var, oneway=False):
    #
    # Count the nos of points in new_data that have a value where base_data doesn't.
    # (should be zero)
    count_diff = new_data[base_data.mask].count()
    # error == 'masked where SST has a value'
    log.out_code += 1
    log.write_out(count_diff, base_data.size)
    #
    # if interested in both ways..
    log.out_code += 1
    if oneway:
        # skip other check..
        log.write_out(-1, -1)
    else:
        # count places that base_data Has data where new-data doesn't
        # (ideally should be zero)
        count_diff = base_data[new_data.mask].count()
        #
        log.write_out(count_diff, base_data.size)


######################
def verify_SST_CCI_data(filename, satellite_type, output_dir):
    # Open an output file..
    log = LogFile(output_dir + '/' + os.path.basename(filename))

    # Check that the given filename exists..
    if not os.path.isfile(filename):
        log.close()
        raise IOError(errno.ENOENT, '', filename)

    # Check if we are looking for ATSR or AVHRR vars..
    if satellite_type == "AVHRR":
        limits_list = AVHRR_var_limits.copy()
    elif satellite_type == "ATSR":
        limits_list = ATSR_var_limits.copy()
    else:
        log.close()
        raise MyException("Supplied sat_type (" + str(satellite_type) + ") not recognised")

    # Open the input file..
    nc_id = nc.Dataset(filename, mode="r")
    vars_in_file = nc_id.variables

    # Start with SST, since so much else depends upon it...
    sst_skin, sst_skin_flag = load_var_and_check_limits('sea_surface_temperature', vars_in_file, limits_list, log)

    # Several variables need to be defined in a way to match the SST, so check them next..
    for check_var, oneway in sst_var_checks.iteritems():
        data, data_flag = load_var_and_check_limits(check_var, vars_in_file, limits_list, log)
        # if both were successful, then we can check they align..
        if data_flag and sst_skin_flag:
            mask_align_check(sst_skin, data, log, check_var, oneway=oneway)
        else:
            # Cannot do geo-physical check!
            log.out_code += 1
            log.write_out(-1, -1)
            log.out_code += 1
            log.write_out(-1, -1)

    # Do something similar for sst_depth and adjust-uncertainty..
    sst_depth, sst_depth_flag = load_var_and_check_limits('sea_surface_temperature_depth', vars_in_file, limits_list,
                                                          log)
    data, data_flag = load_var_and_check_limits('adjustment_uncertainty', vars_in_file, limits_list, log)
    # if both were successful, then we can check they align..
    if data_flag and sst_depth_flag:
        mask_align_check(sst_depth, data, log, check_var)
    else:
        # Cannot do geo-physical check!
        log.out_code += 1
        log.write_out(-1, -1)
        log.out_code += 1
        log.write_out(-1, -1)

    # Do a geophysical check on sst_skin and sst_depth..
    #  *  -5 < SSTskin-depth < +10
    # (Get an array of the places that lie outside of the limits but are Not masked)
    tmp = np.ma.masked_inside(sst_skin[:] - sst_depth[:], -5, 10)
    log.out_code = 99
    log.write_out(tmp.count(), tmp.size)

    # Loop over remaining variables..
    for check_var in limits_list.keys():
        load_var_and_check_limits(check_var, vars_in_file, limits_list, log)

    log.close()

##################################################################################
if __name__ == "__main__":
    #
    # Call with three arguments:
    #
    # 1 = filename
    # 2 = satellite type
    # 3 = output directory
    #
    import doctest
    import sys

    doctest.testmod()

    check_netCDF4_available()

    verify_SST_CCI_data(sys.argv[1], sys.argv[2], sys.argv[3])






