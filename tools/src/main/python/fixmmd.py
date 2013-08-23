__author__ = 'ralf'

from netCDF4 import Dataset
from netCDF4 import MFDataset

import glob
import os

mmd_path = "/Users/Ralf/tmp/archive/mmd/v1/2003"
nwp_path = "/Users/Ralf/tmp/archive/nwp/v1/2003"
month = "01"

# mapping from variable prefix in MMD files to filename prefix of NWP files
variable_prefix_to_filename_prefix_mapping = {
    "matchup.nwp.an": "nwpAn",
    "matchup.nwp.fc": "nwpFc",
    "atsr.1.nwp": "atsr.1",
    "atsr.2.nwp": "atsr.2",
    "atsr.3.nwp": "atsr.3",
    "avhrr.15.nwp": "avhrr.n15",
    "avhrr.16.nwp": "avhrr.n16",
    "avhrr.17.nwp": "avhrr.n17",
}

# mapping from variable prefixes in MMD files to prefixes in NWP files
variable_prefix_mapping = {
    "matchup.nwp.an": "matchup.nwp.an",
    "matchup.nwp.fc": "matchup.nwp.fc",
    "atsr.1.nwp": "atsr.1.nwp",
    "atsr.2.nwp": "atsr.2.nwp",
    "atsr.3.nwp": "atsr.3.nwp",
    "avhrr.15.nwp": "avhrr.n15.nwp",
    "avhrr.16.nwp": "avhrr.n16.nwp",
    "avhrr.17.nwp": "avhrr.n17.nwp",
}

# mapping from variable names in MMD files to names in NWP files
variable_name_mapping = {
    # for satellite sub-scenes and matchup NWP history
    "seaice_fraction": "CI",
    "snow_albedo": "ASN",
    "sea_surface_temperature": "SSTK",
    "total_column_water_vapour": "TCWV",
    "mean_sea_level_pressure": "MSL",
    "total_cloud_cover": "TCC",
    "10m_east_wind_component": "U10",
    "10m_north_wind_component": "V10",
    "2m_temperature": "T2",
    "2m_dew_point": "D2",
    "albedo": "AL",
    "skin_temperature": "SKT",
    "log_surface_pressure": "LNSP",
    "temperature_profile": "T",
    "water_vapour_profile": "Q",
    "ozone_profile": "O3",
    # for matchup NWP history only
    "sea_ice_fraction": "CI",
    "surface_sensible_heat_flux": "SSHF",
    "surface_latent_heat_flux": "SLHF",
    "boundary_layer_height": "BLH",
    "downward_surface_solar_radiation": "SSRD",
    "downward_surface_thermal_radiation": "STRD",
    "surface_solar_radiation": "SSR",
    "surface_thermal_radiation": "STR",
    "turbulent_stress_east": "EWSS",
    "turbulent_stress_north": "NSSS",
    "evaporation": "E",
    "total_precipitation": "TP",
}


def get_nwp_dataset(month, prefix):
    month_dir_path = os.path.join(nwp_path, month)

    file_paths = glob.glob(os.path.join(month_dir_path, prefix + "-*.nc"))
    if len(file_paths) == 0:
        return None

    return MFDataset(file_paths, aggdim="matchup")


def get_mmd_dataset(month):
    dataset_path = os.path.join(mmd_path, "mmd-2003-%s.nc" % month)
    #print(dataset_path)
    return Dataset(dataset_path)


def write_nwp_values(mmd_dataset, mmd_variable_name, mmd_values):
    # TODO - implement
    pass


def copy_variable_values(old_mmd_dataset, new_mmd_dataset, mmd_variable_name):
    # TODO - implement
    pass


def new_mmd_dataset(month, old_mmd_dataset):
    # TODO - implement
    return None


if __name__ == "__main__":
    old_mmd_dataset = get_mmd_dataset(month)
    new_mmd_dataset = new_mmd_dataset(month, old_mmd_dataset)
    mmd_matchup_ids = old_mmd_dataset.variables["matchup.id"][:]

    for mmd_variable_name in old_mmd_dataset.variables:
        pos = mmd_variable_name.rfind(".")
        if pos != -1:
            mmd_variable_prefix = mmd_variable_name[0:pos]
            mmd_variable_short_name = mmd_variable_name[pos + 1:]
            if mmd_variable_prefix in variable_prefix_to_filename_prefix_mapping:
                nwp_filename_prefix = variable_prefix_to_filename_prefix_mapping[mmd_variable_prefix]
                nwp_dataset = get_nwp_dataset(month, nwp_filename_prefix)
                if nwp_dataset is not None:
                    nwp_variable_name = variable_prefix_mapping[mmd_variable_prefix] + "." + variable_name_mapping[mmd_variable_short_name]
                    nwp_values = nwp_dataset.variables[nwp_variable_name][:]
                    mmd_values = old_mmd_dataset.variables[mmd_variable_name][:]
                    nwp_matchup_ids = nwp_dataset.variables["matchup.id"][:]

                    nwp_matchup_id_record_no_map = {matchup_id: i for i, matchup_id in enumerate(nwp_matchup_ids)}

                    for i, v in enumerate(mmd_values):
                        matchup_id = mmd_matchup_ids[i]
                        if matchup_id in nwp_matchup_id_record_no_map:
                            old_value = v
                            new_value = nwp_values[nwp_matchup_id_record_no_map[matchup_id]]
                            mmd_values[i] = new_value

                    write_nwp_values(new_mmd_dataset, mmd_variable_name, mmd_values)
                    print(mmd_variable_name)
                else:
                    copy_variable_values(old_mmd_dataset, new_mmd_dataset, mmd_variable_name)
            else:
                copy_variable_values(old_mmd_dataset, new_mmd_dataset, mmd_variable_name)













