__author__ = 'ralf'

import unittest

from reportplotter import ReportPlotter


class ReportPlotterTests(unittest.TestCase):
    def test_plot_report(self):
        sensor = 'AATSR'
        report = {
            'summary_report.count': 54750,
            'source_pathname_check': 0,
            'source_filename_check': 0,
            'product_can_be_opened_check': 0,
            'corruptness_check': 795,
            'sea_surface_temperature.existence_check': 7
        }
        plotter = ReportPlotter(sensor, report)
        plotter.plot()