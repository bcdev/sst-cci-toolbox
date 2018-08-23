import unittest

from test.cci.sst.qa.test_data_utils import TestDataUtils
from cci.sst.qa.reportaccumulator import ReportAccumulator


class ReportAccumulatorTests(unittest.TestCase):

    def test_accumulate(self):
        data_dir = TestDataUtils.get_test_data_dir()

        accumulator = ReportAccumulator(data_dir, report_filename_pattern="report.*\\.json")

        accumulator.accumulate()

        self.assertEqual(31874970.0, accumulator.summary_report["not_ocean"])
        self.assertEqual(3162407.0, accumulator.summary_report["adjustment_uncertainty.count.valid"])
        self.assertEqual(25530328.0, accumulator.summary_report["lat.count.total"])
        self.assertEqual(51446728.0, accumulator.summary_report["sea_surface_temperature.count.total"])

        