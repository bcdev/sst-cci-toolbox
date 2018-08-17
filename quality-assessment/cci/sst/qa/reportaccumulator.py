__author__ = 'Ralf Quast'

import os
import re

from cci.sst.qa.productverifier import ProductVerifier
from cci.sst.qa.svrrunner import SvrRunner


class ReportAccumulator:
    def __init__(self, report_dir_pathname, summary_report_pathname=None,
                 report_filename_pattern=SvrRunner.get_report_filename_pattern()):
        """

        :type report_dir_pathname: str
        :type summary_report_pathname: str
        :type report_filename_pattern: str
        """
        self.report_dir_pathname = report_dir_pathname
        self.summary_report_pathname = summary_report_pathname
        self.report_filename_pattern = report_filename_pattern
        self.summary_report = {'summary_report.count': 0}

    def accumulate_entry(self, k, v):
        """

        :type k: str
        """
        if k in self.summary_report:
            if k.endswith('_failed_for'):
                previous = self.summary_report[k]
                """ :type previous: str """
                self.summary_report[k] = previous + ' ' + v
            else:
                previous = self.summary_report[k]
                self.summary_report[k] = previous + v
        else:
            self.summary_report[k] = v

    def accumulate(self):
        try:
            for root, dirnames, filenames in os.walk(self.report_dir_pathname):
                for filename in filenames:
                    if re.match(self.report_filename_pattern, filename):
                        report_pathname = os.path.join(root, filename)
                        print(ProductVerifier.get_current_time(), 'accumulating report', report_pathname)
                        report = ProductVerifier.load_report(report_pathname)
                        for k, v in iter(report.items()):
                            self.accumulate_entry(k, v)
                        self.accumulate_entry('summary_report.count', 1)
        finally:
            ProductVerifier.dump_report(self.summary_report, self.summary_report_pathname)


if __name__ == "__main__":
    # Call with up to two arguments:
    #
    # 1 = report dir pathname
    # 2 = summary report pathname (optional)
    import sys

    argument_count = len(sys.argv)
    if argument_count == 2:
        accumulator = ReportAccumulator(sys.argv[1])
    elif argument_count == 3:
        accumulator = ReportAccumulator(sys.argv[1], sys.argv[2])
    else:
        print('usage:', sys.argv[0], '<report dir pathname> <summary report pathname>')
        sys.exit(1)

    # noinspection PyBroadException
    try:
        accumulator.accumulate()
    except:
        sys.exit(1)

    sys.exit()
