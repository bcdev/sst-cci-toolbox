__author__ = 'ralf'

import unittest

from reportplotter import ReportPlotter
from productverifier import ProductVerifier


class ReportPlotterTests(unittest.TestCase):
    def test_plot_report(self):
        plotter = ReportPlotter('l2p', 'AVHRR12_G', ProductVerifier.load_report('testdata/summary.json'))
        plotter.plot()