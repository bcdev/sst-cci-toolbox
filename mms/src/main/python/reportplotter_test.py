__author__ = 'ralf'

import unittest

from reportplotter import ReportPlotter
from productverifier import ProductVerifier


class ReportPlotterTests(unittest.TestCase):
    def test_plot_report_AVHRR(self):
        plotter = ReportPlotter('l2p', 'AVHRR12_G', ProductVerifier.load_report('testdata/AVHRR-summary.json'))
        plotter.plot()

    def test_plot_report_AATSR(self):
        plotter = ReportPlotter('l2p', 'AATSR', ProductVerifier.load_report('testdata/AATSR-summary.json'))
        plotter.plot()


if __name__ == '__main__':
    unittest.main()
