__author__ = 'ralf'

import doctest
import os

import svr_verify_SST_CCI_data




# Use ABSOLUTE directory paths!
# SOURCE_DIR=$1
# TARGET_DIR=$2
# TYPE=$3

# check directory exists..
# mkdir -p ${TARGET_DIR}

# loop over files in the directoy..
# for f in $(ls ${SOURCE_DIR}/*.nc)
#do
#    ${mms.python.exec} ${mms.home}/python/svr_verify_SST_CCI_data.py ${f} ${TYPE} ${TARGET_DIR}
#done

class Verifier:
    def __init__(self, source_dir_path, target_dir_path, satellite_type_name):
        """

        :type source_dir_path: str
        :type target_dir_path: str
        :type satellite_type_name: str
        """
        self.source_dir_path = source_dir_path
        self.target_dir_path = target_dir_path
        self.satellite_type_name = satellite_type_name

        doctest.testmod()
        svr_verify_SST_CCI_data.check_netCDF4_available()

    def verify(self):
        """This loop intends to traverses a directory for a single month of sensor data, where data for individual
        days are contained in sub-directories."""
        for dir_path, subdir_names, file_names in os.walk(self.source_dir_path):
            for file_name in file_names:
                if file_name.endswith('.nc'):
                    file_path = os.path.join(dir_path, file_name)
                    svr_verify_SST_CCI_data.verify_SST_CCI_data(file_path,
                                                                self.satellite_type_name,
                                                                self.target_dir_path)