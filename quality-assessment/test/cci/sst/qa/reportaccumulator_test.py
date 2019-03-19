import unittest

from test.cci.sst.qa.test_data_utils import TestDataUtils
from cci.sst.qa.reportaccumulator import ReportAccumulator


class ReportAccumulatorTests(unittest.TestCase):

    def test_accumulate(self):
        data_dir = TestDataUtils.get_test_data_dir()

        accumulator = ReportAccumulator(data_dir, report_filename_pattern="report_l2p.*\\.json")

        accumulator.accumulate()

        self.assertEqual(9432346.0, accumulator.summary_report["not_ocean"])
        self.assertEqual(3048183.0, accumulator.summary_report["adjustment_uncertainty.count.valid"])
        self.assertEqual(38242248.0, accumulator.summary_report["lat.count.total"])
        self.assertEqual(38242248.0, accumulator.summary_report["lon.count.total"])
        self.assertEqual(38242248.0, accumulator.summary_report["sea_surface_temperature.count.total"])

        