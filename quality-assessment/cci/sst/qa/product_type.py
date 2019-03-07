class ProductType:
    def __init__(self, variable_names, geophysical_check_spec, mask_consistency_check_specs, sst_variable_names):
        """

        :type variable_names: dict
        :type geophysical_check_spec: dict
        :type mask_consistency_check_specs: dict
        :type sst_variable_names: dict
        """
        self.variable_names = variable_names
        self.geophysical_check_spec = geophysical_check_spec
        self.mask_consistency_check_specs = mask_consistency_check_specs
        self.sst_variable_names = sst_variable_names
        self.version = None

    def get_variable_names(self):
        """

        :rtype : list
        """
        return self.variable_names[self.version]

    def get_geophysical_check_spec(self):
        """

        :rtype : list
        """
        return self.geophysical_check_spec[self.version]

    def get_mask_consistency_check_specs(self):
        """

        :rtype : list
        """
        return self.mask_consistency_check_specs[self.version]

    def get_sst_variable_names(self):
        """

        :rtype : list
        """
        return self.sst_variable_names[self.version]

    def set_version(self, version):
        self.version = version


class L2P(ProductType):
    l2_variable_names = {
        "1.2": ['adjustment_uncertainty', 'l2p_flags', 'large_scale_correlated_uncertainty', 'lat', 'lon', 'quality_level', 'sea_surface_temperature', 'sea_surface_temperature_depth', 'sses_bias',
                'sses_standard_deviation', 'sst_depth_total_uncertainty', 'sst_dtime', 'synoptically_correlated_uncertainty', 'time', 'uncorrelated_uncertainty', 'wind_speed'],
        "2.0": ["adjustment_uncertainty", "aerosol_dynamic_indicator", "l2p_flags", "large_scale_correlated_uncertainty", "lat", "lon", "probability_clear", "quality_level", "sea_surface_temperature",
                "sea_surface_temperature_depth", "sensitivity", "sses_bias", "sses_standard_deviation", "sst_depth_dtime", "sst_depth_total_uncertainty", "sst_dtime",
                "synoptically_correlated_uncertainty", "time", "uncorrelated_uncertainty", "wind_speed"],
        "2.1": ["adjustment_alt", "alt_sst_retrieval_type", "depth_adjustment", "empirical_adjustment", "l2p_flags", "lat", "lon", "quality_level", "sea_surface_temperature",
                "sea_surface_temperature_depth", "sea_surface_temperature_depth_anomaly", "sea_surface_temperature_depth_total_uncertainty", "sea_surface_temperature_retrieval_type",
                "sea_surface_temperature_total_uncertainty", "sses_bias", "sses_standard_deviation", "sst_depth_dtime", "sst_dtime", "sst_sensitivity", "time", "uncertainty_correlated",
                "uncertainty_correlated_alt", "uncertainty_correlated_time_and_depth_adjustment", "uncertainty_random", "uncertainty_random_alt", "uncertainty_systematic",
                "uncertainty_systematic_alt", "wind_speed"]}

    l2_geophysical_checks = {
        "1.2": ['sea_surface_temperature', 'sea_surface_temperature_depth', -5.0, 10.0],
        "2.0": ['sea_surface_temperature', 'sea_surface_temperature_depth', -5.0, 10.0],
        "2.1": ['sea_surface_temperature', 'sea_surface_temperature_depth', -5.0, 10.0]
    }

    l2_mask_checks = {"1.2": [['sea_surface_temperature', 'quality_level', 'quality_level', [0, 2, 3, 4, 5]],
                              ['sea_surface_temperature', 'sses_bias', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'sses_standard_deviation', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'large_scale_correlated_uncertainty', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'synoptically_correlated_uncertainty', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'uncorrelated_uncertainty', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'adjustment_uncertainty', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'sea_surface_temperature_depth', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature_depth', 'sst_depth_total_uncertainty', 'quality_level', range(0, 6)]],
                      "2.0": [['sea_surface_temperature', 'quality_level', 'quality_level', [0, 2, 3, 4, 5]],
                              ['sea_surface_temperature', 'sensitivity', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'sses_bias', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'sses_standard_deviation', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'large_scale_correlated_uncertainty', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'synoptically_correlated_uncertainty', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'uncorrelated_uncertainty', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'adjustment_uncertainty', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature', 'sea_surface_temperature_depth', 'quality_level', range(0, 6)],
                              ['sea_surface_temperature_depth', 'sst_depth_total_uncertainty', 'quality_level', range(0, 6)]]
                      }

    l2_sst_variable_names = {
        "1.2": ['sea_surface_temperature', 'sea_surface_temperature_depth'],
        "2.0": ['sea_surface_temperature', 'sea_surface_temperature_depth']
    }

    def __init__(self):
        ProductType.__init__(self, self.l2_variable_names, self.l2_geophysical_checks, self.l2_mask_checks, self.l2_sst_variable_names)


class L3U(ProductType):
    l3_variable_names = {
        "1.2": ['lat', 'lat_bnds', 'lon', 'lon_bnds', 'time', 'time_bnds', 'sst_dtime', 'sea_surface_temperature', 'sea_surface_temperature_depth', 'wind_speed', 'quality_level',
                'sses_standard_deviation', 'sst_depth_total_uncertainty', 'large_scale_correlated_uncertainty', 'synoptically_correlated_uncertainty', 'uncorrelated_uncertainty',
                'adjustment_uncertainty', 'l2p_flags', 'sses_bias', 'aerosol_dynamic_indicator', 'sensitivity'],
        "2.0": ['adjustment_uncertainty', 'aerosol_dynamic_indicator', 'l2p_flags', 'large_scale_correlated_uncertainty', 'lat', 'lat_bnds', 'lon', 'lon_bnds', 'quality_level',
                'sea_surface_temperature', 'sea_surface_temperature_depth', 'sensitivity', 'sses_bias', 'sses_standard_deviation', 'sst_depth_dtime', 'sst_depth_total_uncertainty',
                'sst_dtime', 'synoptically_correlated_uncertainty', 'time', 'time_bnds', 'uncorrelated_uncertainty', 'wind_speed']
    }

    l3_geophysical_checks = {
        "1.2": ['sea_surface_temperature', 'sea_surface_temperature_depth', -5.0, 10.0],
        "2.0": ['sea_surface_temperature', 'sea_surface_temperature_depth', -5.0, 10.0]
    }

    l3_mask_checks = {
        "1.2": [['sea_surface_temperature', 'quality_level', 'quality_level', [0, 2, 3, 4, 5]], ['sea_surface_temperature', 'sses_bias', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'sses_standard_deviation', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'large_scale_correlated_uncertainty', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'synoptically_correlated_uncertainty', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'uncorrelated_uncertainty', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'adjustment_uncertainty', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'sea_surface_temperature_depth', 'quality_level', range(0, 6)],
                ['sea_surface_temperature_depth', 'sst_depth_total_uncertainty', 'quality_level', range(0, 6)]],
        "2.0": [['sea_surface_temperature', 'quality_level', 'quality_level', [0, 2, 3, 4, 5]], ['sea_surface_temperature', 'sses_bias', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'sensitivity', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'sses_standard_deviation', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'large_scale_correlated_uncertainty', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'synoptically_correlated_uncertainty', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'uncorrelated_uncertainty', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'adjustment_uncertainty', 'quality_level', range(0, 6)],
                ['sea_surface_temperature', 'sea_surface_temperature_depth', 'quality_level', range(0, 6)],
                ['sea_surface_temperature_depth', 'sst_depth_total_uncertainty', 'quality_level', range(0, 6)]]
    }

    l3_sst_variable_names = {
        "1.2": ['sea_surface_temperature', 'sea_surface_temperature_depth'],
        "2.0": ['sea_surface_temperature', 'sea_surface_temperature_depth']
    }

    def __init__(self):
        ProductType.__init__(self, self.l3_variable_names, self.l3_geophysical_checks, self.l3_mask_checks, self.l3_sst_variable_names)


class L4(ProductType):
    l4_variable_names = {"1.2": ['lat', 'lat_bnds', 'lon', 'lon_bnds', 'time', 'time_bnds', 'analysed_sst', 'sea_ice_fraction', 'quality_level', 'analysis_error', 'sea_ice_fraction_error', 'mask', ]}

    l4_geophysical_checks = {"1.2": []}

    l4_mask_checks = {"1.2": [['analysed_sst', 'analysis_error', None], ['sea_ice_fraction', 'sea_ice_fraction_error', None]]}

    l4_sst_variable_names = {"1.2": ['analysed_sst']}

    def __init__(self):
        ProductType.__init__(self, self.l4_variable_names, self.l4_geophysical_checks, self.l4_mask_checks, self.l4_sst_variable_names)
