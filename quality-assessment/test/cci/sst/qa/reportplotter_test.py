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

    def tearDown(self):
        if os.path.isfile("l2p-AATSR-figure1.pdf"):
            os.remove("l2p-AATSR-figure1.pdf")

        if os.path.isfile("l2p-AATSR-figure2.pdf"):
            os.remove("l2p-AATSR-figure2.pdf")

        if os.path.isfile("l2p-AVHRR16_G-figure1.pdf"):
            os.remove("l2p-AVHRR16_G-figure1.pdf")

        if os.path.isfile("l2p-AVHRR16_G-figure2.pdf"):
            os.remove("l2p-AVHRR16_G-figure2.pdf")


if __name__ == '__main__':
    unittest.main()
