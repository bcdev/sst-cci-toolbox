__author__ = 'Ralf Quast'

import matplotlib

matplotlib.rc('xtick', labelsize=9)
matplotlib.rc('ytick', labelsize=9)
matplotlib.use('PDF')

import matplotlib.pyplot as plt
import pylab
import numpy as np


class ReportPlotter:
    def __init__(self, sensor, usecase, report):
        """

        :type sensor: str
        :type usecase: str
        :type report: dict
        """
        self.sensor = sensor
        self.usecase = usecase
        self.report = report

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

    def plot(self):
        self.plot_product_checks()
        self.plot_pixel_checks()

    def plot_product_checks(self):
        labels = [
            'Is File',
            'Filename Convention',
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
        label_dict = {
            'Is File': 'source_pathname_check',
            'Filename Convention': 'source_filename_check',
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
        reference_counts = self.get_report()['summary_report.count']
        self.plot_check_results(label_dict, labels, reference_counts, 'figure1.pdf')

    def plot_pixel_checks(self):
        labels = [
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
        label_dict = {
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
        reference_counts = self.get_report()['lat.count.total']
        self.plot_check_results(label_dict, labels, reference_counts, 'figure2.pdf')

    def plot_check_results(self, label_dictionary, label_list, reference_counts, filename):
        """

        :type label_list: list
        :type label_dictionary: dict
        :type reference_counts: int
        """
        labels = []
        counts = []
        percentages = []

        for label in reversed(label_list):
            labels.append(label)
            check = label_dictionary[label]
            check_result = self.get_report()[check]
            counts.append(check_result)
            percentages.append(check_result / (1.0 * reference_counts))

        figure, vertical_axis_l = plt.subplots(figsize=(10, 7.5))
        plt.subplots_adjust(left=0.18, right=0.88)
        pos = np.arange(len(labels)) + 0.5
        vertical_axis_l.barh(pos, percentages, color='r', align='center', height=0.5)
        pylab.yticks(pos, labels)
        title = self.get_usecase().upper() + ' ' + self.get_sensor().replace('_', '-')
        vertical_axis_l.set_title(title)

        vertical_axis_r = vertical_axis_l.twinx()
        vertical_axis_r.set_yticks(pos)
        vertical_axis_r.set_yticklabels(counts)
        vertical_axis_r.set_ylim(vertical_axis_l.get_ylim())
        vertical_axis_r.set_ylabel('Failure Counts')
        vertical_axis_l.set_xlabel('Failure Rate (for ' + str(reference_counts) + ' test cases in total)')

        figure.savefig(filename)
        #plt.show()
