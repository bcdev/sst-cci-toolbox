class ProductType:
    def __init__(self, variable_names, geophysical_check_spec, mask_consistency_check_specs, sst_variable_names):
        """

        :type variable_names: list
        :type geophysical_check_spec: list
        :type mask_consistency_check_specs: list
        :type sst_variable_names: list
        """
        self.variable_names = variable_names
        self.geophysical_check_spec = geophysical_check_spec
        self.mask_consistency_check_specs = mask_consistency_check_specs
        self.sst_variable_names = sst_variable_names

    def get_variable_names(self):
        """

        :rtype : list
        """
        return self.variable_names

    def get_geophysical_check_spec(self):
        """

        :rtype : list
        """
        return self.geophysical_check_spec

    def get_mask_consistency_check_specs(self):
        """

        :rtype : list
        """
        return self.mask_consistency_check_specs

    def get_sst_variable_names(self):
        """

        :rtype : list
        """
        return self.sst_variable_names

    def set_version(self, version):
        self.version = version


class L2P(ProductType):
    def __init__(self):
        ProductType.__init__(self,
                             [
                                 'adjustment_uncertainty',
                                 'l2p_flags',
                                 'large_scale_correlated_uncertainty',
                                 'lat',
                                 'lon',
                                 'quality_level',
                                 'sea_surface_temperature',
                                 'sea_surface_temperature_depth',
                                 'sses_bias',
                                 'sses_standard_deviation',
                                 'sst_depth_total_uncertainty',
                                 'sst_dtime',
                                 'synoptically_correlated_uncertainty',
                                 'time',
                                 'uncorrelated_uncertainty',
                                 'wind_speed',
                             ],
                             ['sea_surface_temperature', 'sea_surface_temperature_depth', -5.0, 10.0],
                             [
                                 ['sea_surface_temperature', 'quality_level', 'quality_level',
                                  [0, 2, 3, 4, 5]],
                                 ['sea_surface_temperature', 'sses_bias', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'sses_standard_deviation', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'large_scale_correlated_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'synoptically_correlated_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'uncorrelated_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'adjustment_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'sea_surface_temperature_depth', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature_depth', 'sst_depth_total_uncertainty', 'quality_level',
                                  range(0, 6)],
                             ],
                             [
                                 'sea_surface_temperature',
                                 'sea_surface_temperature_depth'
                             ])


class L3U(ProductType):
    def __init__(self):
        ProductType.__init__(self,
                             [
                                 'lat',
                                 'lat_bnds',
                                 'lon',
                                 'lon_bnds',
                                 'time',
                                 'time_bnds',
                                 'sst_dtime',
                                 'sea_surface_temperature',
                                 'sea_surface_temperature_depth',
                                 'wind_speed',
                                 'quality_level',
                                 'sses_standard_deviation',
                                 'sst_depth_total_uncertainty',
                                 'large_scale_correlated_uncertainty',
                                 'synoptically_correlated_uncertainty',
                                 'uncorrelated_uncertainty',
                                 'adjustment_uncertainty',
                                 'l2p_flags',
                                 'sses_bias',
                                 'aerosol_dynamic_indicator',
                                 'sensitivity'
                             ],
                             ['sea_surface_temperature', 'sea_surface_temperature_depth', -5.0, 10.0],
                             [
                                 ['sea_surface_temperature', 'quality_level', 'quality_level',
                                  [0, 2, 3, 4, 5]],
                                 ['sea_surface_temperature', 'sses_bias', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'sses_standard_deviation', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'large_scale_correlated_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'synoptically_correlated_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'uncorrelated_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'adjustment_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'sea_surface_temperature_depth', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature_depth', 'sst_depth_total_uncertainty', 'quality_level',
                                  range(0, 6)]
                             ],
                             [
                                 'sea_surface_temperature',
                                 'sea_surface_temperature_depth'
                             ])


class L4(ProductType):
    def __init__(self):
        ProductType.__init__(self,
                             [
                                 'lat',
                                 'lat_bnds',
                                 'lon',
                                 'lon_bnds',
                                 'time',
                                 'time_bnds',
                                 'analysed_sst',
                                 'sea_ice_fraction',
                                 'quality_level'
                                 'analysis_error',
                                 'sea_ice_fraction_error',
                                 'mask',
                             ], [],
                             [
                                 ['analysed_sst', 'analysis_error', None],
                                 ['sea_ice_fraction', 'sea_ice_fraction_error', None]
                             ],
                             [
                                 'analysed_sst'
                             ])
