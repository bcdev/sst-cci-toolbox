__author__ = 'Ralf Quast'

# import matplotlib

# matplotlib.use('PDF')

import matplotlib.pyplot as plt
import pylab
import numpy as np


class ReportPlotter:
    def __init__(self, sensor, report):
        """

        :type sensor: str
        :type report: dict
        """
        self.sensor = sensor
        self.report = report

    def get_sensor(self):
        """

        :rtype : str
        """
        return self.sensor

    def get_report(self):
        """

        :rtype : dict
        """
        return self.report

    def plot(self):
        product_checks = [
            'source_pathname_check',
            'source_filename_check',
            'product_can_be_opened_check',
            'corruptness_check',
        ]
        label_dictionary = {
            '01 File': 'source_pathname_check',
            '02 Filename': 'source_filename_check',
            '03 Can Open': 'product_can_be_opened_check',
            '04 SST Corrupt': 'corruptness_check',
            '05 SST Exists': 'sea_surface_temperature.existence_check',
        }

        total_counts = self.get_report()['summary_report.count']
        checks = []
        counts = []
        percentages = []
        labels = []

        for label, check in sorted(label_dictionary.iteritems(), reverse=True):
            if check in product_checks or (check.endswith('existence_check')):
                checks.append(check)
                check_result = self.get_report()[check]
                counts.append(check_result)
                percentages.append(check_result / (1.0 * total_counts))
                labels.append(label)

        figure, vertical_axis_left = plt.subplots(figsize=(9, 7))
        plt.subplots_adjust(left=0.15, right=0.85)
        pos = np.arange(len(checks)) + 0.5
        vertical_axis_left.barh(pos, percentages, align='center', height=0.5)

        pylab.yticks(pos, labels)

        vertical_axis_left.set_title(self.get_sensor())
        ax2 = vertical_axis_left.twinx()
        ax2.set_yticks(pos)
        ax2.set_yticklabels(counts)
        ax2.set_ylim(vertical_axis_left.get_ylim())
        ax2.set_ylabel('Failure Counts')
        vertical_axis_left.set_xlabel('Failure Rate (for ' + str(total_counts) + ' files in total)')

        #figure.savefig('test.pdf')
        plt.show()
