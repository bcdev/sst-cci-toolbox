import unittest

from cci.sst.qa.product_type import L2P
from cci.sst.qa.product_type import L3U
from cci.sst.qa.product_type import L4


class ProductTypeTest(unittest.TestCase):
    def test_get_variable_names_L2P_v1_2(self):
        product_type = L2P()

        product_type.set_version("1.2")

        variable_names = product_type.get_variable_names()
        self.assertEqual(16, len(variable_names))
        self.assertEqual("adjustment_uncertainty", variable_names[0])
        self.assertEqual("quality_level", variable_names[5])
        self.assertEqual("sst_depth_total_uncertainty", variable_names[10])
        self.assertEqual("wind_speed", variable_names[15])

        geophysical_spec = product_type.get_geophysical_check_spec()
        self.assertEqual(4, len(geophysical_spec))
        self.assertEqual("sea_surface_temperature_depth", geophysical_spec[1])

        mask_specs = product_type.get_mask_consistency_check_specs()
        self.assertEqual(9, len(mask_specs))
        mask_spec = mask_specs[2]
        self.assertEqual("sea_surface_temperature", mask_spec[0])
        self.assertEqual("sses_standard_deviation", mask_spec[1])
        self.assertEqual("quality_level", mask_spec[2])
        self.assertEqual(range(0, 6), mask_spec[3])

        sst_variable_names = product_type.get_sst_variable_names()
        self.assertEqual(2, len(sst_variable_names))
        self.assertEqual("sea_surface_temperature", sst_variable_names[0])
        self.assertEqual("sea_surface_temperature_depth", sst_variable_names[1])

    def test_get_variable_names_L2P_v2_0(self):
        product_type = L2P()

        product_type.set_version("2.0")

        variable_names = product_type.get_variable_names()
        self.assertEqual(20, len(variable_names))
        self.assertEqual("aerosol_dynamic_indicator", variable_names[1])
        self.assertEqual("probability_clear", variable_names[6])
        self.assertEqual("sses_bias", variable_names[11])
        self.assertEqual("synoptically_correlated_uncertainty", variable_names[16])

        geophysical_spec = product_type.get_geophysical_check_spec()
        self.assertEqual(4, len(geophysical_spec))
        self.assertEqual(-5.0, geophysical_spec[2])

        mask_specs = product_type.get_mask_consistency_check_specs()
        self.assertEqual(10, len(mask_specs))
        mask_spec = mask_specs[3]
        self.assertEqual("sea_surface_temperature", mask_spec[0])
        self.assertEqual("sses_standard_deviation", mask_spec[1])
        self.assertEqual("quality_level", mask_spec[2])
        self.assertEqual(range(0, 6), mask_spec[3])

        sst_variable_names = product_type.get_sst_variable_names()
        self.assertEqual(2, len(sst_variable_names))
        self.assertEqual("sea_surface_temperature", sst_variable_names[0])
        self.assertEqual("sea_surface_temperature_depth", sst_variable_names[1])

    def test_get_variable_names_L2P_v2_1(self):
        product_type = L2P()

        product_type.set_version("2.1")

        variable_names = product_type.get_variable_names()
        self.assertEqual(28, len(variable_names))
        self.assertEqual("depth_adjustment", variable_names[2])
        self.assertEqual("quality_level", variable_names[7])
        self.assertEqual("sea_surface_temperature_retrieval_type", variable_names[12])
        self.assertEqual("sst_dtime", variable_names[17])

        geophysical_spec = product_type.get_geophysical_check_spec()
        self.assertEqual(4, len(geophysical_spec))
        self.assertEqual(-5.0, geophysical_spec[2])

        mask_specs = product_type.get_mask_consistency_check_specs()
        self.assertEqual(18, len(mask_specs))
        mask_spec = mask_specs[3]
        self.assertEqual("alt_sst_retrieval_type", mask_spec[0])
        self.assertEqual("adjustment_alt", mask_spec[1])
        self.assertEqual("quality_level", mask_spec[2])
        self.assertEqual(range(0, 6), mask_spec[3])

        sst_variable_names = product_type.get_sst_variable_names()
        self.assertEqual(2, len(sst_variable_names))
        self.assertEqual("sea_surface_temperature", sst_variable_names[0])
        self.assertEqual("sea_surface_temperature_depth", sst_variable_names[1])

    def test_get_variable_names_L3U_v1_2(self):
        product_type = L3U()

        product_type.set_version("1.2")

        variable_names = product_type.get_variable_names()
        self.assertEqual(21, len(variable_names))
        self.assertEqual("lat_bnds", variable_names[1])
        self.assertEqual("sst_dtime", variable_names[6])
        self.assertEqual("sses_standard_deviation", variable_names[11])
        self.assertEqual("adjustment_uncertainty", variable_names[16])

        geophysical_spec = product_type.get_geophysical_check_spec()
        self.assertEqual(4, len(geophysical_spec))
        self.assertEqual(-5, geophysical_spec[2])

        mask_specs = product_type.get_mask_consistency_check_specs()
        self.assertEqual(9, len(mask_specs))
        mask_spec = mask_specs[3]
        self.assertEqual("sea_surface_temperature", mask_spec[0])
        self.assertEqual("large_scale_correlated_uncertainty", mask_spec[1])
        self.assertEqual("quality_level", mask_spec[2])
        self.assertEqual(range(0, 6), mask_spec[3])

        sst_variable_names = product_type.get_sst_variable_names()
        self.assertEqual(2, len(sst_variable_names))
        self.assertEqual("sea_surface_temperature", sst_variable_names[0])
        self.assertEqual("sea_surface_temperature_depth", sst_variable_names[1])

    def test_get_variable_names_L3U_v2_0(self):
        product_type = L3U()

        product_type.set_version("2.0")

        variable_names = product_type.get_variable_names()
        self.assertEqual(22, len(variable_names))
        self.assertEqual("l2p_flags", variable_names[2])
        self.assertEqual("lon_bnds", variable_names[7])
        self.assertEqual("sses_bias", variable_names[12])
        self.assertEqual("synoptically_correlated_uncertainty", variable_names[17])

        geophysical_spec = product_type.get_geophysical_check_spec()
        self.assertEqual(4, len(geophysical_spec))
        self.assertEqual(10, geophysical_spec[3])

        mask_specs = product_type.get_mask_consistency_check_specs()
        self.assertEqual(10, len(mask_specs))
        mask_spec = mask_specs[4]
        self.assertEqual("sea_surface_temperature", mask_spec[0])
        self.assertEqual("large_scale_correlated_uncertainty", mask_spec[1])
        self.assertEqual("quality_level", mask_spec[2])
        self.assertEqual(range(0, 6), mask_spec[3])

        sst_variable_names = product_type.get_sst_variable_names()
        self.assertEqual(2, len(sst_variable_names))
        self.assertEqual("sea_surface_temperature", sst_variable_names[0])
        self.assertEqual("sea_surface_temperature_depth", sst_variable_names[1])

    def test_get_variable_names_L3U_v2_1(self):
        product_type = L3U()

        product_type.set_version("2.1")

        variable_names = product_type.get_variable_names()
        self.assertEqual(31, len(variable_names))
        self.assertEqual("empirical_adjustment", variable_names[3])
        self.assertEqual("lon_bnds", variable_names[8])
        self.assertEqual("sea_surface_temperature_depth_total_uncertainty", variable_names[13])
        self.assertEqual("sst_depth_dtime", variable_names[18])

        geophysical_spec = product_type.get_geophysical_check_spec()
        self.assertEqual(4, len(geophysical_spec))
        self.assertEqual('sea_surface_temperature', geophysical_spec[0])

        mask_specs = product_type.get_mask_consistency_check_specs()
        self.assertEqual(18, len(mask_specs))
        mask_spec = mask_specs[5]
        self.assertEqual("sea_surface_temperature", mask_spec[0])
        self.assertEqual("sea_surface_temperature_depth", mask_spec[1])
        self.assertEqual("quality_level", mask_spec[2])
        self.assertEqual(range(0, 6), mask_spec[3])

        sst_variable_names = product_type.get_sst_variable_names()
        self.assertEqual(2, len(sst_variable_names))
        self.assertEqual("sea_surface_temperature", sst_variable_names[0])
        self.assertEqual("sea_surface_temperature_depth", sst_variable_names[1])

    def test_get_variable_names_L4_v1_2(self):
        product_type = L4()

        product_type.set_version("1.2")

        variable_names = product_type.get_variable_names()
        self.assertEqual(12, len(variable_names))
        self.assertEqual("lon", variable_names[2])
        self.assertEqual("sea_ice_fraction", variable_names[7])

        geophysical_spec = product_type.get_geophysical_check_spec()
        self.assertEqual(0, len(geophysical_spec))

        mask_specs = product_type.get_mask_consistency_check_specs()
        self.assertEqual(2, len(mask_specs))
        mask_spec = mask_specs[0]
        self.assertEqual("analysed_sst", mask_spec[0])
        self.assertEqual("analysis_error", mask_spec[1])
        self.assertEqual(None, mask_spec[2])

        sst_variable_names = product_type.get_sst_variable_names()
        self.assertEqual(1, len(sst_variable_names))
        self.assertEqual("analysed_sst", sst_variable_names[0])


if __name__ == '__main__':
    unittest.main()
