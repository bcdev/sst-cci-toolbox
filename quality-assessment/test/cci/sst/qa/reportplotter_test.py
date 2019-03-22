import os

__author__ = 'ralf'

import unittest

from cci.sst.qa.reportplotter import ReportPlotter
from cci.sst.qa.productverifier import ProductVerifier
from test.cci.sst.qa.test_data_utils import TestDataUtils


class ReportPlotterTests(unittest.TestCase):
    def test_plot_report_AVHRR(self):
        data_dir = TestDataUtils.get_test_data_dir()
        report_file = os.path.join(data_dir, "l2p-AVHRR16_G-summary.json")
        plotter = ReportPlotter('l2p', 'AVHRR16_G', ProductVerifier.load_report(report_file))
        plotter.plot()

    def test_plot_report_AATSR(self):
        data_dir = TestDataUtils.get_test_data_dir()
        report_file = os.path.join(data_dir, "l2p-AATSR-summary.json")
        plotter = ReportPlotter('l2p', 'AATSR', ProductVerifier.load_report(report_file))
        plotter.plot()

    def test_plot_report_L3U_v2_1(self):
        data_dir = TestDataUtils.get_test_data_dir()
        report_file = os.path.join(data_dir, "l3u_AVHRR17G_v21_summary.json")
        plotter = ReportPlotter('l3u', 'AVHRR17_G', ProductVerifier.load_report(report_file))
        plotter.plot()

    def test_extract_labels_plot_1_one_label(self):
        report = {"adjustment_uncertainty.count.total": 20602368.0,
                  "adjustment_uncertainty.count.valid": 2623244.0,
                  "adjustment_uncertainty.existence_check": 0.0,
                  "adjustment_uncertainty.valid_max_check": 0.0,
                  "adjustment_uncertainty.valid_min_check": 0.0}

        labels = ReportPlotter._extract_labels_plot_1(report)
        self.assertIsNotNone(labels)
        self.assertEqual(1, len(labels))
        self.assertEqual("Adjustment Unc Exists", labels[0])

    def test_extract_labels_plot_1_more_labels(self):
        report = {"adjustment_uncertainty.count.total": 20602368.0,
                  "adjustment_uncertainty.count.valid": 2623244.0,
                  "adjustment_uncertainty.existence_check": 0.0,
                  "aerosol_dynamic_indicator.existence_check": 0.0,
                  "aerosol_dynamic_indicator.valid_max_check": 0.0,
                  "large_scale_correlated_uncertainty.existence_check": 0.0,
                  "large_scale_correlated_uncertainty.mask_false_negative_check_0": 0.0,
                  "probability_clear.existence_check": 0.0,
                  "probability_clear.valid_max_check": 0.0,
                  "sensitivity.existence_check": 0.0,
                  "sensitivity.mask_false_negative_check_0": 0.0,
                  "sensitivity.mask_false_negative_check_1": 39420.0,
                  "sses_bias.existence_check": 0.0,
                  "sses_bias.mask_false_negative_check_0": 0.0,
                  "sses_bias.mask_false_negative_check_1": 39420.0,
                  "sses_bias.mask_false_negative_check_1_failed_for": "20050606101532-ESACCI-L2P_GHRSST-SSTskin-AATSR-CDR2.0-v02.0-fv01.0.nc",
                  "sst_dtime.count.total": 20602368.0,
                  "sst_dtime.count.valid": 20602368.0,
                  "sst_dtime.existence_check": 0.0}

        labels = ReportPlotter._extract_labels_plot_1(report)
        self.assertIsNotNone(labels)
        self.assertEqual(7, len(labels))
        self.assertEqual("Adjustment Unc Exists", labels[0])
        self.assertEqual("Aerosol Dyn Ind Exists", labels[1])
        self.assertEqual("Large Scale Unc Exists", labels[2])
        self.assertEqual("Probability Clear Exists", labels[3])
        self.assertEqual("SSES Bias Exists", labels[4])
        self.assertEqual("SST DTime Exists", labels[5])
        self.assertEqual("SST Sens Exists", labels[6])

    def test_extract_labels_plot_2_three_labels(self):
        report = {"uncertainty_random.mask_false_positive_check_3": 0.0,
                  "uncertainty_random.mask_false_positive_check_4": 0.0,
                  "uncertainty_random.mask_false_positive_check_5": 0.0,
                  "uncertainty_random.valid_max_check": 0.0,
                  "uncertainty_random.valid_min_check": 0.0,
                  "uncertainty_random_alt.count.total": 25920000.0,
                  "uncertainty_random_alt.count.valid": 152224.0,
                  "uncertainty_random_alt.existence_check": 0.0}

        labels = ReportPlotter._extract_labels_plot_2(report)
        self.assertIsNotNone(labels)
        self.assertEqual(3, len(labels))
        self.assertEqual("Random Unc Max", labels[0])
        self.assertEqual("Random Unc Min", labels[1])
        self.assertEqual("Unc Rand Mask P", labels[2])

    def test_extract_labels_plot_2_more_labels(self):
        report = {"sea_surface_temperature.valid_min_check": 0.0,
                  "sea_surface_temperature_depth.count.total": 51840000.0,
                  "sea_surface_temperature_depth.count.valid": 357279.0,
                  "sea_surface_temperature_depth.existence_check": 0.0,
                  "sea_surface_temperature_depth.mask_false_negative_check_0": 0.0,
                  "sea_surface_temperature_depth.mask_false_negative_check_1": 120.0,
                  "sea_surface_temperature_depth.mask_false_negative_check_1_failed_for": "20041108023827-ESACCI-L3U_GHRSST-SSTskin-AVHRR17_G-CDR2.1-v02.0-fv01.0.nc 20011114100024-ESACCI-L3U_GHRSST-SSTskin-ATSR2-CDR2.0-v02.0-fv01.0.nc",
                  "sea_surface_temperature_depth.mask_false_negative_check_2": 0.0,
                  "sea_surface_temperature_depth.mask_false_negative_check_3": 0.0,
                  "sea_surface_temperature_depth.mask_false_negative_check_4": 0.0,
                  "sea_surface_temperature_depth.mask_false_negative_check_5": 0.0,
                  "sea_surface_temperature_depth.mask_false_positive_check_0": 0.0,
                  "sea_surface_temperature_depth.mask_false_positive_check_1": 398.0,
                  "sea_surface_temperature_depth.mask_false_positive_check_1_failed_for": "20041108023827-ESACCI-L3U_GHRSST-SSTskin-AVHRR17_G-CDR2.1-v02.0-fv01.0.nc 20011114100024-ESACCI-L3U_GHRSST-SSTskin-ATSR2-CDR2.0-v02.0-fv01.0.nc",
                  "sea_surface_temperature_depth.mask_false_positive_check_2": 0.0,
                  "sea_surface_temperature_depth.mask_false_positive_check_3": 0.0,
                  "sea_surface_temperature_depth.mask_false_positive_check_4": 0.0,
                  "sea_surface_temperature_depth.mask_false_positive_check_5": 0.0,
                  "sea_surface_temperature_depth.valid_max_check": 0.0, }

        labels = ReportPlotter._extract_labels_plot_2(report)
        self.assertIsNotNone(labels)
        self.assertEqual(4, len(labels))
        self.assertEqual("SST Depth Max", labels[0])
        self.assertEqual("SST Min", labels[1])
        self.assertEqual("SST Depth Mask N", labels[2])
        self.assertEqual("SST Depth Mask P", labels[3])

    def tearDown(self):
        if os.path.isfile("l2p-AATSR-figure1.pdf"):
            os.remove("l2p-AATSR-figure1.pdf")

        if os.path.isfile("l2p-AATSR-figure2.pdf"):
            os.remove("l2p-AATSR-figure2.pdf")

        if os.path.isfile("l2p-AVHRR16_G-figure1.pdf"):
            os.remove("l2p-AVHRR16_G-figure1.pdf")

        if os.path.isfile("l2p-AVHRR16_G-figure2.pdf"):
            os.remove("l2p-AVHRR16_G-figure2.pdf")

        if os.path.isfile("l3u-AVHRR17_G-figure1.pdf"):
            os.remove("l3u-AVHRR17_G-figure1.pdf")

        if os.path.isfile("l3u-AVHRR17_G-figure2.pdf"):
            os.remove("l3u-AVHRR17_G-figure2.pdf")


if __name__ == '__main__':
    unittest.main()
