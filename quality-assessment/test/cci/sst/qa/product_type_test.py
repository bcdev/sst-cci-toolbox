import unittest

from cci.sst.qa.product_type import L2P


class ProductTypeTest(unittest.TestCase):
    def test_get_variable_names_L2P_v1_2(self):
        product_type = L2P()

        # product_type.setVersion("1.2")
        # @todo 1 tb/tb continue here

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


if __name__ == '__main__':
    unittest.main()
