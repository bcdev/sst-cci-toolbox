__author__ = 'ralf'

import os
import re

from svr_product_verifier import ProductVerifier


class ReportAccumulator:
    def __init__(self, report_dir_pathname='.', report_filename_pattern='.*\\.json', summary_report_pathname=None):
        """

        :type report_dir_pathname: str
        :type report_filename_pattern: str
        :type summary_report_pathname: str
        """
        self.report_dir_pathname = report_dir_pathname
        self.report_filename_pattern = report_filename_pattern
        self.summary_report_pathname = summary_report_pathname
        self.summary_report = {'summary_report.count': 0}

    def accumulate_entry(self, k, v):
        """

        :type k: str
        """
        if isinstance(v, str):
            if k in self.summary_report:
                if isinstance(v, str):
                    previous = self.summary_report[k]
                    """ :type previous: str """
                    self.summary_report[k] = previous + ', ' + v
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
                        print 'Accumulating report', report_pathname
                        report = ProductVerifier.load_report(report_pathname)
                        for k, v in report.iteritems():
                            self.accumulate_entry(k, v)
                        self.accumulate_entry('summary_report.count', 1)
        finally:
            ProductVerifier.dump_report(self.summary_report, self.summary_report_pathname)


if __name__ == "__main__":
    # Call with up to three arguments:
    #
    # 1 = report dir pathname (optional)
    # 2 = report filename pattern (optional)
    # 3 = summary report pathname (optional)
    import sys

    argument_count = len(sys.argv)
    if argument_count == 1:
        accumulator = ReportAccumulator()
    elif argument_count == 2:
        accumulator = ReportAccumulator(sys.argv[1])
    elif argument_count == 3:
        accumulator = ReportAccumulator(sys.argv[1], sys.argv[2])
    elif argument_count == 4:
        accumulator = ReportAccumulator(sys.argv[1], sys.argv[2], sys.argv[3])
    else:
        print 'usage:', sys.argv[0], '<report dir pathname> <report filename pattern> <summary report pathname>'
        sys.exit(1)

    # noinspection PyBroadException
    try:
        accumulator.accumulate()
    except:
        sys.exit(1)

    sys.exit()
