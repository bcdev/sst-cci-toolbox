__author__ = 'ralf'

import unittest

from reportplotter import ReportPlotter
from productverifier import ProductVerifier


class ReportPlotterTests(unittest.TestCase):
    def test_plot_report(self):
        sensor = 'AVHRR12_G'
        summary_report = ProductVerifier.load_report('testdata/summary.json')
        usecase = 'l2p'
        plotter = ReportPlotter(sensor, usecase, summary_report)
        plotter.plot()