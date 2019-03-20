import unittest

from test.cci.sst.qa.test_data_utils import TestDataUtils
from cci.sst.qa.reportaccumulator import ReportAccumulator


class ReportAccumulatorTests(unittest.TestCase):

    def test_accumulate_l2p(self):
        data_dir = TestDataUtils.get_test_data_dir()

        accumulator = ReportAccumulator(data_dir, report_filename_pattern="report_l2p.*\\.json")

        accumulator.accumulate()

        self.assertEqual(9432346.0, accumulator.summary_report["not_ocean"])
        self.assertEqual(3048183.0, accumulator.summary_report["adjustment_uncertainty.count.valid"])
        self.assertEqual(38242248.0, accumulator.summary_report["lat.count.total"])
        self.assertEqual(38242248.0, accumulator.summary_report["lon.count.total"])
        self.assertEqual(38242248.0, accumulator.summary_report["sea_surface_temperature.count.total"])

    def test_accumulate_l3u(self):
        data_dir = TestDataUtils.get_test_data_dir()

        accumulator = ReportAccumulator(data_dir, report_filename_pattern="report_l3u.*\\.json")

        accumulator.accumulate()

        self.assertEqual(238.0, accumulator.summary_report["adjustment_alt.mask_false_negative_check_1"])
        self.assertEqual(0.0, accumulator.summary_report["depth_adjustment.mask_false_negative_check_5"])
        self.assertEqual(28800.0, accumulator.summary_report["lon_bnds.count.total"])
        self.assertEqual('20041108023827-ESACCI-L3U_GHRSST-SSTskin-AVHRR17_G-CDR2.1-v02.0-fv01.0.nc', accumulator.summary_report["sea_surface_temperature_depth_anomaly.mask_false_negative_check_1_failed_for"])
        self.assertEqual(0.0, accumulator.summary_report["sea_surface_temperature_total_uncertainty.mask_false_positive_check_0"])
        self.assertEqual(357535.0, accumulator.summary_report["sst_depth_dtime.count.valid"])
        self.assertEqual(25920000.0, accumulator.summary_report["uncertainty_random.count.total"])