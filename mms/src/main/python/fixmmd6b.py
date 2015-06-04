__author__ = 'Ralf Quast'

import os
import re
import sys

from netCDF4 import Dataset


def open_mmd_dataset(dataset_path):
    print("opening file " + dataset_path)
    return Dataset(dataset_path)


# noinspection PyShadowingNames
def copy_mmd_dataset_writeable(old_mmd_dataset):
    dataset_path = old_mmd_dataset.filepath().replace(".nc", "-corrected.nc4")
    print("creating file " + dataset_path)
    new_mmd_dataset = Dataset(dataset_path, mode="w")

    for dimension_name in old_mmd_dataset.dimensions:
        dimension = old_mmd_dataset.dimensions[dimension_name]
        new_mmd_dataset.createDimension(dimension_name, len(dimension))
    for variable_name in old_mmd_dataset.variables:
        print("  creating variable " + variable_name)
        old_variable = old_mmd_dataset.variables[variable_name]
        new_variable = new_mmd_dataset.createVariable(variable_name, old_variable.dtype,
                                                      dimensions=old_variable.dimensions, zlib=True)
        if variable_name.find("brightness_temperature") != -1:
            for attribute_name in old_variable.ncattrs():
                print("    creating attribute " + attribute_name)
                if attribute_name == "valid_min":
                    attribute_value = -26000
                elif attribute_name == "valid_max":
                    attribute_value = 28000
                elif attribute_name == "scale_factor":
                    attribute_value = 0.005
                elif attribute_name == "add_offset":
                    attribute_value = 180.0
                else:
                    attribute_value = old_variable.getncattr(attribute_name)
                new_variable.setncattr(attribute_name, attribute_value)
        else:
            for attribute_name in old_variable.ncattrs():
                print("    creating attribute " + attribute_name)
                attribute_value = old_variable.getncattr(attribute_name)
                new_variable.setncattr(attribute_name, attribute_value)
    for attribute_name in old_mmd_dataset.ncattrs():
        print("  creating global attribute " + attribute_name)
        attribute_value = old_mmd_dataset.getncattr(attribute_name)
        new_mmd_dataset.setncattr(attribute_name, attribute_value)

    return new_mmd_dataset


# noinspection PyShadowingNames
def copy_variable_values(old_mmd_dataset, new_mmd_dataset, mmd_variable_name):
    print("  copying variable " + mmd_variable_name)
    old_variable = old_mmd_dataset.variables[mmd_variable_name]
    new_variable = new_mmd_dataset.variables[mmd_variable_name]
    new_variable[:] = old_variable[:]


if __name__ == "__main__":
    root_dir_path = sys.argv[1]
    for root, dirnames, filenames in os.walk(root_dir_path):
        for filename in filenames:
            if re.match("amsr2-mmd6-.*\\.nc", filename):
                dataset_pathname = os.path.join(root, filename)
                old_mmd_dataset = open_mmd_dataset(dataset_pathname)
                new_mmd_dataset = copy_mmd_dataset_writeable(old_mmd_dataset)

                for mmd_variable_name in old_mmd_dataset.variables:
                    copy_variable_values(old_mmd_dataset, new_mmd_dataset, mmd_variable_name)

                new_mmd_dataset_path = new_mmd_dataset.filepath()
                new_mmd_dataset.close()
                old_mmd_dataset.close()
