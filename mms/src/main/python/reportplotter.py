__author__ = 'Ralf Quast'

import matplotlib

matplotlib.rc('xtick', labelsize=9)
matplotlib.rc('ytick', labelsize=9)
matplotlib.use('PDF')

import matplotlib.pyplot as plt
import numpy
import pylab
import os

from productverifier import ProductVerifier


class ReportPlotter:
    def __init__(self, usecase, sensor, report, figure_dirpath='.'):
        """

        :type sensor: str
        :type usecase: str
        :type report: dict
        :type figure_dirpath: str
        """
        self.sensor = sensor
        self.usecase = usecase
        self.report = report
        self.figure_dirpath = figure_dirpath

    def get_sensor(self):
        """

        :rtype : str
        """
        return self.sensor

    def get_usecase(self):
        """

        :rtype : str
        """
        return self.usecase

    def get_report(self):
        """

        :rtype : dict
        """
        return self.report

    def get_figure_dirpath(self):
        """

        :rtype : str
        """
        return self.figure_dirpath

    def plot(self):
        self.plot_figure_1()
        self.plot_figure_2()

    def plot_figure_1(self):
        checks = {
            'Is File': 'source_pathname_check',
            'Filename': 'source_filename_check',
            'Can Open': 'product_can_be_opened_check',
            'SST Corrupt': 'corruptness_check',
            'Adjustment Unc Exists': 'adjustment_uncertainty.existence_check',
            'L2P Flags Exist': 'l2p_flags.existence_check',
            'Large Scale Unc Exists': 'large_scale_correlated_uncertainty.existence_check',
            'Lat Exists': 'lat.existence_check',
            'Lon Exists': 'lon.existence_check',
            'Quality Level Exists': 'quality_level.existence_check',
            'SST Exists': 'sea_surface_temperature.existence_check',
            'SST Depth Exists': 'sea_surface_temperature_depth.existence_check',
            'SSES Bias Exists': 'sses_bias.existence_check',
            'SSES St Dev Exists': 'sses_standard_deviation.existence_check',
            'SST Depth Unc Exists': 'sst_depth_total_uncertainty.existence_check',
            'SST DTime Exists': 'sst_dtime.existence_check',
            'Synoptic Unc Exists': 'synoptically_correlated_uncertainty.existence_check',
            'Time Exists': 'time.existence_check',
            'Uncorrelated Unc Exists': 'uncorrelated_uncertainty.existence_check',
            'Wind Speed Exists': 'wind_speed.existence_check',
        }
        check_labels = [
            'Is File',
            'Filename',
            'Can Open',
            'Lat Exists',
            'Lon Exists',
            'SST Exists',
            'Time Exists',
            'SST DTime Exists',
            'SSES Bias Exists',
            'SSES St Dev Exists',
            'Large Scale Unc Exists',
            'Adjustment Unc Exists',
            'Synoptic Unc Exists',
            'Uncorrelated Unc Exists',
            'SST Depth Exists',
            'SST Depth Unc Exists',
            'Wind Speed Exists',
            'L2P Flags Exist',
            'Quality Level Exists',
            'SST Corrupt',
        ]
        report = self.get_report()
        reference_counts = report['summary_report.count']
        plot_title = self.get_usecase().upper() + ' ' + self.get_sensor().replace('_', '-') + ' File Checks'
        plot_label = 'Failure Permillage (for ' + str(reference_counts) + ' files in total)'
        filename = self.get_usecase().lower() + '-' + self.get_sensor() + "-figure1.pdf"
        filepath = os.path.join(self.get_figure_dirpath(), filename)
        ReportPlotter.plot_report(report, checks, check_labels, reference_counts, plot_title, plot_label, filepath)

    def plot_figure_2(self):
        checks = {
            'Adjustment Unc Max': 'adjustment_uncertainty.valid_max_check',
            'Adjustment Unc Min': 'adjustment_uncertainty.valid_min_check',
            'L2P Flags Max': 'l2p_flags.valid_max_check',
            'L2P Flags Min': 'l2p_flags.valid_min_check',
            'Large Scale Unc Max': 'large_scale_correlated_uncertainty.valid_max_check',
            'Large Scale Unc Min': 'large_scale_correlated_uncertainty.valid_min_check',
            'Lat Max': 'lat.valid_max_check',
            'Lat Min': 'lat.valid_min_check',
            'Lon Max': 'lon.valid_max_check',
            'Lon Min': 'lon.valid_min_check',
            'Quality Level Max': 'quality_level.valid_max_check',
            'Quality Level Min': 'quality_level.valid_min_check',
            'SST Max': 'sea_surface_temperature.valid_max_check',
            'SST Min': 'sea_surface_temperature.valid_min_check',
            'SST Depth Max': 'sea_surface_temperature_depth.valid_max_check',
            'SST Depth Min': 'sea_surface_temperature_depth.valid_min_check',
            'SST Geophysical': 'geophysical_check',
            'SSES Bias Max': 'sses_bias.valid_max_check',
            'SSES Bias Min': 'sses_bias.valid_min_check',
            'SSES St Dev Max': 'sses_standard_deviation.valid_max_check',
            'SSES St Dev Min': 'sses_standard_deviation.valid_min_check',
            'SST Depth Unc Max': 'sst_depth_total_uncertainty.valid_max_check',
            'SST Depth Unc Min': 'sst_depth_total_uncertainty.valid_min_check',
            'SST DTime Max': 'sst_dtime.valid_max_check',
            'SST DTime Min': 'sst_dtime.valid_min_check',
            'Synoptic Unc Max': 'synoptically_correlated_uncertainty.valid_max_check',
            'Synoptic Unc Min': 'synoptically_correlated_uncertainty.valid_min_check',
            'Uncorrelated Unc Max': 'uncorrelated_uncertainty.valid_max_check',
            'Uncorrelated Unc Min': 'uncorrelated_uncertainty.valid_min_check',
            'Wind Speed Max': 'wind_speed.valid_max_check',
            'Wind Speed Min': 'wind_speed.valid_min_check',
            'Adjustment Unc Mask N': 'adjustment_uncertainty.mask_false_negative_check',
            'Adjustment Unc Mask P': 'adjustment_uncertainty.mask_false_positive_check',
            'Large Scale Unc Mask N': 'large_scale_correlated_uncertainty.mask_false_negative_check',
            'Large Scale Unc Mask P': 'large_scale_correlated_uncertainty.mask_false_positive_check',
            'Synoptic Unc Mask N': 'synoptically_correlated_uncertainty.mask_false_negative_check',
            'Synoptic Unc Mask P': 'synoptically_correlated_uncertainty.mask_false_positive_check',
            'Uncorrelated Unc Mask N': 'uncorrelated_uncertainty.mask_false_negative_check',
            'Uncorrelated Unc Mask P': 'uncorrelated_uncertainty.mask_false_positive_check',
            'SSES Bias Mask N': 'sses_bias.mask_false_negative_check',
            'SSES Bias Mask P': 'sses_bias.mask_false_positive_check',
            'SSES St Dev Mask N': 'sses_standard_deviation.mask_false_negative_check',
            'SSES St Dev Mask P': 'sses_standard_deviation.mask_false_positive_check',
            'SST Depth Unc Mask N': 'sst_depth_total_uncertainty.mask_false_negative_check',
            'SST Depth Unc Mask P': 'sst_depth_total_uncertainty.mask_false_positive_check',
        }
        check_labels = [
            'Lat Min',
            'Lat Max',
            'Lon Min',
            'Lon Max',
            'SST DTime Min',
            'SST DTime Max',
            'SST Min',
            'SST Max',
            'SST Geophysical',
            'SSES Bias Min',
            'SSES Bias Max',
            'SSES St Dev Min',
            'SSES St Dev Max',
            'Large Scale Unc Min',
            'Large Scale Unc Max',
            'Adjustment Unc Min',
            'Adjustment Unc Max',
            'Synoptic Unc Min',
            'Synoptic Unc Max',
            'Uncorrelated Unc Min',
            'Uncorrelated Unc Max',
            'SST Depth Min',
            'SST Depth Max',
            'SST Depth Unc Min',
            'SST Depth Unc Max',
            'Wind Speed Min',
            'Wind Speed Max',
            'L2P Flags Min',
            'L2P Flags Max',
            'Quality Level Min',
            'Quality Level Max',
            'SSES Bias Mask N',
            'SSES Bias Mask P',
            'SSES St Dev Mask N',
            'SSES St Dev Mask P',
            'Adjustment Unc Mask N',
            'Adjustment Unc Mask P',
            'Large Scale Unc Mask N',
            'Large Scale Unc Mask P',
            'Synoptic Unc Mask N',
            'Synoptic Unc Mask P',
            'Uncorrelated Unc Mask N',
            'Uncorrelated Unc Mask P',
            'SST Depth Unc Mask N',
            'SST Depth Unc Mask P',
        ]
        report = self.get_report()
        reference_counts = report['lat.count.total']
        plot_title = self.get_usecase().upper() + ' ' + self.get_sensor().replace('_', '-') + ' Pixel Checks'
        plot_label = 'Failure Permillage (for ' + str(reference_counts) + ' pixels in total)'
        filename = self.get_usecase().lower() + '-' + self.get_sensor() + "-figure2.pdf"
        filepath = os.path.join(self.get_figure_dirpath(), filename)
        ReportPlotter.plot_report(report, checks, check_labels, reference_counts, plot_title, plot_label, filepath)

    @staticmethod
    def plot_report(report, checks, check_labels, reference_counts, plot_title, plot_label, filepath):
        """

        :type report: dict
        :type checks: dict
        :type check_labels: list
        :type reference_counts: int
        :type plot_title: str
        :type plot_label: str
        :type filepath: str
        """
        tick_labels = []
        counts = []
        percentages = []

        font_label = {'size': 9}
        font_title = {'size': 12}

        for tick_label in reversed(check_labels):
            tick_labels.append(tick_label)
            check = checks[tick_label]
            count = report[check]
            counts.append(count)
            percentages.append(count / (0.001 * reference_counts))

        figure, vertical_axis_l = plt.subplots(figsize=(9.0, 6.0 / 20 * len(tick_labels)))
        plt.subplots_adjust(left=0.25, right=0.85)
        pos = numpy.arange(len(tick_labels)) + 0.5
        vertical_axis_l.barh(pos, percentages, color='r', align='center', height=0.5)
        pylab.yticks(pos, tick_labels)
        vertical_axis_l.set_title(plot_title, fontdict=font_title)

        vertical_axis_r = vertical_axis_l.twinx()
        vertical_axis_r.set_yticks(pos)
        vertical_axis_r.set_yticklabels(counts)
        vertical_axis_r.set_ylim(vertical_axis_l.get_ylim())
        vertical_axis_r.set_ylabel('Failure Counts', fontdict=font_label)
        vertical_axis_l.set_ylabel('Checks Conducted', fontdict=font_label)
        vertical_axis_l.set_xlabel(plot_label, fontdict=font_label)

        figure.savefig(filepath)


if __name__ == "__main__":
    # Call with up to two arguments:
    #
    # 1 = usecase
    # 2 = sensor
    # 3 = summary report pathname
    # 4 = figure directory path
    import sys

    argument_count = len(sys.argv)
    if argument_count == 4:
        plotter = ReportPlotter(sys.argv[1], sys.argv[2], ProductVerifier.load_report(sys.argv[3]), sys.argv[4])
    else:
        print 'usage:', sys.argv[0], '<usecase> <sensor> <summary report pathname> <figure dirpath>'
        sys.exit(1)

    # noinspection PyBroadException
    try:
        plotter.plot()
    except:
        sys.exit(1)

    sys.exit()
